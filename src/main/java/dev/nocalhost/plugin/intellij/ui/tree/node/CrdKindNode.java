package dev.nocalhost.plugin.intellij.ui.tree.node;

import dev.nocalhost.plugin.intellij.commands.data.NhctlCrdKind;
import javax.swing.tree.DefaultMutableTreeNode;
import lombok.Getter;

@Getter
public class CrdKindNode extends DefaultMutableTreeNode {
    private final String name;
    private final NhctlCrdKind.Spec spec;

    public CrdKindNode(NhctlCrdKind.Spec spec) {
        this.spec = spec;
        this.name = spec.getKind() + "(" + spec.getVersion() + ")";
    }

    public String getResourceType() {
        return String.join(".", spec.getResource(), spec.getVersion(), spec.getGroup());
    }

    public ApplicationNode getApplicationNode() {
        return (ApplicationNode) getParent().getParent().getParent();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }
}
