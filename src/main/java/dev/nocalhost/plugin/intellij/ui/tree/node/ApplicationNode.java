package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplication;
import lombok.Getter;

@Getter
public class ApplicationNode extends DefaultMutableTreeNode {
    private final NhctlListApplication.Application application;

    public ApplicationNode(NhctlListApplication.Application application) {
        this.application = application;
    }

    public String getName() {
        return application.getName();
    }

    public NamespaceNode getNamespaceNode() {
        return (NamespaceNode) this.getParent();
    }

    public ClusterNode getClusterNode() {
        return (ClusterNode) getNamespaceNode().getParent();
    }
}
