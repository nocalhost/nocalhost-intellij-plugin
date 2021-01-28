package dev.nocalhost.plugin.intellij.ui.tree.node;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSvcProfile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ResourceNode extends DefaultMutableTreeNode {
    private KubeResource kubeResource;
    private NhctlSvcProfile nhctlSvcProfile;

    public ResourceNode(KubeResource kubeResource) {
        this(kubeResource, null);
    }

    public ResourceNode clone() {
        return new ResourceNode(kubeResource, nhctlSvcProfile);
    }

    public DevSpace devSpace() {
        TreeNode node = this;
        for (int i = 0; i < 3; i++) {
            if (node != null) {
                node = node.getParent();
            } else {
                return null;
            }
        }
        return ((DevSpaceNode) node).getDevSpace();
    }

    public String resourceName() {
        return kubeResource.getMetadata().getName();
    }
}