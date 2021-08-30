package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import lombok.SneakyThrows;

public class InstallStandaloneApplicationTask extends BaseBackgroundTask {
    private final Project project;
    private final String applicationName;
    private final NhctlInstallOptions opts;

    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    public InstallStandaloneApplicationTask(Project project,
                                            String applicationName,
                                            NhctlInstallOptions opts) {
        super(project, "Deploy Application: " + applicationName);

        this.project = project;
        this.applicationName = applicationName;
        this.opts = opts;

        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @SneakyThrows
    @Override
    public void runTask(@NotNull ProgressIndicator indicator) {
        opts.setTask(this);
        outputCapturedNhctlCommand.install(applicationName, opts);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

        NocalhostNotifier.getInstance(project).notifySuccess(
                "Application " + applicationName + " deployed",
                "");
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(this.getProject(), "Application deployment error",
                "Error occurred while deploying application " + applicationName, e);

    }
}
