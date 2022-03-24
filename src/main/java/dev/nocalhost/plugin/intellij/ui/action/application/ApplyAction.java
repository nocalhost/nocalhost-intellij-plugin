package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlApplyOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.task.BaseBackgroundTask;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ApplyAction extends DumbAwareAction {
    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;
    private final String applicationName;

    public ApplyAction(Project project, ApplicationNode node) {
        super("Apply New Manifest");
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        this.applicationName = node.getName();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final Path chosenPath = FileChooseUtil.chooseSingleFileOrDirectory(project);
        if (chosenPath == null) {
            return;
        }

        ProgressManager.getInstance().run(new BaseBackgroundTask(project, "Applying Kubernetes Configuration") {
            private String result = "";

            @Override
            public void onThrowable(@NotNull Throwable e) {
                ErrorUtil.dealWith(project, "Nocalhost apply error",
                        "Error occurred while applying kubernetes file", e);
            }

            @Override
            public void onSuccess() {
                super.onSuccess();
                NocalhostNotifier.getInstance(project).notifySuccess("Kubernetes configuration applied", result);
            }

            @SneakyThrows
            @Override
            public void runTask(@NotNull ProgressIndicator progressIndicator) {
                final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
                NhctlApplyOptions nhctlApplyOptions = new NhctlApplyOptions(kubeConfigPath, namespace, this);
                nhctlApplyOptions.setFile(chosenPath.toString());
                result = nhctlCommand.apply(applicationName, nhctlApplyOptions);
            }
        });
    }
}
