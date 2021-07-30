package dev.nocalhost.plugin.intellij.task;

import com.google.common.collect.Lists;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevStartOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostProjectSettings;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostConsoleManager;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import lombok.SneakyThrows;

public class StartingDevModeTask extends Task.Backgroundable {
    private static final Logger LOG = Logger.getInstance(StartingDevModeTask.class);

    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final NocalhostSettings nocalhostSettings = ServiceManager.getService(
            NocalhostSettings.class);
    private final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);

    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final ServiceProjectPath serviceProjectPath;
    private final Path kubeConfigPath;

    public StartingDevModeTask(Project project, ServiceProjectPath serviceProjectPath) {
        super(project, "Starting DevMode", false);
        this.project = project;
        this.serviceProjectPath = serviceProjectPath;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(serviceProjectPath.getRawKubeConfig());
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        NocalhostConsoleManager.openTerminalWindow(
                project,
                String.format(
                        "%s/%s:terminal",
                        serviceProjectPath.getApplicationName(),
                        serviceProjectPath.getServiceName()
                ),
                new GeneralCommandLine(Lists.newArrayList(
                        NhctlUtil.binaryPath(),
                        "dev",
                        "terminal", serviceProjectPath.getApplicationName(),
                        "--deployment", serviceProjectPath.getServiceName(),
                        "--kubeconfig", kubeConfigPath.toString(),
                        "--namespace", serviceProjectPath.getNamespace(),
                        "--controller-type", serviceProjectPath.getServiceType(),
                        "--container", "nocalhost-dev"
                ))
        );

        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
        NocalhostNotifier.getInstance(project).notifySuccess("DevMode started", "");

        final NocalhostProjectSettings nocalhostProjectSettings = project.getService(
                NocalhostProjectSettings.class);
        nocalhostProjectSettings.setDevModeService(serviceProjectPath);
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        if (e instanceof NocalhostExecuteCmdException) {
            return;
        }
        LOG.error("error occurred while starting dev mode", e);
        NocalhostNotifier.getInstance(project).notifyError(
                "Nocalhost starting dev mode error",
                "Error occurred while starting dev mode",
                e.getMessage());
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath,
                serviceProjectPath.getNamespace());
        nhctlDescribeOptions.setDeployment(serviceProjectPath.getServiceName());
        nhctlDescribeOptions.setType(serviceProjectPath.getServiceType());

        NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                serviceProjectPath.getApplicationName(),
                nhctlDescribeOptions,
                NhctlDescribeService.class);

        if (nhctlDescribeService.isDeveloping()) {
            return;
        }

        String storageClass = getStorageClass();

        NhctlDevStartOptions nhctlDevStartOptions = new NhctlDevStartOptions(kubeConfigPath,
                serviceProjectPath.getNamespace());
        nhctlDevStartOptions.setDeployment(serviceProjectPath.getServiceName());
        nhctlDevStartOptions.setControllerType(serviceProjectPath.getServiceType());
        nhctlDevStartOptions.setLocalSync(Lists.newArrayList(project.getBasePath()));
        nhctlDevStartOptions.setContainer(serviceProjectPath.getContainerName());
        nhctlDevStartOptions.setStorageClass(storageClass);
        nhctlDevStartOptions.setWithoutTerminal(true);
        outputCapturedNhctlCommand.devStart(serviceProjectPath.getApplicationName(),
                nhctlDevStartOptions);
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
