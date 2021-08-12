package dev.nocalhost.plugin.intellij.ui.tree;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.treeStructure.Tree;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.ClusterStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlCheckClusterOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplication;
import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeConfig;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.settings.data.StandaloneCluster;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.TokenUtil;

public class NocalhostTreeModel extends NocalhostTreeModelBase {
    private static final Logger LOG = Logger.getInstance(NocalhostTreeModel.class);

    private static final List<Pair<String, List<String>>> RESOURCE_GROUP_TYPE = List.of(
            Pair.create("Workloads", List.of(
                    "Deployments",
                    "DaemonSets",
                    "StatefulSets",
                    "Jobs",
                    "CronJobs",
                    "Pods"
            )),
            Pair.create("Network", List.of(
                    "Services",
                    "Endpoints",
                    "Ingresses",
                    "Network Policies"
            )),
            Pair.create("Configuration", List.of(
                    "ConfigMaps",
                    "Secrets",
                    "Resource Quotas",
                    "HPA",
                    "Pod Disruption Budgets"
            )),
            Pair.create("Storage", List.of(
                    "Persistent Volumes",
                    "Persistent Volume Claims",
                    "Storage Classes"
            ))
    );

    private final NocalhostSettings nocalhostSettings =
            ApplicationManager.getApplication().getService(NocalhostSettings.class);
    private final NocalhostApi nocalhostApi = ApplicationManager.getApplication().getService(NocalhostApi.class);
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

    private final Project project;
    private final Tree tree;

    public NocalhostTreeModel(Project project, Tree tree) {
        super(new NocalhostTreeNodeComparator());
        this.project = project;
        this.tree = tree;
    }

    public void update() {
        updateClusters();
    }

    private void updateClusters() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<ClusterNode> clusterNodes = Lists.newArrayList();

                for (StandaloneCluster standaloneCluster : nocalhostSettings.getStandaloneClusters()) {
                    KubeConfig kubeConfig = DataUtils.fromYaml(
                            standaloneCluster.getRawKubeConfig(), KubeConfig.class);
                    clusterNodes.add(new ClusterNode(standaloneCluster.getRawKubeConfig(),
                            kubeConfig));
                }

                for (NocalhostAccount nocalhostAccount : nocalhostSettings.getNocalhostAccounts()) {
                    if (!TokenUtil.isValid(nocalhostAccount.getJwt())) {
                        continue;
                    }
                    List<ServiceAccount> serviceAccounts = nocalhostApi.listServiceAccount(
                            nocalhostAccount.getServer(), nocalhostAccount.getJwt());
                    for (ServiceAccount serviceAccount : serviceAccounts) {
                        KubeConfig kubeConfig = DataUtils.fromYaml(
                                serviceAccount.getKubeConfig(), KubeConfig.class);
                        clusterNodes.add(new ClusterNode(nocalhostAccount, serviceAccount,
                                serviceAccount.getKubeConfig(), kubeConfig));
                    }
                }

