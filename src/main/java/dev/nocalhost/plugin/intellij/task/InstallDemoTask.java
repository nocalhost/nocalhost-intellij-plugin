package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.Constants;
import lombok.SneakyThrows;

import static dev.nocalhost.plugin.intellij.utils.Constants.MANIFEST_TYPE_RAW_MANIFEST;

public class InstallDemoTask extends Task.Backgroundable {
    private static final String DEMO_GIT_URL = "https://github.com/nocalhost/bookinfo.git";

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    public InstallDemoTask(Project project, Path kubeConfigPath, String namespace) {
        super(project, "Install demo", false);
        this.project = project;
        this.kubeConfigPath = kubeConfigPath;
        this.namespace = namespace;
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void onSuccess() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

        NocalhostNotifier.getInstance(project).notifySuccess("Demo installed", "");

        ApplicationManager.getApplication().invokeLater(() -> {
            ProgressManager.getInstance().run(new BrowseDemoTask(project, kubeConfigPath, namespace));
        });
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        NocalhostNotifier.getInstance(project).notifyError("Demo install error",
                "Error occurred while installing demo", e.getMessage());

    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        NhctlInstallOptions nhctlInstallOptions = new NhctlInstallOptions(kubeConfigPath, namespace);
        nhctlInstallOptions.setGitUrl(DEMO_GIT_URL);
        nhctlInstallOptions.setType(MANIFEST_TYPE_RAW_MANIFEST);
        outputCapturedNhctlCommand.install(Constants.DEMO_NAME, nhctlInstallOptions);
    }
}
