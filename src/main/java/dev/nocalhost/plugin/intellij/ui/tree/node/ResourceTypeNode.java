package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResourceTypeNode extends DefaultMutableTreeNode {
    private String name;
    private boolean expanded;
    private boolean loaded;

    public ResourceTypeNode(String name) {
        this(name, false, false);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public ResourceTypeNode clone() {
        return new ResourceTypeNode(name, expanded, loaded);
    }
}