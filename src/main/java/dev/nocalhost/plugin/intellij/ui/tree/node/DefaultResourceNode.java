package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DefaultResourceNode extends DefaultMutableTreeNode implements ResourceKeeperNode {
    public DevSpace getDevSpace() {
        return ((DevSpaceNode) this.getParent()).getDevSpace();
    }
}
