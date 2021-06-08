package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.icons.AllIcons;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.PathsUtil;

public class OpenProjectAction extends DumbAwareAction {
    private final Project project;
    private final ResourceNode node;

    public OpenProjectAction(Project project, ResourceNode node) {
        super("Open Project", "", AllIcons.Actions.MenuOpen);
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        String projectPath = node.getNhctlDescribeService().getAssociate();
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
        ProjectManagerEx.getInstanceEx().openProject(Paths.get(projectPath), new OpenProjectTask());
    }
}
