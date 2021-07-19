package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.task.LoadKubernetesResourceTask;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class EditManifestAction extends DumbAwareAction {
    private final Project project;
    private final ResourceNode node;

    public EditManifestAction(Project project, ResourceNode node) {
        super("Edit Manifest", "", AllIcons.Actions.Edit);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ProgressManager.getInstance().run(new LoadKubernetesResourceTask(project, node));
    }
}
