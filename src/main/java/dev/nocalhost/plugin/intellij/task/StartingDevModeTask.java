package dev.nocalhost.plugin.intellij.task;

import com.google.common.collect.Lists;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
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
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostProjectSettings;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import dev.nocalhost.plugin.intellij.utils.TerminalUtil;
import lombok.SneakyThrows;

public class StartingDevModeTask extends Task.Backgroundable {
    private static final Logger LOG = Logger.getInstance(StartingDevModeTask.class);

    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(
            NocalhostSettings.class);
    private final NocalhostApi nocalhostApi = ApplicationManager.getApplication().getService(NocalhostApi.class);

    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final String command;
    private final Project project;
    private final Path kubeConfigPath;
    private final ServiceProjectPath serviceProjectPath;

    public StartingDevModeTask(Project project, ServiceProjectPath serviceProjectPath, String command) {
        super(project, "Starting DevMode", false);
        this.project = project;
        this.command = command;
        this.serviceProjectPath = serviceProjectPath;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(serviceProjectPath.getRawKubeConfig());
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();

        TerminalUtil.openTerminal(
                project,
                String.format(
                        "%s/%s",
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

        project.getService(NocalhostProjectSettings.class).setDevModeService(serviceProjectPath);

        if (StringUtils.isNotEmpty(command)) {
            ProgressManager
                    .getInstance()
                    .run(new ExecutionTask(project, serviceProjectPath, command));
        }
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        LOG.error("Error occurred while starting dev mode", e);
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
