package dev.nocalhost.plugin.intellij.ui.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
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

        if (value instanceof ClusterNode) {
            ClusterNode node = (ClusterNode) value;
            if (node.getNocalhostAccount() != null) {
                append(node.getKubeConfig().getClusters().get(0).getName() + " ["
                        + node.getNocalhostAccount().getUsername() + " on "
                        + node.getNocalhostAccount().getServer() + "]");
            } else {
                append(node.getKubeConfig().getContexts().get(0).getName());
            }
            setIcon(AllIcons.Webreferences.Server);
        }

        if (value instanceof NamespaceNode) {
            NamespaceNode node = (NamespaceNode) value;
            append(node.getName());
        }

        if (value instanceof ApplicationNode) {
            ApplicationNode node = (ApplicationNode) value;
            append(node.getName());
            setIcon(NocalhostIcons.App.Connected);
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

            Icon icon = getWorkloadIcon(node);
            if (icon != null) {
                setIcon(icon);
            }
        }
    }

    private Icon getWorkloadIcon(ResourceNode node) {
        String workloadType = node.getKubeResource().getKind().toLowerCase();
        if (!Set.of("deployment", "statefulset" ,"daemonset", "job").contains(workloadType)) {
            return null;
        }

        NhctlDescribeService nhctlDescribeService = node.getNhctlDescribeService();
        List<NhctlPortForward> nhctlPortForwards = Lists.newArrayList();
        if (nhctlDescribeService.getDevPortForwardList() != null) {
            nhctlPortForwards = nhctlDescribeService
                    .getDevPortForwardList()
                    .stream()
                    .filter(pf -> !StringUtils.equalsIgnoreCase(pf.getRole(), "SYNC"))
                    .collect(Collectors.toList());
        }

        ServiceStatus status = getServiceStatus(node);
        switch (status) {
            case DEVELOPING:
                if (nhctlDescribeService.isPossess()) {
                    if (CollectionUtils.isNotEmpty(nhctlPortForwards)) {
                        return NocalhostIcons.Status.DevPortForwarding;
                    } else {
                        return NocalhostIcons.Status.DevStart;
                    }
                } else {
                    if (CollectionUtils.isNotEmpty(nhctlPortForwards)) {
                        return NocalhostIcons.Status.DevPortForwardingOther;
                    } else {
                        return NocalhostIcons.Status.DevOther;
                    }
                }
            case RUNNING:
                if (CollectionUtils.isNotEmpty(nhctlPortForwards)) {
                    return NocalhostIcons.Status.NormalPortForwarding;
                } else {
                    return NocalhostIcons.Status.Running;
                }
            case STARTING:
                return NocalhostIcons.Status.Loading;
            default:
                return NocalhostIcons.Status.Unknown;
        }
    }

    private ServiceStatus getServiceStatus(ResourceNode node) {
        ServiceStatus status = ServiceStatus.UNKNOWN;
        NhctlDescribeService nhctlDescribeService = node.getNhctlDescribeService();
        if (nhctlDescribeService.isDeveloping()) {
            return ServiceStatus.DEVELOPING;
        }
        boolean available = false;
        boolean progressing = false;
        List<KubeResource.Status.Condition> conditions = node.getKubeResource().getStatus()
                .getConditions();
        if (conditions != null) {
            for (KubeResource.Status.Condition condition : conditions) {
                if (StringUtils.equals(condition.getType(), "Available")
                        && StringUtils.equals(condition.getStatus(), "True")) {
                    status = ServiceStatus.RUNNING;
                    available = true;
                } else if (StringUtils.equals(condition.getType(), "Progressing")
                        && StringUtils.equals(condition.getStatus(), "True")) {
                    progressing = true;
                }
            }
            if (progressing && !available) {
                status = ServiceStatus.STARTING;
            }
        } else {
            KubeResource.Status kubeStatus = node.getKubeResource().getStatus();
            if (kubeStatus.getReplicas() == kubeStatus.getReadyReplicas()) {
                status = ServiceStatus.RUNNING;
            }
        }
        return status;
    }
}