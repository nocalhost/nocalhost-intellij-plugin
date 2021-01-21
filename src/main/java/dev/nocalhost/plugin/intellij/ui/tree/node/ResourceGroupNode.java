package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResourceGroupNode extends DefaultMutableTreeNode {
    private String name;
    private boolean expanded;

    public ResourceGroupNode(String name) {
        this(name, false);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public ResourceGroupNode clone() {
        return new ResourceGroupNode(name, expanded);
    }
}