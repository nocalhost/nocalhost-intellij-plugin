package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlApplyOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class ApplyAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(ApplyAction.class);

    private final Project project;
    private final Path kubeConfigPath;
    private final String namespace;
    private final String applicationName;

    public ApplyAction(Project project, ApplicationNode node) {
        super("Apply");
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        this.applicationName = node.getName();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final Path chosenPath = FileChooseUtil.chooseSingleFileOrDirectory(project);
        if (chosenPath == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Applying Kubernetes Configuration", false) {
            private String result = "";

            @Override
            public void onThrowable(@NotNull Throwable e) {
                if (e instanceof NocalhostExecuteCmdException) {
                    return;
                }
                LOG.error("error occurred while apply kubernetes config file", e);
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost apply error", "Error occurred while applying kubernetes file", e.getMessage());
            }

            @Override
            public void onSuccess() {
                NocalhostNotifier.getInstance(project).notifySuccess("Kubernetes configuration applied", result);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
                NhctlApplyOptions nhctlApplyOptions = new NhctlApplyOptions(kubeConfigPath, namespace);
                nhctlApplyOptions.setFile(chosenPath.toString());
                result = nhctlCommand.apply(applicationName, nhctlApplyOptions);
            }
        });
    }
}
