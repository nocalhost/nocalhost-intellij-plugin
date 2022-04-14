package dev.nocalhost.plugin.intellij.task;

import com.google.common.collect.Lists;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlRawConfig;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.DevModeService;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeExpandNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import dev.nocalhost.plugin.intellij.utils.PathsUtil;
import dev.nocalhost.plugin.intellij.utils.TerminalUtil;
import lombok.SneakyThrows;

public class StartingDevModeTask extends BaseBackgroundTask {
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(
            NocalhostSettings.class);
    private final NocalhostApi nocalhostApi = ApplicationManager.getApplication().getService(NocalhostApi.class);

    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final Path kubeConfigPath;
    private final DevModeService devModeService;

    public StartingDevModeTask(Project project, DevModeService devModeService) {
        super(project, "Starting DevMode", true);
        this.project = project;
        this.devModeService = devModeService;
        this.kubeConfigPath = KubeConfigUtil.toPath(devModeService.getRawKubeConfig());
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();

        NocalhostContextManager.getInstance(project).refresh();
        project.getMessageBus().syncPublisher(
                NocalhostTreeExpandNotifier.NOCALHOST_TREE_EXPAND_NOTIFIER_TOPIC).action();

        TerminalUtil.openTerminal(
                project,
                String.format(
                        "%s/%s",
                        devModeService.getApplicationName(),
                        devModeService.getServiceName()
                ),
                new GeneralCommandLine(Lists.newArrayList(
                        PathsUtil.backslash(NhctlUtil.binaryPath()),
                        "dev",
                        "terminal", devModeService.getApplicationName(),
                        "--deployment", devModeService.getServiceName(),
                        "--kubeconfig", PathsUtil.backslash(kubeConfigPath.toString()),
                        "--namespace", devModeService.getNamespace(),
                        "--controller-type", devModeService.getServiceType(),
                        "--container", "nocalhost-dev"
                ))
        );

        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();
        NocalhostNotifier.getInstance(project).notifySuccess("DevMode started", "");

        if (StringUtils.isNotEmpty(devModeService.getAction())) {
            ProgressManager
                    .getInstance()
                    .run(new ExecutionTask(project, devModeService, devModeService.getAction()));
        }
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(this.getProject(), "Nocalhost starting dev mode error",
                "Error occurred while starting dev mode", e);
    }

    private boolean canStart(String action) throws InterruptedException, NocalhostExecuteCmdException, IOException {
        if (StringUtils.isEmpty(action)) {
            return true;
        }
        var path = KubeConfigUtil.toPath(devModeService.getRawKubeConfig());
        var opts = new NhctlConfigOptions(path, devModeService.getNamespace());
        opts.setDeployment(devModeService.getServiceName());
        opts.setControllerType(devModeService.getServiceType());
        var config = ApplicationManager
                .getApplication()
                .getService(NhctlCommand.class)
                .getConfig(devModeService.getApplicationName(), opts, NhctlRawConfig.class);
        var bucket = config.getContainers();
        var container = bucket.isEmpty() ? null : bucket.get(0);
        try {
            var lines = ExecutionTask.kDebug.equals(action)
                    ? container.getDev().getCommand().getDebug()
                    : container.getDev().getCommand().getRun();
            return lines.size() > 0;
        } catch (Exception ex) {
            return false;
        }
    }

    @SneakyThrows
    @Override
    public void runTask(@NotNull ProgressIndicator indicator) {
        if ( ! canStart(devModeService.getAction())) {
            NocalhostNotifier
                    .getInstance(project)
                    .notifyError(
                            "Nocalhost",
                            "Please configure the <a href=\"https://nocalhost.dev/docs/reference/nh-config\">run/debug</a> command first.",
                            (notification, event) -> BrowserUtil.browse("https://nocalhost.dev/docs/reference/nh-config/")
                    );
            indicator.cancel();
            return;
        }

        String storageClass = getStorageClass();

        NhctlDevStartOptions nhctlDevStartOptions = new NhctlDevStartOptions(kubeConfigPath,
                devModeService.getNamespace(), this);
        nhctlDevStartOptions.setDeployment(devModeService.getServiceName());
        nhctlDevStartOptions.setControllerType(devModeService.getServiceType());
        nhctlDevStartOptions.setLocalSync(Paths.get(project.getBasePath()).toString());
        nhctlDevStartOptions.setContainer(devModeService.getContainerName());
        nhctlDevStartOptions.setStorageClass(storageClass);
        nhctlDevStartOptions.setWithoutTerminal(true);
        nhctlDevStartOptions.setMode(devModeService.getMode());
        nhctlDevStartOptions.setImage(devModeService.getImage());
        nhctlDevStartOptions.setHeader(devModeService.getHeader());
        outputCapturedNhctlCommand.devStart(devModeService.getApplicationName(),
                nhctlDevStartOptions);
    }

    private String getStorageClass() throws IOException, NocalhostApiException {
        if (devModeService.getServer() != null) {
            Set<NocalhostAccount> nocalhostAccounts = nocalhostSettings.getNocalhostAccounts();
            Optional<NocalhostAccount> nocalhostAccountOptional = nocalhostAccounts.stream()
                    .filter(e -> StringUtils.equals(e.getServer(), devModeService.getServer())
                            && StringUtils.equals(e.getUsername(), devModeService.getUsername()))
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
                    .filter(e -> StringUtils.equals(e.getKubeConfig(), devModeService.getRawKubeConfig()))
                    .findFirst();
            if (serviceAccountOptional.isEmpty()) {
                return null;
            }
            return serviceAccountOptional.get().getStorageClass();
        }
        return null;
    }
}
