package dev.nocalhost.plugin.intellij.ui.tree;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.UIUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.Condition;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.Status;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdKindNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdRootNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import icons.NocalhostIcons;

import static dev.nocalhost.plugin.intellij.utils.Constants.ALL_WORKLOAD_TYPES;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_DEPLOYMENT;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_JOB;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_POD;

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
            setToolTipText("loading...");
            return;
        }

        if (value instanceof ClusterNode) {
            ClusterNode node = (ClusterNode) value;
            append(node.getName());
            append("  ");
            if (node.isActive()) {
                append("ACTIVE", SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
                setIcon(NocalhostIcons.ClusterActive);
                String accountInfo = node.getAccountInfo();
                if (StringUtils.isNotEmpty(accountInfo)) {
                    setToolTipText(node.getName() + " [" + accountInfo + "]");
                } else {
                    setToolTipText(node.getName());
                }
            } else {
                append("Unable to Connect", SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES);
                setIcon(NocalhostIcons.ClusterWarning);
                setToolTipText(node.getInfo());
            }
        }

        if (value instanceof NamespaceNode) {
            NamespaceNode node = (NamespaceNode) value;
            append(node.getName());
            setToolTipText(node.getName());
            if (node.isDevSpaceViewer()) {
                setIcon(NocalhostIcons.DevSpaceViewer);
            } else {
                setIcon(NocalhostIcons.DevSpace);
            }
        }

        if (value instanceof ApplicationNode) {
            ApplicationNode node = (ApplicationNode) value;
            append(node.getName());
            setIcon(NocalhostIcons.App.Connected);
            setToolTipText(node.getName());
        }

        if (value instanceof ResourceGroupNode) {
            ResourceGroupNode node = (ResourceGroupNode) value;
            append(node.getName());
            setToolTipText(node.getName());
        }

        if (value instanceof ResourceTypeNode) {
            ResourceTypeNode node = (ResourceTypeNode) value;
            append(node.getName());
            setToolTipText(node.getName());
        }

        if (value instanceof CrdRootNode) {
            var node = (CrdRootNode) value;
            append(node.getName());
            setToolTipText(node.getName());
        }

        if (value instanceof CrdGroupNode) {
            var node = (CrdGroupNode) value;
            append(node.getName());
            setToolTipText(node.getName());
        }

        if (value instanceof CrdKindNode) {
            var node = (CrdKindNode) value;
            append(node.getName());
            setToolTipText(node.getName());
        }

        if (value instanceof ResourceNode) {
            ResourceNode node = (ResourceNode) value;
            append(node.getKubeResource().getMetadata().getName());

            Icon icon = getWorkloadIcon(node);
            if (icon != null) {
                setIcon(icon);
            }

            String tips = node.getKubeResource().getMetadata().getName();
            if (StringUtils.equalsIgnoreCase(node.controllerType(), WORKLOAD_TYPE_JOB)) {
                tips += "(" + getJobStatus(node) + ")";
            }
            setToolTipText(tips);
        }
    }

    private Icon getWorkloadIcon(ResourceNode node) {
        String resourceType = node.controllerType().toLowerCase();
        if (!ALL_WORKLOAD_TYPES.contains(resourceType) && !node.isCrd()) {
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
                // https://nocalhost.coding.net/p/nocalhost/wiki/551#user-content-jetbrains
                if (NhctlDescribeServiceUtil.isDuplicateMode(nhctlDescribeService)) {
                    if (nhctlPortForwards.isEmpty()) {
                        return NocalhostIcons.Status.DevCopy;
                    }
                    return NocalhostIcons.Status.DevCopyWithPortForwarding;
                }
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
            case FAILED:
                return NocalhostIcons.Status.Failed;
            default:
                return NocalhostIcons.Status.Unknown;
        }
    }

    private String getJobStatus(ResourceNode node) {
        ServiceStatus status = getServiceStatus(node);
        switch (status) {
            case RUNNING:
                return "complete";
            case FAILED:
                return "failed";
            default:
                return "unknown";
        }
    }

    private ServiceStatus getServiceStatus(ResourceNode node) {
        ServiceStatus status = ServiceStatus.UNKNOWN;
        NhctlDescribeService nhctlDescribeService = node.getNhctlDescribeService();
        if (NhctlDescribeServiceUtil.developStarting(nhctlDescribeService)) {
            return ServiceStatus.STARTING;
        }
        if (NhctlDescribeServiceUtil.developStarted(nhctlDescribeService)) {
            return ServiceStatus.DEVELOPING;
        }
        if (node.getKubeResource().getStatus() == null) {
            return status;
        }

        boolean available = false;
        boolean progressing = false;
        List<Condition> conditions = node.getKubeResource().getStatus().getConditions();
        if (conditions != null) {
            switch (node.controllerType().toLowerCase()) {
                case WORKLOAD_TYPE_DEPLOYMENT:
                    for (Condition condition : conditions) {
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
                    break;

                case WORKLOAD_TYPE_JOB:
                    for (Condition condition : conditions) {
                        if (StringUtils.equals(condition.getType(), "Complete")
                                && StringUtils.equals(condition.getStatus(), "True")) {
                            status = ServiceStatus.RUNNING;
                        } else if (StringUtils.equals(condition.getType(), "Failed")
                                && StringUtils.equals(condition.getStatus(), "True")) {
                            status = ServiceStatus.FAILED;
                        }
                    }
                    break;

                case WORKLOAD_TYPE_POD:
                    for (Condition condition : conditions) {
                        if (StringUtils.equals(condition.getType(), "Ready")
                                && StringUtils.equals(condition.getStatus(), "True")) {
                            status = ServiceStatus.RUNNING;
                        }
                    }
                    break;

                default:
            }
        } else {
            Status kubeStatus = node.getKubeResource().getStatus();
            if (kubeStatus.getReplicas() == kubeStatus.getReadyReplicas()) {
                status = ServiceStatus.RUNNING;
            }
        }
        return status;
    }
}