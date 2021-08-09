package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.ui.dialog.AddStandaloneClustersDialog;

public class AddStandaloneClustersAction extends DumbAwareAction {
    private final Project project;

    public AddStandaloneClustersAction(Project project) {
        super("Connect to Cluster", "", AllIcons.General.Add);
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new AddStandaloneClustersDialog(project).showAndGet();
    }
}
