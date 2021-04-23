package dev.nocalhost.plugin.intellij.ui.tree;

import java.util.Comparator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

public class NocalhostTreeModelBase extends DefaultTreeModel {
    private final Comparator<TreeNode> comparator;

    public NocalhostTreeModelBase(Comparator<TreeNode> comparator) {
        super(new DefaultMutableTreeNode());
        if (comparator != null) {
            this.comparator = comparator;
        } else {
            this.comparator = (o1, o2) -> 0;
        }
    }

    @Override
    public void setRoot(TreeNode root) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNodeFromParent(MutableTreeNode node) {
        throw new UnsupportedOperationException();
    }

    public void insertNode(MutableTreeNode child, MutableTreeNode parent) {
        synchronized (this) {
            int count = getChildCount(parent);
            boolean inserted = false;
            for (int i = 0; i < count; i++) {
                int comp = comparator.compare(child, (TreeNode) getChild(parent, i));
                if (comp <= 0) {
                    if (comp < 0) {
                        super.insertNodeInto(child, parent, i);
                    }
                    inserted = true;
                    break;
                }
            }
            if (!inserted) {
                super.insertNodeInto(child, parent, count);
            }
            if (parent == getRoot() && getChildCount(parent) == 1) {
                reload();
            }
        }
    }

    public void removeNode(MutableTreeNode node) {
        synchronized (this) {
            super.removeNodeFromParent(node);
        }
    }
}
