package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.Constants;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import lombok.SneakyThrows;

import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_RAW_MANIFEST;

public class InstallDemoTask extends BaseBackgroundTask {
    private static final String DEMO_GIT_URL = "https://github.com/nocalhost/bookinfo.git";

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    public InstallDemoTask(Project project, Path kubeConfigPath, String namespace) {
        super(project, "Deploy demo", true);
        this.project = project;
        this.kubeConfigPath = kubeConfigPath;
        this.namespace = namespace;
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

        NocalhostNotifier.getInstance(project).notifySuccess("Demo deployed", "");

        ApplicationManager.getApplication().invokeLater(() -> {
            ProgressManager.getInstance().run(new BrowseDemoTask(project, kubeConfigPath, namespace));
        });
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        ErrorUtil.dealWith(this.getProject(), "Demo deployment error",
                "Error occurred while deploying demo", e);

    }

    @SneakyThrows
    @Override
    public void runTask(@NotNull ProgressIndicator indicator) {
        NhctlInstallOptions nhctlInstallOptions = new NhctlInstallOptions(kubeConfigPath, namespace, this);
        nhctlInstallOptions.setGitUrl(DEMO_GIT_URL);
        nhctlInstallOptions.setType(MANIFEST_TYPE_RAW_MANIFEST);
        outputCapturedNhctlCommand.install(Constants.DEMO_NAME, nhctlInstallOptions);
    }
}
