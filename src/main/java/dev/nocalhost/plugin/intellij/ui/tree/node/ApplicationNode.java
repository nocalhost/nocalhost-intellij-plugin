package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import lombok.Getter;

@Getter
public class ApplicationNode extends DefaultMutableTreeNode {
    private String name;

    public ApplicationNode(String name) {
        this.name = name;
    }

    public NamespaceNode getNamespaceNode() {
        return (NamespaceNode) this.getParent();
    }

    public ClusterNode getClusterNode() {
        return (ClusterNode) getNamespaceNode().getParent();
    }
}
