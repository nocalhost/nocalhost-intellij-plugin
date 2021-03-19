package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DefaultResourceNode extends DefaultMutableTreeNode {
    private boolean expanded;

    public DefaultResourceNode() {
        this.expanded = false;
    }
}
