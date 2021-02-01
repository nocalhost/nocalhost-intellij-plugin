package dev.nocalhost.plugin.intellij.ui.tree.listerner.workload;

import com.intellij.openapi.project.Project;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import dev.nocalhost.plugin.intellij.ui.PortForwardConfigurationDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class PortForward implements ActionListener {
    private final ResourceNode node;
    private final Project project;

    public PortForward(ResourceNode node, Project project) {
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        new PortForwardConfigurationDialog(node, project).show();
    }
}
