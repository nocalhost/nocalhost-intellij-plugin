package dev.nocalhost.plugin.intellij.ui.tree;

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
import dev.nocalhost.plugin.intellij.ui.action.application.ConfigAppAction;
import dev.nocalhost.plugin.intellij.ui.action.application.LoadResourceAction;
import dev.nocalhost.plugin.intellij.ui.action.application.UninstallAppAction;
import dev.nocalhost.plugin.intellij.ui.action.application.UpgradeAppAction;
import dev.nocalhost.plugin.intellij.ui.action.application.UpgradeStandaloneApplicationAction;
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
import dev.nocalhost.plugin.intellij.ui.action.workload.EditManifestAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.EndDevelopAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.LogsAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.OpenProjectAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.PortForwardAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.ResetAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.StartDevelopAction;
import dev.nocalhost.plugin.intellij.ui.action.workload.TerminalAction;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.PathsUtil;

import static dev.nocalhost.plugin.intellij.utils.Constants.ALL_WORKLOAD_TYPES;
import static dev.nocalhost.plugin.intellij.utils.Constants.DEFAULT_APPLICATION_NAME;

public class TreeMouseListener extends MouseAdapter {
    private final Project project;
    private final Tree tree;

    public TreeMouseListener(Tree tree, Project project) {
        this.tree = tree;
        this.project = project;
    }

    @Override
    public void mouseReleased(MouseEvent event) {
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
                renderNamespaceAction(event, namespaceNode);
            }

            if (object instanceof ApplicationNode) {
                ApplicationNode applicationNode = (ApplicationNode) object;
                renderApplicationAction(event, applicationNode);
                return;
            }

            if (object instanceof ResourceNode) {
                ResourceNode resourceNode = (ResourceNode) object;
                renderWorkloadAction(event, resourceNode);
                return;
            }
        }
    }

    private void renderClusterAction(MouseEvent event, ClusterNode clusterNode) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new ViewClusterKubeConfigAction(project, clusterNode));

        actionGroup.add(new Separator());
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
            actionGroup.add(new Separator());
            actionGroup.add(new ResetDevSpaceAction(project, namespaceNode));
        } else {
            actionGroup.add(new InstallStandaloneApplicationAction(project, namespaceNode));
        }
        actionGroup.add(new Separator());
        actionGroup.add(new CleanDevSpacePersistentDataAction(project, namespaceNode));

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Namespace.Actions", actionGroup);
        JBPopupMenu.showByEvent(event, menu.getComponent());
    }

    private void renderApplicationAction(MouseEvent event, ApplicationNode applicationNode) {
        if (StringUtils.equals(applicationNode.getName(), DEFAULT_APPLICATION_NAME)) {
            return;
        }

        DefaultActionGroup actionGroup = new DefaultActionGroup();

        actionGroup.add(new UninstallAppAction(project, applicationNode));

        actionGroup.add(new Separator());
        actionGroup.add(new ApplyAction(project, applicationNode));
        actionGroup.add(new ConfigAppAction(project, applicationNode));
        actionGroup.add(new ClearAppPersisentDataAction(project, applicationNode));
        actionGroup.add(new Separator());
        if (applicationNode.getNamespaceNode().getClusterNode().getServiceAccount() != null) {
            actionGroup.add(new UpgradeAppAction(project, applicationNode));
        } else {
            actionGroup.add(new UpgradeStandaloneApplicationAction(project, applicationNode));
        }

        actionGroup.add(new Separator());
        actionGroup.add(new LoadResourceAction(project, applicationNode));

        actionGroup.add(new Separator());
        actionGroup.add(new AppPortForwardAction(project, applicationNode));

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Application.Actions", actionGroup);
        JBPopupMenu.showByEvent(event, menu.getComponent());
    }

    private void renderWorkloadAction(MouseEvent event, ResourceNode resourceNode) {
        String resourceType = resourceNode.getKubeResource().getKind().toLowerCase();
        if (!ALL_WORKLOAD_TYPES.contains(resourceType)) {
            return;
        }

        DefaultActionGroup actionGroup = new DefaultActionGroup();

        NhctlDescribeService nhctlDescribeService = resourceNode.getNhctlDescribeService();
        if (!nhctlDescribeService.isDeveloping()) {
            actionGroup.add(new StartDevelopAction(project, resourceNode));
        } else {
            if (!PathsUtil.isSame(project.getBasePath(), resourceNode.getNhctlDescribeService().getAssociate())) {
                actionGroup.add(new OpenProjectAction(project, resourceNode));
            }
            actionGroup.add(new EndDevelopAction(project, resourceNode));
        }

        actionGroup.add(new Separator());
        actionGroup.add(new EditManifestAction(project, resourceNode));
        actionGroup.add(new Separator());
        actionGroup.add(new ConfigAction(project, resourceNode));
        actionGroup.add(new AssociateLocalDirectoryAction(project, resourceNode));
        actionGroup.add(new CopyTerminalAction(project, resourceNode));
        actionGroup.add(new Separator());
        actionGroup.add(new ClearPersistentDataAction(project, resourceNode));
        actionGroup.add(new Separator());
        actionGroup.add(new LogsAction(project, resourceNode));
        actionGroup.add(new PortForwardAction(project, resourceNode));
        actionGroup.add(new ResetAction(project, resourceNode));
        actionGroup.add(new TerminalAction(project, resourceNode));

        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu("Nocalhost.Workload.Actions", actionGroup);
        JBPopupMenu.showByEvent(event, menu.getComponent());
    }
}
