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

import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.Condition;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.Status;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;
import icons.NocalhostIcons;

import static dev.nocalhost.plugin.intellij.utils.Constants.ALL_WORKLOAD_TYPES;
import static dev.nocalhost.plugin.intellij.utils.Constants.PRIVILEGE_TYPE_CLUSTER_ADMIN;
import static dev.nocalhost.plugin.intellij.utils.Constants.PRIVILEGE_TYPE_CLUSTER_VIEWER;
import static dev.nocalhost.plugin.intellij.utils.Constants.SPACE_OWN_TYPE_VIEWER;
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
            if (isNamespaceViewer(node)) {
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

        if (value instanceof ResourceNode) {
            ResourceNode node = (ResourceNode) value;
            append(node.getKubeResource().getMetadata().getName());

            Icon icon = getWorkloadIcon(node);
            if (icon != null) {
                setIcon(icon);
            }

            String tips = node.getKubeResource().getMetadata().getName();
            if (StringUtils.equals(node.getKubeResource().getKind().toLowerCase(), WORKLOAD_TYPE_JOB)) {
                tips += "(" + getJobStatus(node) + ")";
            }
            setToolTipText(tips);
        }
    }

    private Icon getWorkloadIcon(ResourceNode node) {
        String resourceType = node.getKubeResource().getKind().toLowerCase();
        if (!ALL_WORKLOAD_TYPES.contains(resourceType)) {
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
        if (nhctlDescribeService.isDeveloping()) {
            return ServiceStatus.DEVELOPING;
        }
        boolean available = false;
        boolean progressing = false;
        List<Condition> conditions = node.getKubeResource().getStatus()
                .getConditions();
        if (conditions != null) {
            switch (node.getKubeResource().getKind().toLowerCase()) {
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

    private boolean isNamespaceViewer(NamespaceNode node) {
        ServiceAccount serviceAccount = node.getClusterNode().getServiceAccount();
        if (serviceAccount == null) {
            return false;
        }
        if (serviceAccount.isPrivilege()) {
            if (StringUtils.equals(serviceAccount.getPrivilegeType(), PRIVILEGE_TYPE_CLUSTER_ADMIN)) {
                return false;
            }
            if (StringUtils.equals(serviceAccount.getPrivilegeType(), PRIVILEGE_TYPE_CLUSTER_VIEWER)) {
                if (node.getNamespacePack() != null) {
                    return StringUtils.equals(node.getNamespacePack().getSpaceOwnType(), SPACE_OWN_TYPE_VIEWER);
                } else {
                    return true;
                }
            }
            return true;
        } else {
            return StringUtils.equals(node.getNamespacePack().getSpaceOwnType(), SPACE_OWN_TYPE_VIEWER);
        }
    }
}