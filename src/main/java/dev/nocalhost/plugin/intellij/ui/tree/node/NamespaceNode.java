package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import lombok.Getter;

@Getter
public class NamespaceNode extends DefaultMutableTreeNode {
    private String name;
    private String spaceName;

    public NamespaceNode(String name) {
        this.name = name;
    }

    public NamespaceNode(String name, String spaceName) {
        this.name = name;
        this.spaceName = spaceName;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public ClusterNode getClusterNode() {
        return (ClusterNode) this.getParent();
    }
}
