package dev.nocalhost.plugin.intellij.task;

import com.google.common.collect.Lists;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
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

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncOptions;
import dev.nocalhost.plugin.intellij.helpers.KubectlHelper;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleTerminalNotifier;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class StartingDevModeTask extends Task.Backgroundable {
    private static final Logger LOG = Logger.getInstance(StartingDevModeTask.class);

    private static final String NOCALHOST_DEV_CONTAINER_NAME = "nocalhost-dev";

    private final Project project;
    private final DevSpace devSpace;
    private final DevModeService devModeService;

    NhctlDescribeService nhctlDescribeService;
    NhctlCommand nhctlCommand;
    String kubeconfigPath;
    String appName;


    public StartingDevModeTask(Project project, DevSpace devSpace, DevModeService devModeService) {
        super(project, "Starting DevMode", false);
        this.project = project;
        this.devSpace = devSpace;
        this.devModeService = devModeService;

        nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace).toString();
        appName = devSpace.getContext().getApplicationName();

        final NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions();
        nhctlDescribeOptions.setDeployment(devModeService.getName());
        nhctlDescribeOptions.setKubeconfig(kubeconfigPath);
        try {
            nhctlDescribeService = nhctlCommand.describe(
                    devSpace.getContext().getApplicationName(),
                    nhctlDescribeOptions,
                    NhctlDescribeService.class);
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while describing application", e);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        // start dev space terminal
        ToolWindowManager.getInstance(project).getToolWindow("Nocalhost Console").activate(() -> {
            project.getMessageBus()
                    .syncPublisher(NocalhostConsoleTerminalNotifier.NOCALHOST_CONSOLE_TERMINAL_NOTIFIER_TOPIC)
                    .action(devSpace, devModeService.getName());
        });

        ApplicationManager.getApplication().getMessageBus()
                .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC)
                .action();

        Notifications.Bus.notify(new Notification("Nocalhost.Notification", "DevMode started", "", NotificationType.INFORMATION), project);
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
            nhctlDevStartOptions.setDeployment(devModeService.getName());
            nhctlDevStartOptions.setLocalSync(Lists.newArrayList(project.getBasePath()));
            nhctlDevStartOptions.setKubeconfig(kubeconfigPath);
            final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
            outputCapturedNhctlCommand.devStart(appName, nhctlDevStartOptions);

            // wait for nocalhost-dev container started
            final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
            KubeResource deployment;
            String containerName;
            do {
                Thread.sleep(1000);
                deployment = kubectlCommand.getResource("deployment", devModeService.getName(), devSpace);
                KubeResourceList pods = kubectlCommand.getResourceList("pods", deployment.getSpec().getSelector().getMatchLabels(), devSpace);
                containerName = pods.getItems().get(0).getSpec().getContainers().get(0).getName();
            } while (!KubectlHelper.isKubeResourceAvailable(deployment) || !StringUtils.equals(containerName, NOCALHOST_DEV_CONTAINER_NAME));

            // nhctl sync ...
            indicator.setText("Starting DevMode: sync file");
            NhctlSyncOptions nhctlSyncOptions = new NhctlSyncOptions();
            nhctlSyncOptions.setDeployment(devModeService.getName());
            nhctlSyncOptions.setKubeconfig(kubeconfigPath);
            outputCapturedNhctlCommand.sync(appName, nhctlSyncOptions);

            // nhctl port-forward ...
            indicator.setText("Starting DevMode: port forward");
            NhctlPortForwardStartOptions nhctlPortForwardOptions = new NhctlPortForwardStartOptions();
            nhctlPortForwardOptions.setDeployment(devModeService.getName());
            nhctlPortForwardOptions.setWay(NhctlPortForwardStartOptions.Way.DEV_PORTS);
            nhctlPortForwardOptions.setDevPorts(nhctlDescribeService.getRawConfig().getDevPorts());
            nhctlPortForwardOptions.setKubeconfig(kubeconfigPath);
            outputCapturedNhctlCommand.startPortForward(appName, nhctlPortForwardOptions);
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while starting dev mode", e);
        }
    }
}
