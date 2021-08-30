package dev.nocalhost.plugin.intellij.task;

import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevEndOptions;
import dev.nocalhost.plugin.intellij.settings.data.DevModeService;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ExitDevModeTask extends Task.Backgroundable {
    private final DevModeService service;

    public ExitDevModeTask(Project project, DevModeService service) {
        super(project, "Ending DevMode", true);
        this.service = service;
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

        NocalhostNotifier.getInstance(getProject()).notifySuccess("DevMode ended", "");
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(getProject(), "Ending DevMode error",
                "Error occurred while ending DevMode", e);
    }

    @Override
    @SneakyThrows
    public void run(@NotNull ProgressIndicator indicator) {
        var path = KubeConfigUtil.kubeConfigPath(service.getRawKubeConfig());
        NhctlDevEndOptions opts = new NhctlDevEndOptions(path, service.getNamespace(), this);
        opts.setDeployment(service.getServiceName());
        opts.setControllerType(service.getServiceType());

        getProject()
                .getService(OutputCapturedNhctlCommand.class)
                .devEnd(service.getApplicationName(), opts);
    }
}
