package dev.nocalhost.plugin.intellij.ui.tree;

import com.google.common.collect.ImmutableMap;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.messages.MessageBusConnection;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import dev.nocalhost.plugin.intellij.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.service.NocalhostProjectService;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeExpandNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;

import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_CRONJOB;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_DAEMONSET;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_DEPLOYMENT;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_JOB;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_POD;
import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_STATEFULSET;

public class NocalhostTree extends Tree implements Disposable {
    private static final Map<String, String> RESOURCE_TYPE_MAP = ImmutableMap.<String, String>builder()
            .put("Deployments", WORKLOAD_TYPE_DEPLOYMENT)
            .put("DaemonSets", WORKLOAD_TYPE_DAEMONSET)
            .put("StatefulSets", WORKLOAD_TYPE_STATEFULSET)
            .put("Jobs", WORKLOAD_TYPE_JOB)
            .put("CronJobs", WORKLOAD_TYPE_CRONJOB)
            .put("Pods", WORKLOAD_TYPE_POD)
            .build();

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
        project.getMessageBus().connect(project).subscribe(
                NocalhostTreeExpandNotifier.NOCALHOST_TREE_EXPAND_NOTIFIER_TOPIC,
                this::expendWorkloadNode
        );
    }

    public void updateDevSpaces() {
        ApplicationManager.getApplication().getMessageBus().syncPublisher(
                NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC
        ).action();
    }

    private void expendWorkloadNode() {
        try {
            final ServiceProjectPath serviceProjectPath = project.getService(NocalhostProjectService.class)
                    .getServiceProjectPath();
            if (serviceProjectPath == null) {
                return;
            }
            final String rawKubeConfig = Files.readString(serviceProjectPath.getKubeConfigPath(), StandardCharsets.UTF_8);

            final Object root = model.getRoot();

            final long expired = System.currentTimeMillis() + 5 * 60 * 1000;

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                final AtomicReference<ClusterNode> clusterNodeAtomicReference = new AtomicReference<>(null);
                while (clusterNodeAtomicReference.get() == null && System.currentTimeMillis() < expired) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            for (int i = 0; i < model.getChildCount(root); i++) {
                                ClusterNode clusterNode = (ClusterNode) model.getChild(root, i);
                                if (StringUtils.equals(clusterNode.getRawKubeConfig(), rawKubeConfig)) {
                                    clusterNodeAtomicReference.set(clusterNode);
                                    model.updateNamespaces(clusterNode, true);
                                    break;
                                }
                            }

                        } catch (Exception ignore) {
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (Exception ignore) {
                    }
                }

                final AtomicReference<NamespaceNode> namespaceNodeAtomicReference = new AtomicReference<>(null);
                while (namespaceNodeAtomicReference.get() == null && System.currentTimeMillis() < expired) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            for (int i = 0; i < model.getChildCount(clusterNodeAtomicReference.get()); i++) {
                                NamespaceNode namespaceNode = (NamespaceNode) model.getChild(clusterNodeAtomicReference.get(), i);
                                if (StringUtils.equals(namespaceNode.getNamespace(), serviceProjectPath.getNamespace())) {
                                    namespaceNodeAtomicReference.set(namespaceNode);
                                    model.updateApplications(namespaceNode, true);
                                    break;
                                }
                            }
                        } catch (Exception ignore) {
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (Exception ignore) {
                    }
                }

                final AtomicReference<ApplicationNode> applicationNodeAtomicReference = new AtomicReference<>(null);
                while (applicationNodeAtomicReference.get() == null && System.currentTimeMillis() < expired) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            for (int i = 0; i < model.getChildCount(namespaceNodeAtomicReference.get()); i++) {
                                ApplicationNode applicationNode = (ApplicationNode) model.getChild(namespaceNodeAtomicReference.get(), i);
                                if (StringUtils.equals(applicationNode.getName(), serviceProjectPath.getApplicationName())) {
                                    applicationNodeAtomicReference.set(applicationNode);
                                    break;
                                }
                            }
                        } catch (Exception ignore) {
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (Exception ignore) {
                    }
                }

                final AtomicReference<ResourceNode> resourceNodeAtomicReference = new AtomicReference<>(null);
                while (resourceNodeAtomicReference.get() == null && System.currentTimeMillis() < expired) {
                    ApplicationManager.getApplication().runReadAction(() -> {
                        try {
                            OUTER:
                            for (int i = 0; i < model.getChildCount(applicationNodeAtomicReference.get()); i++) {
                                ResourceGroupNode resourceGroupNode = (ResourceGroupNode) model.getChild(applicationNodeAtomicReference.get(), i);
                                if (!StringUtils.equals(resourceGroupNode.getName(), "Workloads")) {
                                    continue;
                                }
                                for (int j = 0; j < model.getChildCount(resourceGroupNode); j++) {
                                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) model.getChild(resourceGroupNode, j);
                                    if (!StringUtils.equalsIgnoreCase(RESOURCE_TYPE_MAP.get(resourceTypeNode.getName()), serviceProjectPath.getServiceType())) {
                                        continue;
                                    }
                                    model.updateResources(resourceTypeNode, true);
                                    for (int k = 0; k < model.getChildCount(resourceTypeNode); k++) {
                                        ResourceNode resourceNode = (ResourceNode) model.getChild(resourceTypeNode, k);
                                        if (StringUtils.equals(resourceNode.resourceName(), serviceProjectPath.getServiceName())) {
                                            resourceNodeAtomicReference.set(resourceNode);
                                            break OUTER;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignore) {
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (Exception ignore) {
                    }
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    TreePath path = new TreePath(model.getPathToRoot(resourceNodeAtomicReference.get()));
                    this.setSelectionPath(path);
                });
            });
        } catch (Exception ignore) {
        }
    }

    @Override
    public void dispose() {
        messageBusConnection.dispose();
    }
}