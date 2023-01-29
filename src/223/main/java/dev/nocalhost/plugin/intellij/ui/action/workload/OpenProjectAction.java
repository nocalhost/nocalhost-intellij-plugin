package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.icons.AllIcons;
import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateOptions;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.PathsUtil;

public class OpenProjectAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public OpenProjectAction(Project project, ResourceNode node) {
        super("Open Project", "", AllIcons.Actions.MenuOpen);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        String projectPath = node.getNhctlDescribeService().getAssociate();
        if (PathsUtil.isExists(projectPath)) {
            setAssociate(projectPath);
        } else {
            Path codeSource = FileChooseUtil.chooseSingleDirectory(project, "",
                    "Select source code directory.");
            if (codeSource != null) {
                setAssociate(codeSource.toString());
            }
        }
    }

    private void setAssociate(String projectPath) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(kubeConfigPath, namespace);
                opts.setLocalSync(projectPath);
                opts.setDeployment(node.resourceName());
                opts.setControllerType(node.controllerType());
                nhctlCommand.devAssociate(node.applicationName(), opts);

                openProject(projectPath);
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Associate local directory error",
                        "Error while associating local directory", e);
            }
        });
    }

    private void openProject(String projectPath) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Project[] openProjects = ProjectManagerEx.getInstanceEx().getOpenProjects();
            for (Project openProject : openProjects) {
                if (PathsUtil.isSame(projectPath, openProject.getBasePath())) {
                    ToolWindow toolWindow = ToolWindowManager.getInstance(openProject)
                            .getToolWindow(ToolWindowId.PROJECT_VIEW);
                    if (toolWindow != null) {
                        toolWindow.activate(null);
                        return;
                    }
                }
            }

            var task = OpenProjectTask.build();
            RecentProjectsManagerBase.getInstanceEx().openProjectSync(Paths.get(projectPath), task.withRunConfigurators());
        });
    }
}
