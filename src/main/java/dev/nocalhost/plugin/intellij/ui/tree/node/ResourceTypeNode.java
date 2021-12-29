package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import lombok.Getter;

@Getter
public class ResourceTypeNode extends DefaultMutableTreeNode {
    private final String name;

    public ResourceTypeNode(String name) {
        this.name = name;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public ResourceGroupNode getResourceGroupNode() {
        return (ResourceGroupNode) this.getParent();
    }

    public ApplicationNode getApplicationNode() {
        return (ApplicationNode) getResourceGroupNode().getParent();
    }
}