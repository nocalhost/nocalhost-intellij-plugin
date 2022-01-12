package dev.nocalhost.plugin.intellij.task;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.List;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.Constants;
import lombok.SneakyThrows;

public class InstallApplicationTask extends BaseBackgroundTask {
    private static final List<String> BOOKINFO_GITS = Lists.newArrayList(
            "https://github.com/nocalhost/bookinfo.git",
            "git@github.com:nocalhost/bookinfo.git",
            "https://e.coding.net/nocalhost/nocalhost/bookinfo.git",
            "git@e.coding.net:nocalhost/nocalhost/bookinfo.git"
    );

    private final Project project;

    private final Application application;
    private final NhctlInstallOptions opts;

    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    public InstallApplicationTask(@Nullable Project project, Application application, NhctlInstallOptions opts) {
        super(project, "Deploying application: " + application.getContext().getApplicationName(), true);
        this.project = project;
        this.application = application;
        this.opts = opts;

        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

        NocalhostNotifier.getInstance(project).notifySuccess(
                "Application " + application.getContext().getApplicationName() + " Deployed",
                "");

        if (StringUtils.equals(Constants.DEMO_NAME, application.getContext().getApplicationName()) && BOOKINFO_GITS.contains(application.getContext().getApplicationUrl())) {
            ProgressManager.getInstance().run(new BrowseDemoTask(project, Paths.get(opts.getKubeconfig()), opts.getNamespace()));
        }
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(this.getProject(), "Application deployment error",
                "Error occurred while deploying application", e);
    }

    @SneakyThrows
    @Override
    public void runTask(@NotNull ProgressIndicator indicator) {
        opts.setTask(this);
        outputCapturedNhctlCommand.install(application.getContext().getApplicationName(), opts);
    }
}
