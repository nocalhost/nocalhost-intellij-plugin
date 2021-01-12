package dev.nocalhost.plugin.intellij.ui.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import icons.NocalhostIcons;

public class NodeRenderer extends ColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        final Object userObject = ((DefaultMutableTreeNode) value).getUserObject();

        if (userObject instanceof AccountNode) {
            AccountNode node = (AccountNode) userObject;
            setIcon(AllIcons.General.User);
            append("Hi, " + node.getName());
            return;
        }

        if (userObject instanceof DevSpaceNode) {
            DevSpaceNode node = (DevSpaceNode) userObject;
            if (node.getDevSpace().getInstallStatus() == 0) {
                setIcon(NocalhostIcons.App.Inactive);
            } else if (node.getDevSpace().getInstallStatus() == 1) {
                setIcon(NocalhostIcons.App.Connected);
            }
            append(node.getDevSpace().getSpaceName());
            return;
        }

        if (userObject instanceof PlainNode) {
            PlainNode node = (PlainNode) userObject;
            append(node.getName());
            return;
        }

        if (userObject instanceof WorkloadNode) {
            WorkloadNode node = (WorkloadNode) userObject;
            switch (node.getStatus()) {
                case RUNNING:
                    setIcon(NocalhostIcons.Status.Running);
                    break;
                case UNKNOWN:
                    setIcon(NocalhostIcons.Status.Unknown);
                    break;
                case STARTING:
                    setIcon(NocalhostIcons.Status.Loading);
                    break;
                default:
                    // Empty
            }
            append(node.getName());
            return;
        }
    }
}
