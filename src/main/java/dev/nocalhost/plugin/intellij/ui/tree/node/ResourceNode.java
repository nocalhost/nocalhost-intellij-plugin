package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import lombok.Getter;

@Getter
public class ResourceNode extends DefaultMutableTreeNode {
    private KubeResource kubeResource;
    private NhctlDescribeService nhctlDescribeService;

    public ResourceNode(KubeResource kubeResource) {
        this(kubeResource, null);
    }

    public ResourceNode(KubeResource kubeResource, NhctlDescribeService nhctlDescribeService) {
        this.kubeResource = kubeResource;
        this.nhctlDescribeService = nhctlDescribeService;
    }

    public String resourceName() {
        return kubeResource.getMetadata().getName();
    }

    public String applicationName() {
        return ((ApplicationNode) this.getParent().getParent().getParent()).getName();
    }

    public NamespaceNode getNamespaceNode() {
        return (NamespaceNode) this.getParent().getParent().getParent().getParent();
    }

    public ClusterNode getClusterNode() {
        return (ClusterNode) getNamespaceNode().getParent();
    }

    public void updateFrom(ResourceNode o) {
        this.kubeResource = o.kubeResource;
        this.nhctlDescribeService = o.nhctlDescribeService;
    }
}