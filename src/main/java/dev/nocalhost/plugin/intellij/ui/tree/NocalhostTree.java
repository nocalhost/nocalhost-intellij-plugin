package dev.nocalhost.plugin.intellij.ui.tree;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.messages.MessageBusConnection;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeSelectionModel;

import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;

public class NocalhostTree extends Tree implements Disposable {
    private final Project project;
    private final NocalhostTreeModel model;

    private MessageBusConnection messageBusConnection;

    public NocalhostTree(Project project) {
        this.project = project;
        this.model = new NocalhostTreeModel(project, this);
        this.setModel(this.model);

        init();
    }

    private void init() {
        this.setRootVisible(false);
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setCellRenderer(new TreeNodeRenderer());
        this.addMouseListener(new TreeMouseListener(this, project));
        this.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object node = event.getPath().getLastPathComponent();
                if (node instanceof ClusterNode) {
                    ClusterNode clusterNode = (ClusterNode) node;
                    model.insertLoadingNode(clusterNode);
                }
                if (node instanceof NamespaceNode) {
                    NamespaceNode namespaceNode = (NamespaceNode) node;
                    model.insertLoadingNode(namespaceNode);
                }
                if (node instanceof ResourceTypeNode) {
                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) node;
                    model.insertLoadingNode(resourceTypeNode);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {

            }
        });
        this.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                Object node = event.getPath().getLastPathComponent();
                if (node instanceof ClusterNode) {
                    ClusterNode clusterNode = (ClusterNode) node;
                    model.updateNamespaces(clusterNode);
                }
                if (node instanceof NamespaceNode) {
                    NamespaceNode namespaceNode = (NamespaceNode) node;
                    model.updateApplications(namespaceNode);
                }
                if (node instanceof ResourceTypeNode) {
                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) node;
                    model.updateResources(resourceTypeNode);
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {

            }
        });

        messageBusConnection = ApplicationManager.getApplication().getMessageBus().connect();
        messageBusConnection.subscribe(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC,
                model::update
        );
    }

    public void updateDevSpaces() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC
        ).action();
    }

    @Override
    public void dispose() {
        messageBusConnection.dispose();
    }
}