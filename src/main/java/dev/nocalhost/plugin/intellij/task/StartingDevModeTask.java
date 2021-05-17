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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.helpers.KubectlHelper;
import dev.nocalhost.plugin.intellij.settings.NocalhostProjectSettings;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleTerminalNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class StartingDevModeTask extends Task.Backgroundable {
    private static final Logger LOG = Logger.getInstance(StartingDevModeTask.class);

    private static final String NOCALHOST_DEV_CONTAINER_NAME = "nocalhost-dev";

    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final NocalhostSettings nocalhostSettings = ServiceManager.getService(
            NocalhostSettings.class);
    private final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);

    private final Project project;
    private final ServiceProjectPath serviceProjectPath;
    private final Path kubeConfigPath;

    public StartingDevModeTask(Project project, ServiceProjectPath serviceProjectPath) {
        super(project, "Starting DevMode", false);
        this.project = project;
        this.serviceProjectPath = serviceProjectPath;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(serviceProjectPath.getRawKubeConfig());
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        // start dev space terminal
        ToolWindowManager.getInstance(project).getToolWindow("Nocalhost Console").activate(() -> {
            project.getMessageBus()
                    .syncPublisher(NocalhostConsoleTerminalNotifier.NOCALHOST_CONSOLE_TERMINAL_NOTIFIER_TOPIC)
                    .action(kubeConfigPath,
                            serviceProjectPath.getNamespace(),
                            serviceProjectPath.getApplicationName(),
                            serviceProjectPath.getServiceName());
        });

        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
        NocalhostNotifier.getInstance(project).notifySuccess("DevMode started", "");

        final NocalhostProjectSettings nocalhostProjectSettings = project.getService(
                NocalhostProjectSettings.class);
        nocalhostProjectSettings.setDevModeService(serviceProjectPath);
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        LOG.error("error occurred while starting dev mode", e);
        NocalhostNotifier.getInstance(project).notifyError(
                "Nocalhost starting dev mode error",
                "Error occurred while starting dev mode",
                e.getMessage());
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        final NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath,
                serviceProjectPath.getNamespace());
        nhctlDescribeOptions.setDeployment(serviceProjectPath.getServiceName());

        NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                serviceProjectPath.getApplicationName(),
                nhctlDescribeOptions,
                NhctlDescribeService.class);

        // check if devmode already started
        if (nhctlDescribeService.isDeveloping()) {
            return;
        }

        String storageClass = getStorageClass();

        // nhctl dev start ...
        indicator.setText("Starting DevMode: dev start");
        NhctlDevStartOptions nhctlDevStartOptions = new NhctlDevStartOptions(kubeConfigPath,
                serviceProjectPath.getNamespace());
        nhctlDevStartOptions.setDeployment(serviceProjectPath.getServiceName());
        nhctlDevStartOptions.setLocalSync(Lists.newArrayList(project.getBasePath()));
        nhctlDevStartOptions.setContainer(serviceProjectPath.getContainerName());
        nhctlDevStartOptions.setStorageClass(storageClass);
        final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project
                .getService(OutputCapturedNhctlCommand.class);
        outputCapturedNhctlCommand.devStart(serviceProjectPath.getApplicationName(),
                nhctlDevStartOptions);

        // wait for nocalhost-dev container started
        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
        KubeResource deployment;
        List<String> containerNames = Lists.newArrayList();
        do {
            Thread.sleep(1000);
            deployment = kubectlCommand.getResource("deployment", serviceProjectPath.getServiceName(), kubeConfigPath, serviceProjectPath.getNamespace());
            KubeResourceList pods = kubectlCommand.getResourceList("pods", deployment.getSpec().getSelector().getMatchLabels(), kubeConfigPath, serviceProjectPath.getNamespace());
            containerNames = pods.getItems().get(0).getSpec().getContainers().stream().map(KubeResource.Spec.Container::getName).collect(Collectors.toList());
        } while (!KubectlHelper.isKubeResourceAvailable(deployment) || !containerNames.contains(NOCALHOST_DEV_CONTAINER_NAME));

        // nhctl sync ...
        indicator.setText("Starting DevMode: sync file");
        NhctlSyncOptions nhctlSyncOptions = new NhctlSyncOptions(kubeConfigPath,
                serviceProjectPath.getNamespace());
        nhctlSyncOptions.setDeployment(serviceProjectPath.getServiceName());
        nhctlSyncOptions.setContainer(serviceProjectPath.getContainerName());
        outputCapturedNhctlCommand.sync(serviceProjectPath.getApplicationName(),
                nhctlSyncOptions);
    }

    private String getStorageClass() throws IOException, NocalhostApiException {
        if (serviceProjectPath.getServer() != null) {
            Set<NocalhostAccount> nocalhostAccounts = nocalhostSettings.getNocalhostAccounts();
            Optional<NocalhostAccount> nocalhostAccountOptional = nocalhostAccounts.stream()
                    .filter(e -> StringUtils.equals(e.getServer(), serviceProjectPath.getServer())
                            && StringUtils.equals(e.getUsername(), serviceProjectPath.getUsername()))
                    .findFirst();
            if (nocalhostAccountOptional.isEmpty()) {
                return null;
            }
            NocalhostAccount nocalhostAccount = nocalhostAccountOptional.get();

            List<ServiceAccount> serviceAccounts = nocalhostApi.listServiceAccount(
                    nocalhostAccount.getServer(), nocalhostAccount.getJwt());
            if (serviceAccounts == null) {
                return null;
            }
            Optional<ServiceAccount> serviceAccountOptional = serviceAccounts.stream()
                    .filter(e -> StringUtils.equals(e.getKubeConfig(), serviceProjectPath.getRawKubeConfig()))
                    .findFirst();
            if (serviceAccountOptional.isEmpty()) {
                return null;
            }
            return serviceAccountOptional.get().getStorageClass();
        }
        return null;
    }
}
