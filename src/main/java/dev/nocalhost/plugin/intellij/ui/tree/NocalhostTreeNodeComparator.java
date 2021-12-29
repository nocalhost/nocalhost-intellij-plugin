package dev.nocalhost.plugin.intellij.ui.tree;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

import javax.swing.tree.TreeNode;

import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdKindNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

import static dev.nocalhost.plugin.intellij.utils.Constants.DEFAULT_APPLICATION_NAME;

public class NocalhostTreeNodeComparator implements Comparator<TreeNode> {
    @Override
    public int compare(TreeNode o1, TreeNode o2) {
        if (o1 instanceof ClusterNode && o2 instanceof ClusterNode) {
            ClusterNode n1 = (ClusterNode) o1;
            ClusterNode n2 = (ClusterNode) o2;
            return n1.getName().compareTo(n2.getName());
        }

        if (o1 instanceof NamespaceNode && o2 instanceof NamespaceNode) {
            NamespaceNode n1 = (NamespaceNode) o1;
            NamespaceNode n2 = (NamespaceNode) o2;
            return n1.getNamespace().compareTo(n2.getNamespace());
        }

        if (o1 instanceof ApplicationNode && o2 instanceof ApplicationNode) {
            ApplicationNode n1 = (ApplicationNode) o1;
            if (StringUtils.equals(n1.getName(), DEFAULT_APPLICATION_NAME)) {
                return 1;
            }
            ApplicationNode n2 = (ApplicationNode) o2;
            if (StringUtils.equals(n2.getName(), DEFAULT_APPLICATION_NAME)) {
                return -1;
            }
            return n1.getName().compareTo(n2.getName());
        }

        if (o1 instanceof ResourceNode && o2 instanceof ResourceNode) {
            ResourceNode n1 = (ResourceNode) o1;
            ResourceNode n2 = (ResourceNode) o2;
            return n1.getKubeResource().getMetadata().getName()
                    .compareTo(n2.getKubeResource().getMetadata().getName());
        }

        if (o1 instanceof CrdGroupNode && o2 instanceof CrdGroupNode) {
            var a = (CrdGroupNode) o1;
            var b = (CrdGroupNode) o2;
            return a.getName().compareTo(b.getName());
        }

        if (o1 instanceof CrdKindNode && o2 instanceof CrdKindNode) {
            var a = (CrdKindNode) o1;
            var b = (CrdKindNode) o2;
            return a.getName().compareTo(b.getName());
        }

        throw new ClassCastException();
    }
}
