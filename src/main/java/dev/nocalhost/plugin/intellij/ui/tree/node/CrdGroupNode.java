package dev.nocalhost.plugin.intellij.ui.tree.node;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import lombok.Getter;

@Getter
public class CrdGroupNode extends DefaultMutableTreeNode {
    private final String name;

    public CrdGroupNode(String name) {
        this.name = name;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
