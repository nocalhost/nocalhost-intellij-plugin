package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class DevSpaceNode extends DefaultMutableTreeNode {
    private DevSpace devSpace;
    private boolean expanded;
    private boolean installed;

    public DevSpaceNode(DevSpace devSpace) {
        this(devSpace, false, false);
    }

    @Override
    public boolean isLeaf() {
        return !installed;
    }

    public DevSpaceNode clone() {
        return new DevSpaceNode(devSpace, expanded, installed);
    }
}