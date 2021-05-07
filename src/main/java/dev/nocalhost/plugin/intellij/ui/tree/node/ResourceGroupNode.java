package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import lombok.Getter;

@Getter
public class ResourceGroupNode extends DefaultMutableTreeNode {
    private String name;

    public ResourceGroupNode(String name) {
        this.name = name;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}