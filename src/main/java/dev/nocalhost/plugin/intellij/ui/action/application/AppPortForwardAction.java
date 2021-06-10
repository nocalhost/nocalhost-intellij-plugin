package dev.nocalhost.plugin.intellij.ui.action.application;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.ui.dialog.AppPortForwardConfigurationDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;

public class AppPortForwardAction extends DumbAwareAction {
    private final Project project;
    private final ApplicationNode node;

    public AppPortForwardAction(Project project, ApplicationNode node) {
        super("Port Forward");
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        new AppPortForwardConfigurationDialog(project, node).showAndGet();
    }
}
