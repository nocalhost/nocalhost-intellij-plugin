package dev.nocalhost.plugin.intellij.ui.tree;

import java.util.Comparator;

import javax.swing.tree.TreeNode;

import dev.nocalhost.plugin.intellij.ui.tree.node.AccountNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.DefaultResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class NocalhostTreeNodeComparator implements Comparator<TreeNode> {
    @Override
    public int compare(TreeNode o1, TreeNode o2) {
        if (o1 instanceof AccountNode && o2 instanceof DevSpaceNode) {
            return -1;
        }
        if (o1 instanceof DevSpaceNode && o2 instanceof AccountNode) {
            return 1;
        }
        if (o1 instanceof DevSpaceNode && o2 instanceof DevSpaceNode) {
            DevSpaceNode n1 = (DevSpaceNode) o1;
            DevSpaceNode n2 = (DevSpaceNode) o2;
            return n1.getDevSpace().getSpaceName().compareTo(n2.getDevSpace().getSpaceName());
        }

        if (o1 instanceof ApplicationNode && o2 instanceof DefaultResourceNode) {
            return -1;
        }
        if (o1 instanceof DefaultResourceNode && o2 instanceof ApplicationNode) {
            return 1;
        }
        if (o1 instanceof ApplicationNode && o2 instanceof ApplicationNode) {
            ApplicationNode n1 = (ApplicationNode) o1;
            ApplicationNode n2 = (ApplicationNode) o2;
            return n1.getApplication().getContext().getApplicationName()
                    .compareTo(n2.getApplication().getContext().getApplicationName());
        }

        if (o1 instanceof ResourceNode && o2 instanceof ResourceNode) {
            ResourceNode n1 = (ResourceNode) o1;
            ResourceNode n2 = (ResourceNode) o2;
            return n1.getKubeResource().getMetadata().getName()
                    .compareTo(n2.getKubeResource().getMetadata().getName());
        }

        throw new ClassCastException();
    }
}
