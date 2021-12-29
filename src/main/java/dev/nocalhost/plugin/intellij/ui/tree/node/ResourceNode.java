package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.KubeResource;
import lombok.Getter;

@Getter
public class ResourceNode extends DefaultMutableTreeNode {
    private final boolean crd;
    private KubeResource kubeResource;
    private NhctlDescribeService nhctlDescribeService;

    public ResourceNode(KubeResource kubeResource, NhctlDescribeService nhctlDescribeService, boolean crd) {
        this.crd = crd;
        this.kubeResource = kubeResource;
        this.nhctlDescribeService = nhctlDescribeService;
    }

    public ResourceNode(KubeResource kubeResource, NhctlDescribeService nhctlDescribeService) {
        this(kubeResource, nhctlDescribeService, false);
    }

    public String resourceName() {
        return kubeResource.getMetadata().getName();
    }

    public String controllerType() {
        if (crd) {
            return ((CrdKindNode) getParent()).getResourceType();
        }
        return kubeResource.getKind();
    }

    public String applicationName() {
        var owner = getParent();
        while (owner != null) {
            if (owner instanceof ApplicationNode) {
                return ((ApplicationNode) owner).getName();
            }
            owner = owner.getParent();
        }
        return null;
    }

    public NamespaceNode getNamespaceNode() {
        var owner = getParent();
        while (owner != null) {
            if (owner instanceof NamespaceNode) {
                return (NamespaceNode) owner;
            }
            owner = owner.getParent();
        }
        return null;
    }

    public ClusterNode getClusterNode() {
        var owner = getParent();
        while (owner != null) {
            if (owner instanceof ClusterNode) {
                return (ClusterNode) owner;
            }
            owner = owner.getParent();
        }
        return null;
    }

    public void updateFrom(ResourceNode o) {
        this.kubeResource = o.kubeResource;
        this.nhctlDescribeService = o.nhctlDescribeService;
    }
}