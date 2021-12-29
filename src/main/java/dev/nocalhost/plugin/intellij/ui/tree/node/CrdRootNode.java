package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;
import org.jetbrains.annotations.NotNull;
import lombok.Getter;

@Getter
public class CrdRootNode extends DefaultMutableTreeNode {
    private final String name;

    public CrdRootNode(@NotNull String name) {
        this.name = name;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public NamespaceNode getNamespaceNode() {
        var owner = getParent();
        while (owner != null) {
            if (owner instanceof NamespaceNode) {
                return (NamespaceNode) owner;
            }
            owner = owner.getParent();
        }
        return null;
    }
}
