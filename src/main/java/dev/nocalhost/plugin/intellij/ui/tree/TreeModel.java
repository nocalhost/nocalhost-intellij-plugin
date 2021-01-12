package dev.nocalhost.plugin.intellij.ui.tree;

import com.intellij.util.ui.tree.AbstractTreeModel;

public class TreeModel extends AbstractTreeModel {

    @Override
    public Object getRoot() {
        return null;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        return false;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return 0;
    }
}
