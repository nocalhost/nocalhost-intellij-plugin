package dev.nocalhost.plugin.intellij.ui.tree;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.treeStructure.Tree;

import org.apache.commons.collections.CollectionUtils;
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
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeConfig;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeAllService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplicationOptions;
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
import dev.nocalhost.plugin.intellij.utils.Constants;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

import static dev.nocalhost.plugin.intellij.utils.Constants.DEFAULT_APPLICATION_NAME;
import static dev.nocalhost.plugin.intellij.utils.Constants.HELM_ANNOTATION_NAME;
import static dev.nocalhost.plugin.intellij.utils.Constants.NOCALHOST_ANNOTATION_NAME;

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
            ServiceManager.getService(NocalhostSettings.class);
    private final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);

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
                    KubeConfig kubeConfig = DataUtils.YAML.loadAs(
                            standaloneCluster.getRawKubeConfig(), KubeConfig.class);
                    clusterNodes.add(new ClusterNode(standaloneCluster.getRawKubeConfig(),
                            kubeConfig));
                }

                for (NocalhostAccount nocalhostAccount : nocalhostSettings.getNocalhostAccounts()) {
                    List<ServiceAccount> serviceAccounts = nocalhostApi.listServiceAccount(
                            nocalhostAccount.getServer(), nocalhostAccount.getJwt());
                    for (ServiceAccount serviceAccount : serviceAccounts) {
                        KubeConfig kubeConfig = DataUtils.YAML.loadAs(
                                serviceAccount.getKubeConfig(), KubeConfig.class);
                        clusterNodes.add(new ClusterNode(nocalhostAccount, serviceAccount,
                                serviceAccount.getKubeConfig(), kubeConfig));
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
                        nodeChanged(clusterNode);

                        updateNamespaces(clusterNode);
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
                List<NamespaceNode> namespaceNodes;
                if (clusterNode.getServiceAccount() != null) {
                    if (clusterNode.getServiceAccount().isPrivilege()) {
                        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(
                                clusterNode.getRawKubeConfig());
                        KubeResourceList namespaceList = kubectlCommand.getNamespaceList(
                                kubeConfigPath);
                        namespaceNodes = namespaceList.getItems().stream()
                                .map(e -> new NamespaceNode(e.getMetadata().getName()))
                                .collect(Collectors.toList());
                    } else {
                        if (clusterNode.getServiceAccount().getNamespaces() != null) {
                            namespaceNodes = clusterNode.getServiceAccount().getNamespaces()
                                    .stream()
                                    .map(e -> new NamespaceNode(e.getNamespace(), e.getSpaceName(), e.getSpaceId()))
                                    .collect(Collectors.toList());
                        } else {
                            namespaceNodes = Lists.newArrayList();
                        }
                    }
                } else {
                    Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(
                            clusterNode.getRawKubeConfig());
                    try {
                        KubeResourceList namespaceList = kubectlCommand.getNamespaceList(
                                kubeConfigPath);
                        namespaceNodes = namespaceList.getItems().stream()
                                .map(e -> new NamespaceNode(e.getMetadata().getName()))
                                .collect(Collectors.toList());
                    } catch (NocalhostExecuteCmdException nece) {
                        if (StringUtils.contains(nece.getMessage(), "namespaces is forbidden")
                                && StringUtils.isNotEmpty(clusterNode.getKubeConfig().getContexts().get(0).getContext().getNamespace())) {
                            namespaceNodes = Lists.newArrayList(new NamespaceNode(clusterNode.getKubeConfig().getContexts().get(0).getContext().getNamespace()));
                        } else {
                            throw nece;
                        }
                    }
                }

                final List<NamespaceNode> pendingNamespaces = namespaceNodes;
                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshNamespaceNodes(clusterNode, pendingNamespaces);
                });
            } catch (Exception e) {
                if (e instanceof NocalhostExecuteCmdException) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NocalhostNotifier.getInstance(project).notifyError(
                                "Loading namespaces error",
                                "Error occurs while loading namespaces",
                                e.getMessage());
                    });
                } else {
                    LOG.error("Loading namespaces error", e);
                }
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
                            .filter(e -> StringUtils.equals(e.getName(), namespaceNode.getName()))
                            .findFirst();
                    if (pendingNamespaceNodeOptional.isPresent()) {
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
                            pendingNamespaceNode.getName(),
                            namespaceNode.getName()
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
                NhctlListApplicationOptions opts = new NhctlListApplicationOptions(path,
                        namespaceNode.getName());
                List<ApplicationNode> applicationNodes = nhctlCommand.listApplication(opts)
                        .get(0)
                        .getApplication()
                        .stream()
                        .map(e -> new ApplicationNode(e.getName()))
                        .collect(Collectors.toList());
                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshApplicationNodes(namespaceNode, applicationNodes);
                });
            } catch (Exception e) {
                if (e instanceof NocalhostExecuteCmdException) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        NocalhostNotifier.getInstance(project).notifyError(
                                "Loading applicatons error",
                                "Error occurs while loading applicatons",
                                e.getMessage());
                    });
                } else {
                    LOG.error("Loading applicatons error", e);
                }
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
            } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
                if (e instanceof NocalhostExecuteCmdException) {
                    NocalhostNotifier.getInstance(project).notifyError(
                            "Loading resources error",
                            "Error occurred while loading resources",
                            e.getMessage());
                } else {
                    LOG.error("Loading resources error", e);
                }
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
        String namespace = applicationNode.getNamespaceNode().getName();

        String kubeResourceType = resourceTypeNode.getName().toLowerCase()
                .replaceAll(" ", "");
        KubeResourceList kubeResourceList = kubectlCommand.getResourceList(kubeResourceType, null,
                kubeConfigPath, namespace);

        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath,
                namespace);
        NhctlDescribeAllService nhctlDescribeAllService = null;
        try {
            nhctlDescribeAllService = nhctlCommand.describe(applicationName, nhctlDescribeOptions,
                    NhctlDescribeAllService.class);
        } catch (NocalhostExecuteCmdException e) {
            if (StringUtils.contains(e.getMessage(), "Application not found")) {
                return null;
            }
            throw e;
        }

        List<NhctlDescribeService> nhctlDescribeServices = nhctlDescribeAllService.getSvcProfile();

        List<KubeResource> kubeResources = kubeResourceList.getItems();
        if (!StringUtils.equals(resourceTypeNode.getApplicationNode().getName(), DEFAULT_APPLICATION_NAME)) {
            kubeResources = kubeResourceList.getItems().stream()
                    .filter(e -> StringUtils.equals(e.getMetadata().getAnnotations().get(Constants.NOCALHOST_ANNOTATION_NAME), applicationName)
                            || StringUtils.equals(e.getMetadata().getAnnotations().get(Constants.HELM_ANNOTATION_NAME), applicationName))
                    .collect(Collectors.toList());
        }

        List<ResourceNode> resources = Lists.newArrayList();
        for (KubeResource kubeResource : kubeResources) {
            final Optional<NhctlDescribeService> nhctlDescribeService =
                    nhctlDescribeServices.isEmpty()
                            ? Optional.empty()
                            : nhctlDescribeServices.stream()
                            .filter(svc -> StringUtils.equals(svc.getRawConfig().getName(), kubeResource.getMetadata().getName()))
                            .findFirst();
            if (StringUtils.equalsIgnoreCase(kubeResource.getKind(), "Deployment")
                    && nhctlDescribeService.isPresent()) {
                NhctlDescribeService nhctlDescribe = nhctlDescribeService.get();
                resources.add(new ResourceNode(kubeResource, nhctlDescribe));
            } else if (StringUtils.equalsIgnoreCase(kubeResource.getKind(), "StatefulSet")) {
                String metadataName = kubeResource.getMetadata().getName();
                KubeResource statefulsetKubeResource = kubectlCommand.getResource("" +
                        "StatefulSet/" + metadataName, "", kubeConfigPath, namespace);
                if (nhctlDescribeService.isPresent()) {
                    resources.add(new ResourceNode(
                            statefulsetKubeResource,
                            nhctlDescribeService.get()
                    ));
                } else {
                    resources.add(new ResourceNode(statefulsetKubeResource));
                }
            } else {
                resources.add(new ResourceNode(kubeResource));
            }
        }

        return resources;
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
