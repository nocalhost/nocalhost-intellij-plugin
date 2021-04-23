package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApplicationNode extends DefaultMutableTreeNode implements ResourceKeeperNode {
    private Application application;

    public DevSpace getDevSpace() {
        return ((DevSpaceNode) this.getParent()).getDevSpace();
    }
}
