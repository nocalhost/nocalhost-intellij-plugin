package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.ui.PortForwardConfigurationDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class PortForwardAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(PortForwardAction.class);

    private final Project project;
    private final ResourceNode node;

    public PortForwardAction(Project project, ResourceNode node) {
        super("Port Forward");
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        new PortForwardConfigurationDialog(node, project).show();
    }
}
