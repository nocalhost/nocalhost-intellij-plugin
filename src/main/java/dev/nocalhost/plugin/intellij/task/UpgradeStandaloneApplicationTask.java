package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUpgradeOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import lombok.SneakyThrows;

public class UpgradeStandaloneApplicationTask extends Task.Backgroundable {
    private final Project project;
    private final String applicationName;
    private final NhctlUpgradeOptions opts;

    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    public UpgradeStandaloneApplicationTask(Project project,
                                            String applicationName,
                                            NhctlUpgradeOptions opts) {
        super(project, "Upgrade Application: " + applicationName);

        this.project = project;
        this.applicationName = applicationName;
        this.opts = opts;

        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        outputCapturedNhctlCommand.upgrade(applicationName, opts);
    }


    @Override
    public void onSuccess() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

        NocalhostNotifier.getInstance(project).notifySuccess(
                "Application " + applicationName + " upgraded",
                "");
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(this.getProject(), "Application upgrade error",
                "Error occurred while upgrading application " + applicationName, e);
    }


}
