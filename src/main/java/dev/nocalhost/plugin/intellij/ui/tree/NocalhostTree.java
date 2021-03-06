package dev.nocalhost.plugin.intellij.ui.tree;

import com.google.common.collect.Lists;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.treeStructure.Tree;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.AliveDeployment;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeAllService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.helpers.UserDataKeyHelper;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.ui.tree.node.AccountNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class NocalhostTree extends Tree {
    private static final Logger LOG = Logger.getInstance(NocalhostTree.class);

    private final Project project;
    private final DefaultTreeModel model;
    private final DefaultMutableTreeNode root;
    private final AtomicBoolean updatingDecSpaces = new AtomicBoolean(false);

    public NocalhostTree(Project project) {
        super(new DefaultTreeModel(new DefaultMutableTreeNode(new Object())));

        this.project = project;
        model = (DefaultTreeModel) this.getModel();
        root = (DefaultMutableTreeNode) model.getRoot();

        init();

        model.insertNodeInto(new LoadingNode(), root, 0);
        model.reload();
    }

    private void init() {
        this.expandPath(new TreePath(root.getPath()));
        this.setRootVisible(false);
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setCellRenderer(new TreeNodeRenderer());
        this.addMouseListener(new TreeMouseListener(this, project));
        this.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object node = event.getPath().getLastPathComponent();
                if (node instanceof ResourceTypeNode) {
                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) node;
                    if (!resourceTypeNode.isLoaded() && model.getChildCount(resourceTypeNode) == 0) {
                        model.insertNodeInto(new LoadingNode(), resourceTypeNode, model.getChildCount(resourceTypeNode));
                    }
                    return;
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
                if (node instanceof DevSpaceNode) {
                    DevSpaceNode devSpaceNode = (DevSpaceNode) node;
                    devSpaceNode.setExpanded(true);
                    return;
                }
                if (node instanceof ResourceGroupNode) {
                    ResourceGroupNode resourceGroupNode = (ResourceGroupNode) node;
                    resourceGroupNode.setExpanded(true);
                    return;
                }
                if (node instanceof ResourceTypeNode) {
                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) node;
                    resourceTypeNode.setExpanded(true);

                    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fetching nocalhost data", false) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            if (!resourceTypeNode.isLoaded()) {
                                try {
                                    loadKubeResources(resourceTypeNode);
                                    resourceTypeNode.setLoaded(true);
                                } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                                    LOG.error("error occurred while loading kube resources", e);
                                    if (StringUtils.contains(e.getMessage(), "No such file or directory")) {
                                        NocalhostNotifier.getInstance(project).notifyKubectlNotFound();
                                    } else {
                                        NocalhostNotifier.getInstance(project).notifyError("Nocalhost fetch data error", "Error occurred while fetching data", e.getMessage());
                                    }
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                Object node = event.getPath().getLastPathComponent();
                if (node instanceof DevSpaceNode) {
                    DevSpaceNode devSpaceNode = (DevSpaceNode) node;
                    devSpaceNode.setExpanded(false);
                    return;
                }
                if (node instanceof ResourceGroupNode) {
                    ResourceGroupNode resourceGroupNode = (ResourceGroupNode) node;
                    resourceGroupNode.setExpanded(false);
                    return;
                }
                if (node instanceof ResourceTypeNode) {
                    ResourceTypeNode resourceTypeNode = (ResourceTypeNode) node;
                    resourceTypeNode.setExpanded(false);
                    return;
                }
            }
        });
    }

    public void updateDevSpaces() {
        if (!updatingDecSpaces.compareAndSet(false, true)) {
            return;
        }
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Fetching nocalhost data", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
                try {
                    List<DevSpace> devSpaces = nocalhostApi.listDevSpace();
                    updateDevSpaces(devSpaces);
                } catch (IOException | InterruptedException | NocalhostExecuteCmdException | NocalhostApiException e) {
                    LOG.error(e);
                    if (StringUtils.contains(e.getMessage(), "No such file or directory")) {
                        NocalhostNotifier.getInstance(project).notifyNhctlNotFound();
                    } else {
                        NocalhostNotifier.getInstance(project).notifyError("Nocalhost fetch data error", "Error occurred while fetching data", e.getMessage());
                    }
                } finally {
                    updatingDecSpaces.set(false);
                }
            }
        });
    }

    private void updateDevSpaces(List<DevSpace> devSpaces) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        boolean needReload = false;

        if (model.getChild(root, 0) instanceof LoadingNode) {
            model.removeNodeFromParent((MutableTreeNode) model.getChild(root, 0));
            final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
            model.insertNodeInto(new AccountNode(nocalhostSettings.getUserInfo()), root, 0);
            needReload = true;
        }

        // remove non-existed nodes
        for (int i = model.getChildCount(root) - 1; i >= 1; i--) {
            DevSpaceNode devSpaceNode = (DevSpaceNode) model.getChild(root, i);
            boolean toBeRemoved = true;
            for (DevSpace devSpace : devSpaces) {
                if (devSpaceNode.getDevSpace().getId() == devSpace.getId()
                        && devSpaceNode.getDevSpace().getDevSpaceId() == devSpace.getDevSpaceId()) {
                    toBeRemoved = false;
                    break;
                }
            }
            if (toBeRemoved) {
                model.removeNodeFromParent(devSpaceNode);
                needReload = true;
            }
        }

        // add or modify existed nodes
        for (int i = 0; i < devSpaces.size(); i++) {
            DevSpace devSpace = devSpaces.get(i);

            if (model.getChildCount(root) <= i + 1) {
                model.insertNodeInto(createDevSpaceNode(devSpace), root, model.getChildCount(root));
                needReload = true;
                continue;
            }

            DevSpaceNode devSpaceNode = (DevSpaceNode) model.getChild(root, i + 1);

            if (devSpaceNode.getDevSpace().getId() != devSpace.getId()
                    || devSpaceNode.getDevSpace().getDevSpaceId() != devSpace.getDevSpaceId()) {
                model.insertNodeInto(createDevSpaceNode(devSpace), root, i + 1);
                continue;
            }

            if (devSpaceNode.getDevSpace().getId() == devSpace.getId()
                    || devSpaceNode.getDevSpace().getDevSpaceId() == devSpace.getDevSpaceId()) {
                if (NhctlHelper.isApplicationInstalled(devSpace) == devSpaceNode.isInstalled()) {
                    devSpaceNode.setDevSpace(devSpace);
                } else {
                    model.removeNodeFromParent(devSpaceNode);
                    model.insertNodeInto(createDevSpaceNode(devSpace), root, i + 1);
                }
            }
        }

        if (needReload) {
            model.reload();
        }

        for (int i = 1; i < model.getChildCount(root); i++) {
            makeExpandedVisible((DevSpaceNode) model.getChild(root, i));
        }

        for (int i = 1; i < model.getChildCount(root); i++) {
            loadResourceNodes((DevSpaceNode) model.getChild(root, i));
        }
    }

    private DevSpaceNode createDevSpaceNode(DevSpace devSpace) throws IOException, InterruptedException {
        DevSpaceNode devSpaceNode = new DevSpaceNode(devSpace);

        if (NhctlHelper.isApplicationInstalled(devSpaceNode.getDevSpace())) {
            devSpaceNode.setInstalled(true);
            List<Pair<String, List<String>>> pairs = Lists.newArrayList(
                    Pair.create("Workloads", Lists.newArrayList(
                            "Deployments",
                            "DaemonSets",
                            "StatefulSets",
                            "Jobs",
                            "CronJobs",
                            "Pods"
                    )),
                    Pair.create("Network", Lists.newArrayList(
                            "Services",
                            "Endpoints",
                            "Ingresses",
                            "Network Policies"
                    )),
                    Pair.create("Configuration", Lists.newArrayList(
                            "ConfigMaps",
                            "Secrets",
                            "Resource Quotas",
                            "HPA",
                            "Pod Disruption Budgets"
                    )),
                    Pair.create("Storage", Lists.newArrayList(
                            "Persistent Volumes",
                            "Persistent Volume Claims",
                            "Storage Classes"
                    ))
            );
            for (Pair<String, List<String>> pair : pairs) {
                ResourceGroupNode resourceGroupNode = new ResourceGroupNode(pair.first);
                for (String name : pair.second) {
                    resourceGroupNode.add(new ResourceTypeNode(name));
                }
                devSpaceNode.add(resourceGroupNode);
            }
            ResourceGroupNode resourceGroupNode = (ResourceGroupNode) devSpaceNode.getChildAt(0);
            resourceGroupNode.setExpanded(true);
        }

        return devSpaceNode;
    }

    private DevSpaceNode cloneSelfAndChildren(DevSpaceNode oldDevSpaceNode) {
        DevSpaceNode newDevSpaceNode = oldDevSpaceNode.clone();
        for (int i = 0; i < oldDevSpaceNode.getChildCount(); i++) {
            ResourceGroupNode oldResourceGroupNode = (ResourceGroupNode) oldDevSpaceNode.getChildAt(i);
            ResourceGroupNode newResourceGroupNode = oldResourceGroupNode.clone();
            for (int j = 0; j < oldResourceGroupNode.getChildCount(); j++) {
                ResourceTypeNode oldResourceTypeNode = (ResourceTypeNode) oldResourceGroupNode.getChildAt(j);
                ResourceTypeNode newResourceTypeNode = oldResourceTypeNode.clone();
                for (int k = 0; k < oldResourceTypeNode.getChildCount(); k++) {
                    ResourceNode oldResourceNode = (ResourceNode) oldResourceTypeNode.getChildAt(k);
                    ResourceNode newResourceNode = oldResourceNode.clone();
                    newResourceTypeNode.add(newResourceNode);
                }
                newResourceGroupNode.add(newResourceTypeNode);
            }
            newDevSpaceNode.add(newResourceGroupNode);
        }
        return newDevSpaceNode;
    }

    private void makeExpandedVisible(DevSpaceNode devSpaceNode) {
        boolean devSpaceNodeExpanded = false;
        for (int i = 0; i < devSpaceNode.getChildCount(); i++) {
            ResourceGroupNode resourceGroupNode = (ResourceGroupNode) devSpaceNode.getChildAt(i);
            boolean resourceGroupNodeExpanded = false;
            for (int j = 0; j < resourceGroupNode.getChildCount(); j++) {
                ResourceTypeNode resourceTypeNode = (ResourceTypeNode) resourceGroupNode.getChildAt(j);
                if (resourceTypeNode.isExpanded()) {
                    this.expandPath(new TreePath(model.getPathToRoot(resourceTypeNode)));
                    resourceGroupNodeExpanded = true;
                }
            }
            if (resourceGroupNode.isExpanded() && !resourceGroupNodeExpanded) {
                this.expandPath(new TreePath(model.getPathToRoot(resourceGroupNode)));
                devSpaceNodeExpanded = true;
            }
        }
        if (devSpaceNode.isExpanded() && !devSpaceNodeExpanded) {
            this.expandPath(new TreePath(model.getPathToRoot(devSpaceNode)));
        }
    }

    private void loadResourceNodes(DevSpaceNode devSpaceNode) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        for (int i = 0; i < model.getChildCount(devSpaceNode); i++) {
            ResourceGroupNode resourceGroupNode = (ResourceGroupNode) model.getChild(devSpaceNode, i);
            for (int j = 0; j < model.getChildCount(resourceGroupNode); j++) {
                ResourceTypeNode resourceTypeNode = (ResourceTypeNode) model.getChild(resourceGroupNode, j);
                if (!resourceTypeNode.isLoaded()) {
                    continue;
                }
                loadKubeResources(resourceTypeNode);
            }
        }
    }

    private void loadKubeResources(ResourceTypeNode resourceTypeNode) throws IOException, InterruptedException, NocalhostExecuteCmdException {
        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
        final DevSpace devSpace = ((DevSpaceNode) resourceTypeNode.getParent().getParent()).getDevSpace();
        String resourceName = resourceTypeNode.getName().toLowerCase().replaceAll(" ", "");
        KubeResourceList kubeResourceList = kubectlCommand.getResourceList(resourceName, null, devSpace);

        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        final String kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace).toString();
        List<ResourceNode> resourceNodes = Lists.newArrayList();
        final NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions();
        nhctlDescribeOptions.setKubeconfig(kubeconfigPath);
        NhctlDescribeAllService nhctlDescribeAllService = nhctlCommand.describe(devSpace.getContext().getApplicationName(), nhctlDescribeOptions, NhctlDescribeAllService.class);
        final NhctlDescribeService[] nhctlDescribeServices = nhctlDescribeAllService.getSvcProfile();
        for (KubeResource kubeResource : kubeResourceList.getItems()) {

            final Optional<NhctlDescribeService> nhctlDescribeService = Arrays.stream(nhctlDescribeServices).filter(svc -> svc.getRawConfig().getName().equals(kubeResource.getMetadata().getName())).findFirst();
            if (StringUtils.equalsIgnoreCase(kubeResource.getKind(), "Deployment") && nhctlDescribeService.isPresent()) {
                NhctlDescribeService nhctlDescribe = nhctlDescribeService.get();
                if (nhctlDescribe.isDeveloping()) {
                    UserDataKeyHelper.addAliveDeployments(project, new AliveDeployment(devSpace, nhctlDescribe.getRawConfig().getName(), project.getProjectFilePath()));
                } else {
                    UserDataKeyHelper.removeAliveDeployments(project, new AliveDeployment(devSpace, nhctlDescribe.getRawConfig().getName(), project.getProjectFilePath()));
                }
                resourceNodes.add(new ResourceNode(kubeResource, nhctlDescribe));
            } else if (StringUtils.equalsIgnoreCase(kubeResource.getKind(), "StatefulSet")) {
                String metadataName = kubeResource.getMetadata().getName();
                KubeResource statefulsetKubeResource = kubectlCommand.getResource("StatefulSet/" + metadataName, "", devSpace);
                resourceNodes.add(new ResourceNode(statefulsetKubeResource));
            } else {
                resourceNodes.add(new ResourceNode(kubeResource));
            }
        }

        boolean needReload = false;

        if (resourceTypeNode.isLoaded()) {
            for (int k = model.getChildCount(resourceTypeNode) - 1; k >= 0; k--) {
                ResourceNode resourceNode = (ResourceNode) model.getChild(resourceTypeNode, k);
                boolean toBeRemoved = true;

                for (ResourceNode rn : resourceNodes) {
                    if (StringUtils.equals(rn.resourceName(), resourceNode.resourceName())) {
                        toBeRemoved = false;
                        break;
                    }
                }

                if (toBeRemoved) {
                    model.removeNodeFromParent(resourceNode);
                    needReload = true;
                }
            }
        } else {
            for (int i = model.getChildCount(resourceTypeNode) - 1; i >= 0; i--) {
                model.removeNodeFromParent((MutableTreeNode) model.getChild(resourceTypeNode, i));
                needReload = true;
            }
        }

        for (int i = 0; i < resourceNodes.size(); i++) {
            ResourceNode resourceNode = resourceNodes.get(i);

            if (model.getChildCount(resourceTypeNode) <= i) {
                model.insertNodeInto(resourceNode, resourceTypeNode, model.getChildCount(resourceTypeNode));
                needReload = true;
                continue;
            }

            ResourceNode rn = (ResourceNode) model.getChild(resourceTypeNode, i);

            if (StringUtils.equals(resourceNode.resourceName(), rn.resourceName())) {
                rn.setKubeResource(resourceNode.getKubeResource());
                rn.setNhctlDescribeService(resourceNode.getNhctlDescribeService());
                model.reload(rn);
            }
        }

        if (needReload) {
            model.reload(resourceTypeNode);
        }

    }
}