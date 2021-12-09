package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.google.gson.reflect.TypeToken;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateOptions;
import dev.nocalhost.plugin.intellij.nhctl.NhctlDevContainerListCommand;
import dev.nocalhost.plugin.intellij.task.BaseBackgroundTask;
import dev.nocalhost.plugin.intellij.ui.dialog.ListChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class AssociateLocalDirectoryAction extends DumbAwareAction {
    private final AtomicReference<String> container = new AtomicReference<>("");

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public AssociateLocalDirectoryAction(Project project, ResourceNode resourceNode) {
        super("Associate Local DIR");

        this.project = project;
        this.node = resourceNode;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ProgressManager.getInstance().run(new BaseBackgroundTask(project, "Associating project path") {

            @Override
            public void onThrowable(@NotNull Throwable ex) {
                ErrorUtil.dealWith(project, "Failed to associate project path",
                        "Error occurred while associating project path", ex);
            }

            @Override
            @SneakyThrows
            public void runTask(@NotNull ProgressIndicator indicator) {
                List<String> containers;

                var cmd = new NhctlDevContainerListCommand(project);
                cmd.setNamespace(namespace);
                cmd.setKubeConfig(kubeConfigPath);
                cmd.setDeployment(node.resourceName());
                cmd.setApplication(node.applicationName());
                cmd.setControllerType(node.getKubeResource().getKind());
                containers = DataUtils.GSON.fromJson(cmd.execute(), TypeToken.getParameterized(List.class, String.class).getType());

                ApplicationManager.getApplication().invokeLater(() -> {
                    if (containers.size() == 1) {
                        container.set(containers.get(0));
                    } else {
                        var dialog = new ListChooseDialog(project, "Select Container", containers);
                        if (dialog.showAndGet()) {
                            container.set(dialog.getSelectedValue());
                        } else {
                            return;
                        }
                    }

                    final Path dir = FileChooseUtil.chooseSingleDirectory(project);
                    if (dir != null) {
                        associate(dir);
                    }
                });
            }
        });
    }

    private void associate(@NotNull Path dir) {
        ProgressManager.getInstance().run(new BaseBackgroundTask(project, "Associating project path") {
            @Override
            public void onThrowable(@NotNull Throwable ex) {
                ErrorUtil.dealWith(project, "Failed to associate project path",
                        "Error occurred while associating project path", ex);
            }

            @Override
            @SneakyThrows
            public void runTask(@NotNull ProgressIndicator indicator) {
                var opts = new NhctlDevAssociateOptions(kubeConfigPath, namespace, this);
                opts.setLocalSync(dir.toString());
                opts.setContainer(container.get());
                opts.setDeployment(node.resourceName());
                opts.setControllerType(node.getKubeResource().getKind());
                project.getService(OutputCapturedNhctlCommand.class).devAssociate(node.applicationName(), opts);
            }
        });
    }
}
