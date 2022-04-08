package dev.nocalhost.plugin.intellij.ui.tree;

import com.google.common.collect.Lists;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.ui.treeStructure.Tree;

import org.apache.commons.lang3.StringUtils;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.tree.TreePath;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.ui.action.application.AppPortForwardAction;
import dev.nocalhost.plugin.intellij.ui.action.application.ApplyAction;
import dev.nocalhost.plugin.intellij.ui.action.application.ClearAppPersisentDataAction;
import dev.nocalhost.plugin.intellij.ui.action.application.UninstallAppAction;
import dev.nocalhost.plugin.intellij.ui.action.application.UpgradeAppAction;
import dev.nocalhost.plugin.intellij.ui.action.cluster.RemoveClusterAction;
import dev.nocalhost.plugin.intellij.ui.action.cluster.RenameClusterAction;
import dev.nocalhost.plugin.intellij.ui.action.cluster.ViewClusterKubeConfigAction;
import dev.nocalhost.plugin.intellij.ui.action.namespace.CleanDevSpacePersistentDataAction;
import dev.nocalhost.plugin.intellij.ui.action.namespace.InstallApplicationAction;
import dev.nocalhost.plugin.intellij.ui.action.namespace.InstallStandaloneApplicationAction;
import dev.nocalhost.plugin.intellij.ui.action.namespace.ResetDevSpaceAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.AssociateLocalDirectoryAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.ClearPersistentDataAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.ConfigAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.CopyTerminalAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.DebugAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.EditManifestAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.EndDevelopAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.LogsAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.OpenProjectAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.PortForwardAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.ProxyDisconnectAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.ProxyReconnectAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.ResetAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.RunAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.StartDevelopAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.ProxyConnectAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.TerminalAction;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import dev.nocalhost.plugin.intellij.utils.PathsUtil;

import static dev.nocalhost.plugin.intellij.utils.Constants.VPN_UNHEALTHY;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_POD;
import static dev.nocalhost.plugin.intellij.utils.Constants.ALL_WORKLOAD_TYPES;
import static dev.nocalhost.plugin.intellij.utils.Constants.DEV_MODE_DUPLICATE;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_SERVICE;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_DAEMONSET;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_DEPLOYMENT;
import static dev.nocalhost.plugin.intellij.utils.Constants.DEFAULT_APPLICATION_NAME;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_STATEFULSET;

public class TreeMouseListener extends MouseAdapter {
    private static final Separator SEPARATOR = new Separator();

    private final Project project;
    private final Tree tree;

    public TreeMouseListener(Tree tree, Project project) {
        this.tree = tree;
        this.project = project;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        if (event.getButton() == MouseEvent.BUTTON3) {
            TreePath treePath = tree.getClosestPathForLocation(event.getX(), event.getY());
            if (treePath == null) {
                return;
            }
            Object object = treePath.getLastPathComponent();

            if (object instanceof ClusterNode) {
                ClusterNode clusterNode = (ClusterNode) object;
                renderClusterAction(event, clusterNode);
                return;
            }

            if (object instanceof NamespaceNode) {
                NamespaceNode namespaceNode = (NamespaceNode) object;
                if (namespaceNode.isDevSpaceViewer()) {
                    return;
                }
                renderNamespaceAction(event, namespaceNode);
            }

            if (object instanceof ApplicationNode) {
                ApplicationNode applicationNode = (ApplicationNode) object;
                if (applicationNode.getNamespaceNode().isDevSpaceViewer()) {
                    return;
                }
                renderApplicationAction(event, applicationNode);
                return;
            }

            if (object instanceof ResourceNode) {
                ResourceNode resourceNode = (ResourceNode) object;
                if (resourceNode.getNamespaceNode().isDevSpaceViewer()) {
                    renderViewerWorkloadAction(event, resourceNode);
                } else {
                    renderWorkloadAction(event, resourceNode);
                }
                return;
            }
        }
    }

