package dev.nocalhost.plugin.intellij.task;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.AliveDeployment;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.helpers.KubectlHelper;
import dev.nocalhost.plugin.intellij.helpers.UserDataKeyHelper;
import dev.nocalhost.plugin.intellij.settings.NocalhostRepo;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleTerminalNotifier;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class StartingDevModeTask extends Task.Backgroundable {
    private static final Logger LOG = Logger.getInstance(StartingDevModeTask.class);

    private static final String NOCALHOST_DEV_CONTAINER_NAME = "nocalhost-dev";

    private final Project project;
    private final DevSpace devSpace;
    private final DevModeService devModeService;

    private NhctlDescribeService nhctlDescribeService;
    private NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final String kubeconfigPath;
    private final String appName;
    private List<String> portForward = Lists.newArrayList();

    public StartingDevModeTask(Project project, DevSpace devSpace, DevModeService devModeService) {
        super(project, "Starting DevMode", false);
        this.project = project;
        this.devSpace = devSpace;
        this.devModeService = devModeService;

        kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace).toString();
        appName = devSpace.getContext().getApplicationName();

        final NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions();
        nhctlDescribeOptions.setDeployment(devModeService.getServiceName());
        nhctlDescribeOptions.setKubeconfig(kubeconfigPath);
        try {
            nhctlDescribeService = nhctlCommand.describe(
                    devSpace.getContext().getApplicationName(),
                    nhctlDescribeOptions,
                    NhctlDescribeService.class);
            for (ServiceContainer container : nhctlDescribeService.getRawConfig().getContainers()) {
                if (StringUtils.equals(devModeService.getContainerName(), container.getName())) {
                    portForward = container.getDev().getPortForward();
                    break;
                }
            }
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            LOG.error("error occurred while describing application", e);
            NocalhostNotifier.getInstance(project).notifyError("Nocalhost starting dev mode error", "Error occurred while starting dev mode", e.getMessage());
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        // start dev space terminal
        ToolWindowManager.getInstance(project).getToolWindow("Nocalhost Console").activate(() -> {
            project.getMessageBus()
                   .syncPublisher(NocalhostConsoleTerminalNotifier.NOCALHOST_CONSOLE_TERMINAL_NOTIFIER_TOPIC)
                   .action(devSpace, devModeService.getServiceName());
        });

        ApplicationManager.getApplication().getMessageBus()
                          .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC)
                          .action();
        NocalhostNotifier.getInstance(project).notifySuccess("DevMode started", "");

        NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        NocalhostRepo nocalhostRepo = new NocalhostRepo(
                nocalhostSettings.getBaseUrl(),
                nocalhostSettings.getUserInfo().getEmail(),
                appName,
                devModeService.getServiceName(),
                project.getBasePath()
        );
        nocalhostSettings.addRepos(nocalhostRepo);
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {

        try {
            // check if devmode already started
            if (nhctlDescribeService.isDeveloping()) {
                return;
            }

            // nhctl dev start ...
            indicator.setText("Starting DevMode: dev start");
            NhctlDevStartOptions nhctlDevStartOptions = new NhctlDevStartOptions();
            nhctlDevStartOptions.setDeployment(devModeService.getServiceName());
            nhctlDevStartOptions.setLocalSync(Lists.newArrayList(project.getBasePath()));
            nhctlDevStartOptions.setKubeconfig(kubeconfigPath);
            nhctlDevStartOptions.setContainer(devModeService.getContainerName());
            final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
            outputCapturedNhctlCommand.devStart(appName, nhctlDevStartOptions);

            // wait for nocalhost-dev container started
            final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
            KubeResource deployment;
            List<String> containerNames = Lists.newArrayList();
            do {
                Thread.sleep(1000);
                deployment = kubectlCommand.getResource("deployment", devModeService.getServiceName(), devSpace);
                KubeResourceList pods = kubectlCommand.getResourceList("pods", deployment.getSpec().getSelector().getMatchLabels(), devSpace);
                containerNames = pods.getItems().get(0).getSpec().getContainers().stream().map(e -> e.getName()).collect(Collectors.toList());
            } while (!KubectlHelper.isKubeResourceAvailable(deployment) || !containerNames.contains(NOCALHOST_DEV_CONTAINER_NAME));

            // nhctl sync ...
            indicator.setText("Starting DevMode: sync file");
            NhctlSyncOptions nhctlSyncOptions = new NhctlSyncOptions();
            nhctlSyncOptions.setDeployment(devModeService.getServiceName());
            nhctlSyncOptions.setContainer(devModeService.getContainerName());
            nhctlSyncOptions.setKubeconfig(kubeconfigPath);
            outputCapturedNhctlCommand.sync(appName, nhctlSyncOptions);

            // nhctl port-forward ...
            if (portForward.size() > 0) {
                indicator.setText("Starting DevMode: port forward");
                NhctlPortForwardStartOptions nhctlPortForwardOptions = new NhctlPortForwardStartOptions();
                nhctlPortForwardOptions.setDeployment(devModeService.getServiceName());
                nhctlPortForwardOptions.setWay(NhctlPortForwardStartOptions.Way.DEV_PORTS);
                nhctlPortForwardOptions.setDevPorts(portForward);
                nhctlPortForwardOptions.setKubeconfig(kubeconfigPath);
                outputCapturedNhctlCommand.startPortForward(appName, nhctlPortForwardOptions);
            }
            UserDataKeyHelper.removeAliveDeployments(project, new AliveDeployment(devSpace, devModeService.getServiceName(), project.getProjectFilePath()));
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            LOG.error("error occurred while starting dev mode", e);
            NocalhostNotifier.getInstance(project).notifyError("Nocalhost starting dev mode error", "Error occurred while starting dev mode", e.getMessage());
        }
    }
}
