package dev.nocalhost.plugin.intellij.ui.tree;

import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.treeStructure.Tree;

import org.jetbrains.annotations.NotNull;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.swing.tree.TreePath;
import javax.swing.tree.MutableTreeNode;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.ServiceAccount;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.ClusterStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlCheckClusterOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlCrdKind;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplication;
import dev.nocalhost.plugin.intellij.data.kubeconfig.KubeConfig;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.nhctl.NhctlKubeConfigRemoveCommand;
import dev.nocalhost.plugin.intellij.nhctl.NhctlKubeConfigRenderCommand;
import dev.nocalhost.plugin.intellij.nhctl.NhctlCrdListCommand;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.settings.data.StandaloneCluster;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ClusterNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdKindNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.CrdRootNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.TokenUtil;

public class NocalhostTreeModel extends NocalhostTreeModelBase {
    private static final List<Pair<String, List<String>>> RESOURCE_GROUP_TYPE = List.of(
            Pair.create("Workloads", List.of(
                    "Deployments",
                    "DaemonSets",
                    "StatefulSets",
                    "Jobs",
                    "CronJobs",
                    "Pods"
            )),
            Pair.create("CustomResources", List.of()),
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

    private final NocalhostApi nocalhostApi = ApplicationManager.getApplication().getService(NocalhostApi.class);
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
    private final NocalhostSettings settings = ApplicationManager.getApplication().getService(NocalhostSettings.class);
    private final AtomicReference<Map<String, String>> previous = new AtomicReference<>(Maps.newHashMap());
    private final AtomicReference<List<ClusterNode>> clusters = new AtomicReference<>(Lists.newArrayList());


    private final Project project;
    private final Tree tree;

    public NocalhostTreeModel(Project project, Tree tree) {
        super(new NocalhostTreeNodeComparator());
        this.project = project;
        this.tree = tree;
        previous.set(settings.getKubeConfigMap());
    }

    public void update() {
        updateClusters();
    }

    private void updateClusters() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Map<String, String> map = Maps.newHashMap();
            List<ClusterNode> clusterNodes = Lists.newArrayList();

            for (StandaloneCluster standaloneCluster : settings.getStandaloneClusters()) {
                try {
                    KubeConfig kubeConfig = DataUtils.fromYaml(standaloneCluster.getRawKubeConfig(), KubeConfig.class);
                    clusterNodes.add(new ClusterNode(standaloneCluster.getRawKubeConfig(), kubeConfig));
                } catch (Exception ex) {
                    ErrorUtil.console(project, "Error occurred while loading standalone cluster.", ex);
                }
            }

            for (var na : settings.getNocalhostAccounts()) {
                if ( ! TokenUtil.isValid(na.getJwt())) {
                    continue;
                }

                try {
                    List<ServiceAccount> sas = nocalhostApi.listServiceAccount(na.getServer(), na.getJwt());
                    for (var sa : sas) {
                        KubeConfig kubeConfig = DataUtils.fromYaml(sa.getKubeConfig(), KubeConfig.class);
                        if (sa.isVirtualCluster() && sa.isClusterIP()) {
                            var path = KubeConfigUtil.kubeConfigPath(sa.getKubeConfig());
                            if (NhctlKubeConfigRenderCommand.isAlive(path.toString())) {
                                var conf = NhctlKubeConfigRenderCommand.getConf(path.toString());
                                if (StringUtils.isNotEmpty(conf)) {
                                    clusterNodes.add(new ClusterNode(na, sa, conf, DataUtils.fromYaml(conf, KubeConfig.class)));
                                }
                            } else {
                                var cmd = new NhctlKubeConfigRenderCommand(project);
                                cmd.setPort(sa.getVirtualCluster().getPort());
                                cmd.setAddress(sa.getVirtualCluster().getAddress());
                                cmd.setContext(sa.getVirtualCluster().getContext());
                                cmd.setNamespace(sa.getVirtualCluster().getNamespace());
                                cmd.setKubeConfig(path);

                                if (project.isDisposed()) {
                                    return;
                                }

                                var conf = cmd.execute();
                                clusterNodes.add(new ClusterNode(na, sa, conf, DataUtils.fromYaml(conf, KubeConfig.class)));
                            }
                        } else {
                            clusterNodes.add(new ClusterNode(na, sa, sa.getKubeConfig(), kubeConfig));
                        }

                        var key = computeKey(na, sa);
                        map.put(key, sa.getKubeConfig());
                        notifyToNhctl(key, sa.getKubeConfig());
                    }
                } catch (Exception ex) {
                    var summary = String.format(
                            "Error occurred while loading cluster from server: %s, account: %s",
                            na.getServer(),
                            na.getUsername()
                    );
                    ErrorUtil.console(project, summary, ex);
                }
            }

            previous.set(map);
            clusters.set(clusterNodes);
            settings.setKubeConfigMap(map);

            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                var nodes = clusters.get();
                for (ClusterNode clusterNode : nodes) {
                    try {
                        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(clusterNode.getRawKubeConfig());
                        NhctlCheckClusterOptions opts = new NhctlCheckClusterOptions(kubeConfigPath);
                        ClusterStatus clusterStatus = nhctlCommand.checkCluster(opts);
                        clusterNode.setActive(clusterStatus.getCode() == 200);
                        clusterNode.setInfo(clusterStatus.getInfo());
                    } catch (Exception ignore) {
                    }
                }
                ApplicationManager.getApplication().invokeLater(() -> refreshClusterNodes((MutableTreeNode) root, nodes));
            });
        });
    }

    private @NotNull String computeKey(@NotNull NocalhostAccount na, @NotNull ServiceAccount sa) {
        return na.getServer() + ":" + na.getUsername() + ":" + sa.getClusterId();
    }

    private void notifyToNhctl(@NotNull String key, @NotNull String value) {
        try {
            var map = previous.get();
            if (map.containsKey(key) && !StringUtils.equals(map.get(key), value)) {
                var path = KubeConfigUtil.kubeConfigPath(map.get(key));
                NhctlKubeConfigRenderCommand.destroy(path.toString());
                var cmd = new NhctlKubeConfigRemoveCommand(project);
                cmd.setKubeConfig(path);
                cmd.execute();
            }
        } catch (Exception ex) {
            ErrorUtil.dealWith(project, "Notify nhctl error", "Error occurred while notify nhctl.", ex);
        }
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
        updateNamespaces(clusterNode, false, () -> {});
    }

    void updateNamespaces(ClusterNode clusterNode, boolean force, @NotNull Runnable next) {
        if (!force && !tree.isExpanded(new TreePath(getPathToRoot(clusterNode)))) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<NamespaceNode> namespaceNodes = Lists.newArrayList();
                Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(clusterNode.getRawKubeConfig());
                NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(kubeConfigPath, "");
                if (clusterNode.getServiceAccount() != null) {
                    if (clusterNode.getServiceAccount().isPrivilege()) {
                        List<NhctlGetResource> namespaces = nhctlCommand.getResources("namespaces", nhctlGetOptions);
                        Map<String, ServiceAccount.NamespacePack> map = Maps.newHashMap();
                        if (clusterNode.getServiceAccount().getNamespacePacks() != null) {
                            map.putAll(
                                    clusterNode.getServiceAccount()
                                               .getNamespacePacks()
                                               .stream()
                                               .collect(Collectors.toMap(ServiceAccount.NamespacePack::getNamespace, x -> x))
                            );
                        }

                        namespaceNodes.addAll(namespaces.stream().map(x -> {
                            var ns = x.getKubeResource().getMetadata().getName();
                            if (map.containsKey(ns)) {
                                return new NamespaceNode(map.get(ns));
                            }
                            return new NamespaceNode(ns);
                        }).collect(Collectors.toList()));
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
                    if (nhctlGetResources != null) {
                        namespaceNodes = nhctlGetResources.stream()
                                .map(e -> new NamespaceNode(e.getKubeResource().getMetadata().getName()))
                                .collect(Collectors.toList());
                    }
                }

                final List<NamespaceNode> pendingNamespaces = namespaceNodes;
                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshNamespaceNodes(clusterNode, pendingNamespaces);
                    next.run();
                });
            } catch (Exception ex) {
                ErrorUtil.console(project, "Error occurred while loading namespaces.", ex);
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
        updateApplications(namespaceNode, false, () -> {});
    }

    void updateApplications(NamespaceNode namespaceNode, boolean force, @NotNull Runnable next) {
        if (!force && !tree.isExpanded(new TreePath(getPathToRoot(namespaceNode)))) {
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
                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshApplicationNodes(namespaceNode, finalApplicationNodes);
                    next.run();
                });
            } catch (Exception ex) {
                ErrorUtil.console(project, "Error occurred while loading applications.", ex);
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
            if (StringUtils.equals(group, "CustomResources")) {
                applicationNode.add(new CrdRootNode(group));
                continue;
            }
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
                var child = getChild(applicationNode, i);
                if (child instanceof CrdRootNode) {
                    var crd = (CrdRootNode) child;
                    updateCrdRootNode(crd, false, () -> {});
                    continue;
                }
                ResourceGroupNode resourceGroupNode = (ResourceGroupNode) child;
                for (int j = 0; j < getChildCount(resourceGroupNode); j++) {
                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) getChild(
                            resourceGroupNode, j);
                    updateResources(resourceTypeNode);
                }
            }
        });
    }

    void updateCrdRootNode(CrdRootNode node, boolean force, @NotNull Runnable next) {
        if (force || tree.isExpanded(new TreePath(getPathToRoot(node)))) {
            var config = node.getNamespaceNode().getClusterNode().getRawKubeConfig();
            var parser = TypeToken.getParameterized(List.class, NhctlCrdKind.class).getType();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    var cmd = new NhctlCrdListCommand(project);
                    cmd.setKubeConfig(KubeConfigUtil.kubeConfigPath(config));
                    List<NhctlCrdKind> results = DataUtils.GSON.fromJson(cmd.execute(), parser);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        refreshCrdRootNode(node, results);
                        next.run();
                    });
                } catch (Exception ex) {
                    ErrorUtil.dealWith(project, "Failed to refresh CRD kinds",
                            "Error occurred while loading CRD kinds", ex);
                }
            });
        }
    }

    void refreshCrdRootNode(CrdRootNode node, List<NhctlCrdKind> results) {
        synchronized (node) {
            removeLoadingNode(node);

            if (results == null) {
                return;
            }

            var hash = results
                    .stream()
                    .filter(x -> x.getInfo().isNamespaced())
                    .collect(Collectors.toMap(
                            x -> x.getInfo().getGroup(),
                            x -> Lists.newArrayList(x.getInfo()),
                            (a, b) -> {
                                a.addAll(b);
                                return a;
                            }
                    ));
            for (int i = getChildCount(node) - 1; i >= 0; i--) {
                var child = (CrdGroupNode) getChild(node, i);
                if (hash.containsKey(child.getName())) {
                    for (int j = getChildCount(child) - 1; j >= 0; j--) {
                        updateCrdKindNode((CrdKindNode) getChild(child, j), false, () -> {});
                    }
                } else {
                    removeNode(child);
                }
            }
            hash.forEach((k, v) -> {
                var exist = false;
                for (int i = getChildCount(node) - 1; i >= 0; i--) {
                    if (getChild(node, i) instanceof CrdGroupNode) {
                        var child = (CrdGroupNode) getChild(node, i);
                        if (StringUtils.equals(child.getName(), k)) {
                            exist = true;
                            break;
                        }
                    }
                }
                if ( ! exist) {
                    var group = new CrdGroupNode(k);
                    v.forEach(x -> group.add(new CrdKindNode(x)));
                    insertNode(group, node);
                }
            });
        }
    }

    void updateCrdKindNode(CrdKindNode node, boolean force, @NotNull Runnable next) {
        if (!force && !tree.isExpanded(new TreePath(getPathToRoot(node)))) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var appNode = node.getApplicationNode();
                var path = KubeConfigUtil.kubeConfigPath(appNode.getClusterNode().getRawKubeConfig());
                var options = new NhctlGetOptions(path, appNode.getNamespaceNode().getNamespace());
                options.setApplication(appNode.getName());
                var results = nhctlCommand.getResources(node.getResourceType(), options);

                ApplicationManager.getApplication().invokeLater(() -> {
                    refreshCrdKindNode(node, results);
                    next.run();
                });
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Failed to refresh CRD resources",
                        "Error occurs while loading CRD resources", e);
            }
        });
    }

    void refreshCrdKindNode(CrdKindNode node, List<NhctlGetResource> results) {
        synchronized (node) {
            removeLoadingNode(node);

            if (results == null) {
                return;
            }

            var hash = results
                    .stream()
                    .collect(Collectors.toMap(
                            x -> x.getKubeResource().getMetadata().getName(),
                            x -> x
                    ));
            for (int i = getChildCount(node) - 1; i >= 0; i--) {
                var child = (ResourceNode) getChild(node, i);
                if (hash.containsKey(child.resourceName())) {
                    var resource = hash.get(child.resourceName());
                    var describe = resource.getNhctlDescribeService();
                    if (describe == null) {
                        describe = new NhctlDescribeService();
                    }
                    var other = new ResourceNode(resource.getKubeResource(), describe, resource.getVpn(), true);
                    child.updateFrom(other);
                    nodeChanged(child);
                } else {
                    removeNode(child);
                }
            }
            hash.forEach((k, v) -> {
                var exist = false;
                for (int i = getChildCount(node) - 1; i >= 0; i--) {
                    if (getChild(node, i) instanceof ResourceNode) {
                        var child = (ResourceNode) getChild(node, i);
                        if (StringUtils.equals(child.resourceName(), k)) {
                            exist = true;
                            break;
                        }
                    }
                }
                if ( ! exist) {
                    var describe = v.getNhctlDescribeService();
                    if (describe == null) {
                        describe = new NhctlDescribeService();
                    }
                    var insert = new ResourceNode(v.getKubeResource(), describe,  v.getVpn(),true);
                    insertNode(insert, node);
                }
            });
        }
    }

    void updateResources(ResourceTypeNode resourceTypeNode) {
        updateResources(resourceTypeNode, false, () -> {});
    }

    void updateResources(ResourceTypeNode resourceTypeNode, boolean force, @NotNull Runnable next) {
        if (!force && !tree.isExpanded(new TreePath(getPathToRoot(resourceTypeNode)))) {
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
                    next.run();
                });
            } catch (Exception ex) {
                ErrorUtil.console(project, "Error occurred while loading resources.", ex);
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
                    return new ResourceNode(e.getKubeResource(), nhctlDescribeService, e.getVpn());
                })
                .collect(Collectors.toList());
    }

    void insertLoadingNode(MutableTreeNode parent) {
        if (getChildCount(parent) == 0) {
            insertNode(new LoadingNode(), parent);
        }
    }

    public void removeLoadingNode(MutableTreeNode parent) {
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
