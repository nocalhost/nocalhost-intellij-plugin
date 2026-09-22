package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUpgradeOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import lombok.SneakyThrows;

public class UpgradeStandaloneApplicationTask extends BaseBackgroundTask {
    private final Project project;
    private final String applicationName;
    private final NhctlUpgradeOptions opts;

    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    public UpgradeStandaloneApplicationTask(Project project,
                                            String applicationName,
                                            NhctlUpgradeOptions opts) {
        super(project, NocalhostI18n.format("progress.upgradeApplication", applicationName));

        this.project = project;
        this.applicationName = applicationName;
        this.opts = opts;

        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @SneakyThrows
    @Override
    public void runTask(@NotNull ProgressIndicator indicator) {
        opts.setTask(this);
        outputCapturedNhctlCommand.upgrade(applicationName, opts);
    }


    @Override
    public void onSuccess() {
        super.onSuccess();
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

        NocalhostNotifier.getInstance(project).notifySuccess(
                NocalhostI18n.format("success.applicationUpgraded", applicationName),
                "");
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(this.getProject(), NocalhostI18n.get("error.upgradeApplicationStandalone"),
                NocalhostI18n.format("error.upgradeApplicationStandalone.content", applicationName), e);
    }


}