                for (ClusterNode clusterNode : clusterNodes) {
                    try {
                        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(clusterNode.getRawKubeConfig());
                        NhctlCheckClusterOptions opts = new NhctlCheckClusterOptions(kubeConfigPath);
                        ClusterStatus clusterStatus = nhctlCommand.checkCluster(opts);
                        clusterNode.setActive(clusterStatus.getCode() == 200);
                        clusterNode.setInfo(clusterStatus.getInfo());
                    } catch (Exception ignored) {
                    }
                }

                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshClusterNodes((MutableTreeNode) root, clusterNodes);
                });

            } catch (Exception e) {
                if (e instanceof NocalhostApiException) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NocalhostNotifier.getInstance(project).notifyError(
                                "Loading clusters error",
                                "Error occurs while loading clusters",
                                e.getMessage());
                    });
                } else {
                    LOG.error("Loading clusters error", e);
                }
            }
        });
    }

    private void refreshClusterNodes(MutableTreeNode parent,
                                     List<ClusterNode> pendingClusterNodes) {
        synchronized (parent) {
            removeLoadingNode(parent);

            if (getChildCount(parent) > 0) {
                for (int i = getChildCount(parent) - 1; i >= 0; i--) {
                    ClusterNode clusterNode = (ClusterNode) getChild(parent, i);
                    Optional<ClusterNode> pendingClusterNodeOptional = pendingClusterNodes.stream()
                            .filter(e -> clusterNodeEquals(e, clusterNode))
                            .findFirst();
                    if (pendingClusterNodeOptional.isPresent()) {
                        clusterNode.updateFrom(pendingClusterNodeOptional.get());
                        if (clusterNode.isActive()) {
                            nodeChanged(clusterNode);
                            updateNamespaces(clusterNode);
                        } else {
                            nodeChanged(clusterNode);
                            removeAllChildren(clusterNode);
                        }
                    } else {
                        removeNode(clusterNode);
                    }
                }
            }

            for (ClusterNode pendingClusterNode : pendingClusterNodes) {
                boolean existed = false;
                for (int i = 0; i < getChildCount(parent); i++) {
                    ClusterNode clusterNode = (ClusterNode) getChild(parent, i);
                    if (clusterNodeEquals(pendingClusterNode, clusterNode)) {
                        existed = true;
                        break;
                    }
                }
                if (!existed) {
                    insertNode(pendingClusterNode, parent);
                }
            }
        }
    }

    void updateNamespaces(ClusterNode clusterNode) {
        if (!tree.isExpanded(new TreePath(getPathToRoot(clusterNode)))) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<NamespaceNode> namespaceNodes = Lists.newArrayList();
                Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(clusterNode.getRawKubeConfig());
                NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(kubeConfigPath, "");
                if (clusterNode.getServiceAccount() != null) {
                    if (clusterNode.getServiceAccount().isPrivilege()) {
                        List<String> namespaces = nhctlCommand.getResources("namespaces", nhctlGetOptions)
                                .stream()
                                .map(e -> e.getKubeResource().getMetadata().getName())
                                .collect(Collectors.toList());
                        List<ServiceAccount.NamespacePack> namespacePacks = clusterNode.getServiceAccount().getNamespacePacks();
                        List<String> namespacesInsideNamespacePacks = namespacePacks.stream()
                                .map(ServiceAccount.NamespacePack::getNamespace)
                                .collect(Collectors.toList());
                        namespaceNodes = Lists.newArrayList();
                        namespaceNodes.addAll(namespaces.stream()
                                .filter(e -> !namespacesInsideNamespacePacks.contains(e))
                                .map(NamespaceNode::new)
                                .collect(Collectors.toList()));
                        namespaceNodes.addAll(namespacePacks.stream()
                                .map(NamespaceNode::new)
                                .collect(Collectors.toList()));
                    } else {
                        if (clusterNode.getServiceAccount().getNamespacePacks() != null) {
                            namespaceNodes = clusterNode.getServiceAccount().getNamespacePacks()
                                    .stream()
                                    .map(NamespaceNode::new)
                                    .collect(Collectors.toList());
                        }
                    }
                } else {
                    List<NhctlGetResource> nhctlGetResources = nhctlCommand.getResources("namespaces", nhctlGetOptions);
                    if (nhctlGetResources == null || nhctlGetResources.isEmpty()) {
                        namespaceNodes = Lists.newArrayList(new NamespaceNode(clusterNode.getKubeConfig().getContexts().get(0).getContext().getNamespace()));
                    } else {
                        namespaceNodes = nhctlGetResources.stream()
                                .map(e -> new NamespaceNode(e.getKubeResource().getMetadata().getName()))
                                .collect(Collectors.toList());
                    }
                }

                final List<NamespaceNode> pendingNamespaces = namespaceNodes;
                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshNamespaceNodes(clusterNode, pendingNamespaces);
                });
            } catch (Exception e) {
                LOG.error("Loading namespaces error", e);
            }
        });
    }

    private void refreshNamespaceNodes(ClusterNode parent,
                                       List<NamespaceNode> pendingNamespaceNodes) {
        synchronized (parent) {
            removeLoadingNode(parent);

            if (getChildCount(parent) > 0) {
                for (int i = getChildCount(parent) - 1; i >= 0; i--) {
                    NamespaceNode namespaceNode = (NamespaceNode) getChild(parent, i);
                    Optional<NamespaceNode> pendingNamespaceNodeOptional = pendingNamespaceNodes
                            .stream()
                            .filter(e -> StringUtils.equals(e.getNamespace(), namespaceNode.getNamespace()))
                            .findFirst();
                    if (pendingNamespaceNodeOptional.isPresent()) {
                        namespaceNode.updateFrom(pendingNamespaceNodeOptional.get());
                        nodeChanged(namespaceNode);
                        updateApplications(namespaceNode);
                    } else {
                        removeNode(namespaceNode);
                    }
                }
            }

            for (NamespaceNode pendingNamespaceNode : pendingNamespaceNodes) {
                boolean existed = false;
                for (int i = 0; i < getChildCount(parent); i++) {
                    NamespaceNode namespaceNode = (NamespaceNode) getChild(parent, i);
                    if (StringUtils.equals(
                            pendingNamespaceNode.getNamespace(),
                            namespaceNode.getNamespace()
                    )) {
                        existed = true;
                        break;
                    }
                }
                if (!existed) {
                    insertNode(pendingNamespaceNode, parent);
                }
            }
        }
    }

    void updateApplications(NamespaceNode namespaceNode) {
        if (!tree.isExpanded(new TreePath(getPathToRoot(namespaceNode)))) {
            return;
        }
        ClusterNode clusterNode = namespaceNode.getClusterNode();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path path = KubeConfigUtil.kubeConfigPath(
                        clusterNode.getRawKubeConfig());
                NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(path, namespaceNode.getNamespace());
                List<ApplicationNode> applicationNodes = Lists.newArrayList();
                List<NhctlListApplication> nhctlListApplications = nhctlCommand.getApplications(nhctlGetOptions);
                if (nhctlListApplications != null
                        && !nhctlListApplications.isEmpty()
                        && nhctlListApplications.get(0) != null
                        && nhctlListApplications.get(0).getApplication() != null) {
                    applicationNodes = nhctlListApplications.get(0).getApplication()
                            .stream()
                            .map(ApplicationNode::new)
                            .collect(Collectors.toList());
                }
                final List<ApplicationNode> finalApplicationNodes = applicationNodes;
                ApplicationManager.getApplication().invokeLater(() ->
                        refreshApplicationNodes(namespaceNode, finalApplicationNodes));
            } catch (Exception e) {
                LOG.error("Loading applicatons error", e);
            }
        });
    }

    private void refreshApplicationNodes(NamespaceNode parent,
                                         List<ApplicationNode> pendingApplicationNodes) {
        synchronized (parent) {
            removeLoadingNode(parent);

            if (getChildCount(parent) > 0) {
                for (int i = getChildCount(parent) - 1; i >= 0; i--) {
                    ApplicationNode applicationNode = (ApplicationNode) getChild(parent, i);
                    Optional<ApplicationNode> applicationNodeOptional = pendingApplicationNodes
                            .stream()
                            .filter(e -> StringUtils.equals(e.getName(), applicationNode.getName()))
                            .findFirst();
                    if (applicationNodeOptional.isPresent()) {
                        updateApplications(applicationNode);
                    } else {
                        removeNode(applicationNode);
                    }
                }
            }
            for (ApplicationNode pendingApplicationNode : pendingApplicationNodes) {
                boolean existed = false;
                for (int i = 0; i < getChildCount(parent); i++) {
                    ApplicationNode applicationNode = (ApplicationNode) getChild(parent, i);
                    if (StringUtils.equals(
                            pendingApplicationNode.getName(),
                            applicationNode.getName()
                    )) {
                        existed = true;
                        break;
                    }
                }
                if (!existed) {
                    insertNode(pendingApplicationNode, parent);
                    createResourceGroupTypeStructure(pendingApplicationNode);
                }
            }
        }
    }

    private void createResourceGroupTypeStructure(ApplicationNode applicationNode) {
        for (Pair<String, List<String>> pair : RESOURCE_GROUP_TYPE) {
            String group = pair.first;
            ResourceGroupNode resourceGroupNode = new ResourceGroupNode(group);
            applicationNode.add(resourceGroupNode);
            for (String type : pair.second) {
                ResourceTypeNode resourceTypeNode = new ResourceTypeNode(type);
                resourceGroupNode.add(resourceTypeNode);
            }
        }
        nodeStructureChanged(applicationNode);
    }

    private void updateApplications(ApplicationNode applicationNode) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            for (int i = 0; i < getChildCount(applicationNode); i++) {
                ResourceGroupNode resourceGroupNode = (ResourceGroupNode) getChild(applicationNode, i);
                for (int j = 0; j < getChildCount(resourceGroupNode); j++) {
                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) getChild(
                            resourceGroupNode, j);
                    updateResources(resourceTypeNode);
                }
            }
        });
    }

    void updateResources(ResourceTypeNode resourceTypeNode) {
        if (!tree.isExpanded(new TreePath(getPathToRoot(resourceTypeNode)))) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<ResourceNode> resources = fetchResourcesData(resourceTypeNode);
                if (resources == null) {
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshResourceNodes(resourceTypeNode, resources);
                });
            } catch (Exception e) {
                LOG.error("Loading resources error", e);
            }
        });
    }

    private void refreshResourceNodes(ResourceTypeNode parent,
                                      List<ResourceNode> pendingResourceNodes) {
        synchronized (parent) {
            removeLoadingNode(parent);

            if (getChildCount(parent) > 0) {
                for (int i = getChildCount(parent) - 1; i >= 0; i--) {
                    ResourceNode resourceNode = (ResourceNode) getChild(parent, i);
                    Optional<ResourceNode> pendingResourceNodeOptional = pendingResourceNodes
                            .stream()
                            .filter(e -> StringUtils.equals(
                                    resourceNode.resourceName(),
                                    e.resourceName()
                            ))
                            .findFirst();
                    if (pendingResourceNodeOptional.isPresent()) {
                        ResourceNode pendingResourceNode = pendingResourceNodeOptional.get();
                        resourceNode.updateFrom(pendingResourceNode);
                        nodeChanged(resourceNode);
                    } else {
                        removeNode(resourceNode);
                    }
                }
            }
            for (ResourceNode pendingResourceNode : pendingResourceNodes) {
                boolean existed = false;
                for (int i = 0; i < getChildCount(parent); i++) {
                    if (getChild(parent, i) instanceof ResourceNode) {
                        ResourceNode resourceNode = (ResourceNode) getChild(parent, i);
                        if (StringUtils.equals(
                                resourceNode.resourceName(),
                                pendingResourceNode.resourceName()
                        )) {
                            existed = true;
                            break;
                        }
                    }
                }
                if (!existed) {
                    insertNode(pendingResourceNode, parent);
                }
            }
        }
    }

    private List<ResourceNode> fetchResourcesData(ResourceTypeNode resourceTypeNode)
            throws InterruptedException, NocalhostExecuteCmdException, IOException {
        ApplicationNode applicationNode = resourceTypeNode.getApplicationNode();
        String applicationName = applicationNode.getName();
        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(applicationNode.getClusterNode().getRawKubeConfig());
        String namespace = applicationNode.getNamespaceNode().getNamespace();

        String kubeResourceType = resourceTypeNode.getName().replaceAll(" ", "");

        NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(kubeConfigPath, namespace);
        nhctlGetOptions.setApplication(applicationName);
        List<NhctlGetResource> nhctlGetResources = nhctlCommand.getResources(kubeResourceType,
                nhctlGetOptions);
        if (nhctlGetResources == null) {
            return Lists.newArrayList();
        }
        return nhctlGetResources.stream()
                .map(e -> {
                    NhctlDescribeService nhctlDescribeService = e.getNhctlDescribeService();
                    if (nhctlDescribeService == null) {
                        nhctlDescribeService = new NhctlDescribeService();
                    }
                    return new ResourceNode(e.getKubeResource(), nhctlDescribeService);
                })
                .collect(Collectors.toList());
    }

    void insertLoadingNode(MutableTreeNode parent) {
        if (getChildCount(parent) == 0) {
            insertNode(new LoadingNode(), parent);
        }
    }

    private void removeLoadingNode(MutableTreeNode parent) {
        if (getChildCount(parent) > 0 && getChild(parent, 0) instanceof LoadingNode) {
            removeNode((LoadingNode) getChild(parent, 0));
        }
    }

    private boolean clusterNodeEquals(ClusterNode a, ClusterNode b) {
        if (a.getNocalhostAccount() != null && b.getNocalhostAccount() != null) {
            return StringUtils.equals(a.getNocalhostAccount().getServer(), b.getNocalhostAccount().getServer())
                    && StringUtils.equals(a.getNocalhostAccount().getUsername(), b.getNocalhostAccount().getUsername())
                    && a.getServiceAccount().getClusterId() == b.getServiceAccount().getClusterId();
        }
        if (a.getNocalhostAccount() == null && b.getNocalhostAccount() == null) {
            return StringUtils.equals(a.getRawKubeConfig(), b.getRawKubeConfig());
        }
        return false;
    }
}
