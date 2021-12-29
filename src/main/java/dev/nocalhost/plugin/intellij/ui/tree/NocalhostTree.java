package dev.nocalhost.plugin.intellij.ui.tree;

import com.google.common.collect.ImmutableMap;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.messages.MessageBusConnection;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import dev.nocalhost.plugin.intellij.data.NocalhostContext;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeExpandNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdKindNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdRootNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;
import dev.nocalhost.plugin.intellij.utils.KubeResourceUtil;

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
            public void treeWillExpand(TreeExpansionEvent event) {
                var node = event.getPath().getLastPathComponent();
                if (node instanceof MutableTreeNode) {
                    var mutable = (MutableTreeNode) node;
                    if ( ! mutable.isLeaf()) {
                        model.insertLoadingNode(mutable);
                    }
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {

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
                if (node instanceof CrdRootNode) {
                    var crd = (CrdRootNode) node;
                    model.updateCrdRootNode(crd, false, () -> {});
                }
                if (node instanceof CrdKindNode) {
                    var kind = (CrdKindNode) node;
                    model.updateCrdKindNode(kind, false, () -> {});
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

    private String compress(String raw) {
        return raw.replaceAll("[\\s\\t\\n\\r]", "");
    }

    private void expendWorkloadNode() {
        try {
            var context = NocalhostContextManager.getInstance(project).getContext();
            if (context == null) {
                return;
            }

            final Object root = model.getRoot();
            final String rawKubeConfig = Files.readString(context.getKubeConfigPath(), StandardCharsets.UTF_8);

            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    for (int i = 0; i < model.getChildCount(root); i++) {
                        ClusterNode clusterNode = (ClusterNode) model.getChild(root, i);
                        if (StringUtils.equals(compress(clusterNode.getRawKubeConfig()), compress(rawKubeConfig))) {
                            model.updateNamespaces(clusterNode, true, () -> _locateNamespace(clusterNode, context));
                            break;
                        }
                    }
                } catch (Exception ignore) {
                }
            });
        } catch (Exception ignore) {
        }
    }

    private void _locateNamespace(@NotNull ClusterNode node, @NotNull NocalhostContext context) {
        for (int i = 0; i < model.getChildCount(node); i++) {
            var namespaceNode = (NamespaceNode) model.getChild(node, i);
            if (StringUtils.equals(namespaceNode.getNamespace(), context.getNamespace())) {
                model.updateApplications(namespaceNode, true, () -> _locateApplication(namespaceNode, context));
                break;
            }
        }
    }

    private void _locateApplication(@NotNull NamespaceNode node, @NotNull NocalhostContext context) {
        for (int i = 0; i < model.getChildCount(node); i++) {
            var applicationNode = (ApplicationNode) model.getChild(node, i);
            if (StringUtils.equals(applicationNode.getName(), context.getApplicationName())) {
                if (KubeResourceUtil.isCRD(context.getServiceType())) {
                    _locateCrdKind(applicationNode, context);
                } else {
                    _locateResource(applicationNode, context);
                }
                break;
            }
        }
    }

    private void _locateCrdKind(@NotNull ApplicationNode node, @NotNull NocalhostContext context) {
        for (int i = 0, j = model.getChildCount(node); i < j; i++) {
            var child = (MutableTreeNode) model.getChild(node, i);
            if (child instanceof CrdRootNode) {
                var crdRootNode = (CrdRootNode) child;
                model.updateCrdRootNode(crdRootNode, true, () -> {
                    var group = context.getServiceType().replaceFirst("^([a-zA-Z0-9]+)\\.([a-zA-Z0-9]+)\\.", "");
                    for (int a = 0, b = model.getChildCount(crdRootNode); a < b; a++) {
                        var crdGroupNode = (CrdGroupNode) model.getChild(crdRootNode, a);
                        if (StringUtils.equals(crdGroupNode.getName(), group)) {
                            for (int x = 0, y = model.getChildCount(crdGroupNode); x < y; x++) {
                                var kind = (CrdKindNode) model.getChild(crdGroupNode, x);
                                if (StringUtils.equals(kind.getResourceType(), context.getServiceType())) {
                                    model.updateCrdKindNode(kind, true, () -> _locateCrdResource(kind, context));
                                }
                            }
                        }
                    }
                });
                break;
            }
        }
    }

    private void _locateCrdResource(@NotNull CrdKindNode kind, @NotNull NocalhostContext context) {
        for (int i = 0, j = model.getChildCount(kind); i < j; i++) {
            var node = (ResourceNode) model.getChild(kind, i);
            if (StringUtils.equals(node.resourceName(), context.getServiceName())) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    var path = new TreePath(model.getPathToRoot(node));
                    this.scrollPathToVisible(path);
                    this.setSelectionPath(path);
                });
                break;
            }
        }
    }

    private void _locateResource(@NotNull ApplicationNode node, @NotNull NocalhostContext context) {
        for (int i = 0; i < model.getChildCount(node); i++) {
            var group = (ResourceGroupNode) model.getChild(node, i);
            if (StringUtils.equals(group.getName(), "Workloads")) {
                for (int j = 0; j < model.getChildCount(group); j++) {
                    var resourceTypeNode = (ResourceTypeNode) model.getChild(group, j);
                    if (StringUtils.equalsIgnoreCase(RESOURCE_TYPE_MAP.get(resourceTypeNode.getName()), context.getServiceType())) {
                        model.updateResources(resourceTypeNode, true, () -> {
                            for (int k = 0; k < model.getChildCount(resourceTypeNode); k++) {
                                ResourceNode resourceNode = (ResourceNode) model.getChild(resourceTypeNode, k);
                                if (StringUtils.equals(resourceNode.resourceName(), context.getServiceName())) {
                                    ApplicationManager.getApplication().invokeLater(() -> {
                                        var path = new TreePath(model.getPathToRoot(resourceNode));
                                        this.scrollPathToVisible(path);
                                        this.setSelectionPath(path);
                                    });
                                    break;
                                }
                            }
                        });
                        break;
                    }
                }
                break;
            }
        }
    }

    @Override
    public void dispose() {
        messageBusConnection.dispose();
    }
}