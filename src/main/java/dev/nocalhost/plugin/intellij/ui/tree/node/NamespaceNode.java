package dev.nocalhost.plugin.intellij.ui.tree.node;

import org.apache.commons.lang3.StringUtils;

import javax.swing.tree.DefaultMutableTreeNode;

import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import lombok.Getter;

import static dev.nocalhost.plugin.intellij.utils.Constants.PRIVILEGE_TYPE_CLUSTER_ADMIN;
import static dev.nocalhost.plugin.intellij.utils.Constants.PRIVILEGE_TYPE_CLUSTER_VIEWER;
import static dev.nocalhost.plugin.intellij.utils.Constants.SPACE_OWN_TYPE_VIEWER;

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
        return inNamespacePack()
                ? String.format("%s (%s)", namespacePack.getSpaceName(), namespacePack.getNamespace())
                : namespace;
    }

    public boolean isDevSpaceViewer() {
        if (this.getClusterNode() == null) {
            return false;
        }
        ServiceAccount serviceAccount = this.getClusterNode().getServiceAccount();
        if (serviceAccount == null) {
            return false;
        }
        if (serviceAccount.isPrivilege()) {
            if (StringUtils.equals(serviceAccount.getPrivilegeType(), PRIVILEGE_TYPE_CLUSTER_ADMIN)) {
                return false;
            }
            if (StringUtils.equals(serviceAccount.getPrivilegeType(), PRIVILEGE_TYPE_CLUSTER_VIEWER)) {
                if (inNamespacePack()) {
                    return StringUtils.equals(this.getNamespacePack().getSpaceOwnType(), SPACE_OWN_TYPE_VIEWER);
                } else {
                    return true;
                }
            }
            return false;
        } else {
            return StringUtils.equals(this.getNamespacePack().getSpaceOwnType(), SPACE_OWN_TYPE_VIEWER);
        }
    }

    public boolean inNamespacePack() {
        return this.getNamespacePack() != null;
    }

    public void updateFrom(NamespaceNode o) {
        this.namespace = o.namespace;
        this.namespacePack = o.namespacePack;
    }
}