    private void renderClusterAction(MouseEvent event, ClusterNode clusterNode) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new ViewClusterKubeConfigAction(project, clusterNode));

        actionGroup.add(SEPARATOR);
        if (clusterNode.getServiceAccount() == null) {
            actionGroup.add(new RenameClusterAction(project, clusterNode));
            actionGroup.add(new RemoveClusterAction(project, clusterNode));
        }

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Cluster.Actions", actionGroup);
        JBPopupMenu.showByEvent(event, menu.getComponent());
    }

    private void renderNamespaceAction(MouseEvent event, NamespaceNode namespaceNode) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        if (namespaceNode.getClusterNode().getNocalhostAccount() != null) {
            actionGroup.add(new InstallApplicationAction(project, namespaceNode));
            actionGroup.add(SEPARATOR);
            actionGroup.add(new ResetDevSpaceAction(project, namespaceNode));
        } else {
            actionGroup.add(new InstallStandaloneApplicationAction(project, namespaceNode));
        }
        actionGroup.add(SEPARATOR);
        actionGroup.add(new CleanDevSpacePersistentDataAction(project, namespaceNode));

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Namespace.Actions", actionGroup);
        JBPopupMenu.showByEvent(event, menu.getComponent());
    }

    private void renderApplicationAction(MouseEvent event, ApplicationNode applicationNode) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        if (StringUtils.equals(applicationNode.getName(), DEFAULT_APPLICATION_NAME)) {
            actionGroup.add(new AppPortForwardAction(project, applicationNode));
            actionGroup.add(new ClearAppPersisentDataAction(project, applicationNode));
            actionGroup.add(new ApplyAction(project, applicationNode));

        } else {
            actionGroup.add(new UninstallAppAction(project, applicationNode));
            actionGroup.add(SEPARATOR);

            actionGroup.add(new AppPortForwardAction(project, applicationNode));
            actionGroup.add(new ClearAppPersisentDataAction(project, applicationNode));
            actionGroup.add(new ApplyAction(project, applicationNode));

            if (applicationNode.getNamespaceNode().getClusterNode().getServiceAccount() != null) {
                actionGroup.add(new UpgradeAppAction(project, applicationNode));
            }
        }

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Application.Actions", actionGroup);
        JBPopupMenu.showByEvent(event, menu.getComponent());
    }

    private void renderWorkloadAction(MouseEvent event, ResourceNode resourceNode) {
        String kind = resourceNode.controllerType().toLowerCase();
        if (!ALL_WORKLOAD_TYPES.contains(kind) && !resourceNode.isCrd()) {
            return;
        }

        DefaultActionGroup actionGroup = new DefaultActionGroup();

        // the workload is in proxy mode
        var vpn = resourceNode.getVpn();
        if (vpn != null) {
            if (vpn.isBelongsToMe() && StringUtils.equals(vpn.getStatus(), VPN_UNHEALTHY)) {
                actionGroup.add(new ProxyReconnectAction(project, resourceNode));
            }
            actionGroup.add(new ProxyDisconnectAction(project, resourceNode));

            ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Workload.Actions", actionGroup);
            JBPopupMenu.showByEvent(event, menu.getComponent());
            return;
        }

        if (StringUtils.equals(kind, WORKLOAD_TYPE_SERVICE)) {
            actionGroup.add(new ProxyConnectAction(project, resourceNode));
            ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Workload.Actions", actionGroup);
            JBPopupMenu.showByEvent(event, menu.getComponent());
            return;
        }

        NhctlDescribeService nhctlDescribeService = resourceNode.getNhctlDescribeService();
        if (NhctlDescribeServiceUtil.isDeveloping(nhctlDescribeService)) {
            actionGroup.add(new EndDevelopAction(project, resourceNode));

            if ( ! nhctlDescribeService.isPossess()) {
                actionGroup.add(StartDevelopAction.duplicate(project, resourceNode));
                actionGroup.add(StartDevelopAction.duplicateMesh(project, resourceNode));
            }
            actionGroup.add(new RunAction(project, resourceNode));
            actionGroup.add(new DebugAction(project, resourceNode));
            actionGroup.add(SEPARATOR);
        } else {
            actionGroup.add(new StartDevelopAction(project, resourceNode));
            actionGroup.add(StartDevelopAction.duplicate(project, resourceNode));
            actionGroup.add(StartDevelopAction.duplicateMesh(project, resourceNode));
            actionGroup.add(new RunAction(project, resourceNode));
            actionGroup.add(new DebugAction(project, resourceNode));
            actionGroup.add(SEPARATOR);
            if (proxyable(resourceNode, kind)) {
                actionGroup.add(new ProxyConnectAction(project, resourceNode));
                actionGroup.add(SEPARATOR);
            }
        }

        actionGroup.add(new AssociateLocalDirectoryAction(project, resourceNode));
        actionGroup.add(new ConfigAction(project, resourceNode));

        // https://nocalhost.coding.net/p/nocalhost/subtasks/issues/792/detail
        if ( ! NhctlDescribeServiceUtil.isDuplicateMode(nhctlDescribeService)) {
            actionGroup.add(new EditManifestAction(project, resourceNode));
        }

        actionGroup.add(new PortForwardAction(project, resourceNode));
        actionGroup.add(new LogsAction(project, resourceNode));
        actionGroup.add(SEPARATOR);
        actionGroup.add(new ResetAction(project, resourceNode));
        actionGroup.add(new ClearPersistentDataAction(project, resourceNode));
        actionGroup.add(SEPARATOR);
        actionGroup.add(new TerminalAction(project, resourceNode));
        actionGroup.add(new CopyTerminalAction(project, resourceNode));

        if (NhctlDescribeServiceUtil.developStarted(nhctlDescribeService)
                && PathsUtil.isDiff(project.getBasePath(), resourceNode.getNhctlDescribeService().getAssociate())) {
            actionGroup.add(SEPARATOR);
            actionGroup.add(new OpenProjectAction(project, resourceNode));
        }

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Workload.Actions", actionGroup);
        JBPopupMenu.showByEvent(event, menu.getComponent());
    }

    private void renderViewerWorkloadAction(MouseEvent event, ResourceNode resourceNode) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new PortForwardAction(project, resourceNode));
        actionGroup.add(new LogsAction(project, resourceNode));
        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Workload.Actions", actionGroup);
        JBPopupMenu.showByEvent(event, menu.getComponent());
    }

    private boolean proxyable(ResourceNode node, String kind) {
        if (node.isCrd()) {
            return true;
        }

        return Lists.newArrayList(
                WORKLOAD_TYPE_DEPLOYMENT,
                WORKLOAD_TYPE_STATEFULSET,
                WORKLOAD_TYPE_DAEMONSET,
                WORKLOAD_TYPE_SERVICE,
                WORKLOAD_TYPE_POD
        ).contains(kind);
    }
}
