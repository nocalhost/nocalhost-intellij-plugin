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

    @Override
    public boolean isLeaf() {
        return false;
    }
}