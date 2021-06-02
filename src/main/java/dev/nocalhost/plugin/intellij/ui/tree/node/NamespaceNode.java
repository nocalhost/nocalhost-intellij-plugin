package dev.nocalhost.plugin.intellij.ui.tree.node;

import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;

import lombok.Getter;

@Getter
public class NamespaceNode extends DefaultMutableTreeNode {
    private String namespace;
    private String spaceName;
    private long spaceId;

    public NamespaceNode(String namespace) {
        this.namespace = namespace;
    }

    public NamespaceNode(String namespace, String spaceName, long spaceId) {
        this.namespace = namespace;
        this.spaceName = spaceName;
        this.spaceId = spaceId;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public ClusterNode getClusterNode() {
        return (ClusterNode) this.getParent();
    }

    public String getName() {
        return StringUtils.isNotEmpty(spaceName) ? spaceName :namespace;
    }
}
