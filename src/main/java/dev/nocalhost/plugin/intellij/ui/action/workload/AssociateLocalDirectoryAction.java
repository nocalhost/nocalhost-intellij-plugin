package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateOptions;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class AssociateLocalDirectoryAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public AssociateLocalDirectoryAction(Project project, ResourceNode resourceNode) {
        super("Associate Local Directory");

        this.project = project;
        this.node = resourceNode;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getName();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Path dir = FileChooseUtil.chooseSingleDirectory(project);

        if (dir == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Associating project path") {

            @Override
            public void onThrowable(@NotNull Throwable e) {
                ErrorUtil.dealWith(project, "Associating project path error",
                        "Error occurs while associating project path", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(kubeConfigPath, namespace);
                opts.setAssociate(dir.toString());
                opts.setDeployment(node.resourceName());
                opts.setControllerType(node.getKubeResource().getKind());
                nhctlCommand.devAssociate(node.applicationName(), opts);
            }
        });
    }
}
