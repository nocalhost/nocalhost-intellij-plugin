package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResourceTypeNode extends DefaultMutableTreeNode {
    private String name;
    private boolean loaded;

    public ResourceTypeNode(String name) {
        this(name, false);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public DevSpace getDevSpace() {
        TreeNode node = this;
        for (int i = 0; i < 3; i++) {
            if (node != null) {
                node = node.getParent();
            } else {
                return null;
            }
        }
        return ((DevSpaceNode) node).getDevSpace();
    }
}