package dev.nocalhost.plugin.intellij.ui.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.ui.tree.node.AccountNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;
import icons.NocalhostIcons;

public class TreeNodeRenderer extends ColoredTreeCellRenderer {
    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
        if (value instanceof LoadingNode) {
            if (!selected) setForeground(UIUtil.getInactiveTextColor());
            setIcon(JBUIScale.scaleIcon(EmptyIcon.create(8, 16)));
            return;
        }

        if (value instanceof dev.nocalhost.plugin.intellij.ui.tree.node.AccountNode) {
            dev.nocalhost.plugin.intellij.ui.tree.node.AccountNode node = (AccountNode) value;
            append("Hi, " + node.getUserInfo().getName());
            setIcon(AllIcons.General.User);
        }

        if (value instanceof DevSpaceNode) {
            DevSpaceNode node = (DevSpaceNode) value;
            append(node.getDevSpace().getSpaceName());
            if (node.isInstalled()) {
                setIcon(NocalhostIcons.App.Connected);
            } else {
                setIcon(NocalhostIcons.App.Inactive);
            }
        }

        if (value instanceof ResourceGroupNode) {
            ResourceGroupNode node = (ResourceGroupNode) value;
            append(node.getName());
        }

        if (value instanceof ResourceTypeNode) {
            ResourceTypeNode node = (ResourceTypeNode) value;
            append(node.getName());
        }

        if (value instanceof ResourceNode) {
            ResourceNode node = (ResourceNode) value;
            append(node.getKubeResource().getMetadata().getName());

            if (StringUtils.equalsIgnoreCase(node.getKubeResource().getKind(), "Deployment")) {
                Icon icon = getDeploymentIcon(node);
                setIcon(icon);
            }
        }
    }

    protected Icon getDeploymentIcon(ResourceNode node) {
        final NhctlDescribeService nhctlDescribeService = node.getNhctlDescribeService();

        DeploymentStatus status = getDeploymentStatus(node);
        switch (status) {
            case DEVELOPING:
                if (nhctlDescribeService != null && nhctlDescribeService.isPortForwarded()) {
                    return NocalhostIcons.Status.DevPortForwarding;
                }
                return NocalhostIcons.Status.DevStart;
            case RUNNING:
                if (nhctlDescribeService != null && nhctlDescribeService.isPortForwarded()) {
                    return NocalhostIcons.Status.NormalPortForwarding;
                }
                return NocalhostIcons.Status.Running;
            case STARTING:
                return NocalhostIcons.Status.Loading;
            case UNKNOWN:
                return NocalhostIcons.Status.Unknown;
            default:
                throw new IllegalStateException("Unexpected value: " + status);
        }
    }

    protected DeploymentStatus getDeploymentStatus(ResourceNode node) {
        DeploymentStatus status = DeploymentStatus.UNKNOWN;
        final NhctlDescribeService nhctlDescribeService = node.getNhctlDescribeService();
        if (nhctlDescribeService != null && nhctlDescribeService.isDeveloping()) {
            return DeploymentStatus.DEVELOPING;
        }
        boolean available = false;
        boolean progressing = false;
        List<KubeResource.Status.Condition> conditions = node.getKubeResource().getStatus()
                .getConditions();
        for (KubeResource.Status.Condition condition : conditions) {
            if (StringUtils.equals(condition.getType(), "Available")
                    && StringUtils.equals(condition.getStatus(), "True")) {
                status = DeploymentStatus.RUNNING;
                available = true;
            } else if (StringUtils.equals(condition.getType(), "Progressing")
                    && StringUtils.equals(condition.getStatus(), "True")) {
                progressing = true;
            }
        }
        if (progressing && !available) {
            status = DeploymentStatus.STARTING;
        }
        return status;
    }
}