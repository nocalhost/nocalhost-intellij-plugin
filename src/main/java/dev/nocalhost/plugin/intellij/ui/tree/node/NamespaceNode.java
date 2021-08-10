package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import lombok.Getter;

@Getter
public class NamespaceNode extends DefaultMutableTreeNode {
    private String namespace;
    private ServiceAccount.NamespacePack namespacePack;

    public NamespaceNode(String namespace) {
        this.namespace = namespace;
    }

    public NamespaceNode(ServiceAccount.NamespacePack namespacePack) {
        this.namespace = namespacePack.getNamespace();
        this.namespacePack = namespacePack;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public ClusterNode getClusterNode() {
        return (ClusterNode) this.getParent();
    }

    public String getName() {
        return namespacePack != null
                ? String.format("%s (%s)", namespacePack.getSpaceName(), namespacePack.getNamespace())
                : namespace;
    }
}
