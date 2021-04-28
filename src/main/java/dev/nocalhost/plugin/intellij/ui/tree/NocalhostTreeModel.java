package dev.nocalhost.plugin.intellij.ui.tree;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.LoadingNode;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.AliveDeployment;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeAllService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplication;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.helpers.UserDataKeyHelper;
import dev.nocalhost.plugin.intellij.settings.NocalhostRepo;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.ui.tree.node.AccountNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ApplicationNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.DefaultResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceGroupNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceKeeperNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceTypeNode;
import dev.nocalhost.plugin.intellij.utils.Constants;

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

    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);

    private final Project project;

    public NocalhostTreeModel(Project project) {
        super(new NocalhostTreeNodeComparator());
        this.project = project;
        insertNode(new LoadingNode(), (MutableTreeNode) root);
    }

    public void update(List<DevSpace> devSpaces,
                       List<Application> applications,
                       List<NhctlListApplication> nhctlListApplications) {

        updateAccount();
        updateDevSpaces(devSpaces, applications, nhctlListApplications);
    }

    private void updateAccount() {
        if (getChild(root, 0) instanceof AccountNode) {
            AccountNode node = (AccountNode) getChild(root, 0);
            node.setUserInfo(nocalhostSettings.getUserInfo());
            nodeChanged(node);
        }
        if (getChild(root, 0) instanceof LoadingNode) {
            LoadingNode node = (LoadingNode) getChild(root, 0);
            removeNode(node);
            insertNode(new AccountNode(nocalhostSettings.getUserInfo()), (MutableTreeNode) root);
        }
    }

    private void updateDevSpaces(List<DevSpace> devSpaces,
                                 List<Application> applications,
                                 List<NhctlListApplication> nhctlListApplications) {
        if (getChildCount(root) > 1) {
            for (int i = getChildCount(root) - 1; i >= 1; i--) {
                DevSpaceNode devSpaceNode = (DevSpaceNode) getChild(root, i);
                Optional<DevSpace> devSpaceOptional = devSpaces.stream()
                        .filter(devSpace -> devSpace.getId() == devSpaceNode.getDevSpace().getId())
                        .findFirst();
                if (devSpaceOptional.isPresent()) {
                    devSpaceNode.setDevSpace(devSpaceOptional.get());
                    nodeChanged(devSpaceNode);

                    updateApplications(
                            devSpaceNode,
                            findInstalledApplications(
                                    devSpaceNode.getDevSpace(),
                                    applications,
                                    nhctlListApplications
                            )
                    );
                    updateDefaultResources(devSpaceNode);
                } else {
                    removeNode(devSpaceNode);
                }
            }
        }
        for (DevSpace devSpace : devSpaces) {
            boolean existed = false;
            for (int i = 1; i < getChildCount(root); i++) {
                if (getChild(root, i) instanceof DevSpaceNode) {
                    DevSpaceNode devSpaceNode = (DevSpaceNode) getChild(root, i);
                    if (devSpace.getId() == devSpaceNode.getDevSpace().getId()) {
                        existed = true;
                        break;
                    }
                }
            }
            if (!existed) {
                DevSpaceNode devSpaceNode = new DevSpaceNode(devSpace);
                insertNode(devSpaceNode, (MutableTreeNode) root);

                DefaultResourceNode defaultResourceNode = new DefaultResourceNode();
                insertNode(defaultResourceNode, devSpaceNode);
                createResourceGroupTypeStructure(defaultResourceNode);

                updateApplications(
                        devSpaceNode,
                        findInstalledApplications(devSpace, applications, nhctlListApplications)
                );
                updateDefaultResources(devSpaceNode);
            }
        }
    }

    private void updateApplications(DevSpaceNode devSpaceNode, List<Application> applications) {
        if (getChildCount(devSpaceNode) > 1) {
            for (int i = getChildCount(devSpaceNode) - 2; i >= 0; i--) {
                ApplicationNode applicationNode = (ApplicationNode) getChild(devSpaceNode, i);
                Optional<Application> applicationOptional = applications.stream()
                        .filter(app -> StringUtils.equals(
                                app.getContext().getApplicationName(),
                                applicationNode.getApplication().getContext().getApplicationName()
                        ))
                        .findFirst();
                if (applicationOptional.isPresent()) {
                    applicationNode.setApplication(applicationOptional.get());
                    nodeChanged(applicationNode);

                    updateApplicationResources(applicationNode);
                } else {
                    removeNode(applicationNode);
                }
            }
        }
        for (Application application : applications) {
            boolean existed = false;
            for (int i = 0; i < getChildCount(devSpaceNode) - 1; i++) {
                if (getChild(devSpaceNode, i) instanceof ApplicationNode) {
                    ApplicationNode applicationNode = (ApplicationNode) getChild(devSpaceNode, i);
                    if (StringUtils.equals(
                            application.getContext().getApplicationName(),
                            applicationNode.getApplication().getContext().getApplicationName())) {
                        existed = true;
                        break;
                    }
                }
            }
            if (!existed) {
                ApplicationNode applicationNode = new ApplicationNode(application);
                insertNode(applicationNode, devSpaceNode);
                createResourceGroupTypeStructure(applicationNode);

                updateApplicationResources(applicationNode);
            }
        }
    }

    private List<Application> findInstalledApplications(
            DevSpace devSpace,
            List<Application> applications,
            List<NhctlListApplication> nhctlListApplications) {
        List<Application> devSpaceApplications = Lists.newArrayList();
        Optional<NhctlListApplication> nhctlListApplicationOptional = nhctlListApplications.stream()
                .filter(e -> StringUtils.equals(e.getNamespace(), devSpace.getNamespace()))
                .findFirst();
        if (nhctlListApplicationOptional.isPresent()
                && nhctlListApplicationOptional.get().getApplication() != null) {
            NhctlListApplication.Application[] installedApplications =
                    nhctlListApplicationOptional.get().getApplication();
            for (NhctlListApplication.Application installedApplication : installedApplications) {
                Optional<Application> applicationOptional = applications.stream()
                        .filter(e -> StringUtils.equals(
                                e.getContext().getApplicationName(),
                                installedApplication.getName()
                        ))
                        .findFirst();
                applicationOptional.ifPresent(devSpaceApplications::add);
            }
        }
        return devSpaceApplications;
    }


    private void createResourceGroupTypeStructure(ResourceKeeperNode resourceKeeperNode) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) resourceKeeperNode;
        for (Pair<String, List<String>> pair : RESOURCE_GROUP_TYPE) {
            String group = pair.first;
            ResourceGroupNode resourceGroupNode = new ResourceGroupNode(group);
            node.add(resourceGroupNode);
            for (String type : pair.second) {
                ResourceTypeNode resourceTypeNode = new ResourceTypeNode(type);
                resourceGroupNode.add(resourceTypeNode);
                LoadingNode loadingNode = new LoadingNode();
                resourceTypeNode.add(loadingNode);
            }
        }
        nodeStructureChanged(node);
    }

    private void updateDefaultResources(DevSpaceNode devSpaceNode) {
        Object object = getChild(devSpaceNode, getChildCount(devSpaceNode) - 1);
        if (object instanceof DefaultResourceNode) {
            DefaultResourceNode defaultResourceNode = (DefaultResourceNode) object;
            for (int i = 0; i < getChildCount(defaultResourceNode); i++) {
                ResourceGroupNode resourceGroupNode =
                        (ResourceGroupNode) getChild(defaultResourceNode, i);
                for (int j = 0; j < getChildCount(resourceGroupNode); j++) {
                    ResourceTypeNode resourceTypeNode =
                            (ResourceTypeNode) getChild(resourceGroupNode, j);
                    updateResources(resourceTypeNode);
                }
            }
        }
    }

    private void updateApplicationResources(ApplicationNode applicationNode) {
        for (int i = 0; i < getChildCount(applicationNode); i++) {
            ResourceGroupNode resourceGroupNode = (ResourceGroupNode) getChild(applicationNode, i);
            for (int j = 0; j < getChildCount(resourceGroupNode); j++) {
                ResourceTypeNode resourceTypeNode =
                        (ResourceTypeNode) getChild(resourceGroupNode, j);
                updateResources(resourceTypeNode);
            }
        }
    }

    void updateResources(ResourceTypeNode resourceTypeNode) {
        if (!resourceTypeNode.isLoaded()) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<ResourceNode> resources = fetchResourcesData(resourceTypeNode);
                if (resources == null) {
                    return;
                }
                ApplicationManager.getApplication().invokeLater(() -> {
                    updateResources(resourceTypeNode, resources);
                });
            } catch (InterruptedException | NocalhostExecuteCmdException | IOException e) {
                LOG.error(e);
                if (StringUtils.contains(e.getMessage(), "No such file or directory")) {
                    NocalhostNotifier.getInstance(project).notifyNhctlNotFound();
                } else {
                    NocalhostNotifier.getInstance(project).notifyError(
                            "Nocalhost update tree error",
                            "Error occurred while updating tree",
                            e.getMessage());
                }
            }
        });
    }

    void updateResources(ResourceTypeNode resourceTypeNode, List<ResourceNode> resources) {
        synchronized (resourceTypeNode) {
            // remove loading node
            if (getChildCount(resourceTypeNode) > 0
                    && getChild(resourceTypeNode, 0) instanceof LoadingNode) {
                removeNode((MutableTreeNode) getChild(resourceTypeNode, 0));
            }

            // update resource nodes
            for (int i = getChildCount(resourceTypeNode) - 1; i >= 0; i--) {
                ResourceNode resourceNode = (ResourceNode) getChild(resourceTypeNode, i);
                Optional<ResourceNode> resourceOptional = resources.stream()
                        .filter(e -> StringUtils.equals(resourceNode.resourceName(), e.resourceName()))
                        .findFirst();
                if (resourceOptional.isPresent()) {
                    ResourceNode resource = resourceOptional.get();
                    resourceNode.setKubeResource(resource.getKubeResource());
                    resourceNode.setNhctlDescribeService(resource.getNhctlDescribeService());
                    nodeChanged(resourceNode);
                } else {
                    removeNode(resourceNode);
                }
            }
            for (ResourceNode resource : resources) {
                boolean existed = false;
                for (int i = 0; i < getChildCount(resourceTypeNode); i++) {
                    if (getChild(resourceTypeNode, i) instanceof ResourceNode) {
                        ResourceNode resourceNode = (ResourceNode) getChild(resourceTypeNode, i);
                        if (StringUtils.equals(resourceNode.resourceName(), resource.resourceName())) {
                            existed = true;
                            break;
                        }
                    }
                }
                if (!existed) {
                    insertNode(resource, resourceTypeNode);
                }
            }
        }
    }

    private List<ResourceNode> fetchResourcesData(ResourceTypeNode resourceTypeNode)
            throws InterruptedException, NocalhostExecuteCmdException, IOException {
        DevSpace devSpace;
        String applicationName;
        TreeNode parent = resourceTypeNode.getParent().getParent();
        if (parent instanceof ApplicationNode) {
            ApplicationNode applicationNode = (ApplicationNode) parent;
            devSpace = applicationNode.getDevSpace();
            applicationName = applicationNode.getApplication().getContext().getApplicationName();
        } else {
            DefaultResourceNode defaultResourceNode = (DefaultResourceNode) parent;
            devSpace = defaultResourceNode.getDevSpace();
            applicationName = Constants.DEFAULT_APPLICATION_NAME;
        }

        String kubeResourceType = resourceTypeNode.getName().toLowerCase()
                .replaceAll(" ", "");
        KubeResourceList kubeResourceList = kubectlCommand.getResourceList(kubeResourceType, null,
                devSpace);

        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(devSpace);
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

        NhctlDescribeService[] nhctlDescribeServices = nhctlDescribeAllService.getSvcProfile();

        List<KubeResource> kubeResources = kubeResourceList.getItems().stream()
                .filter(e ->
                        StringUtils.equals(
                                e.getMetadata().getAnnotations().get(
                                        Constants.NOCALHOST_ANNOTATION_NAME),
                                applicationName
                        )
                                || StringUtils.equals(
                                e.getMetadata().getAnnotations().get(
                                        Constants.HELM_ANNOTATION_NAME),
                                applicationName
                        )
                )
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(kubeResources)) {
            kubeResources = kubeResourceList.getItems().stream()
                    .filter(e ->
                            StringUtils.isBlank(
                                    e.getMetadata().getAnnotations().get(NOCALHOST_ANNOTATION_NAME)
                            )
                                    && StringUtils.isBlank(
                                    e.getMetadata().getAnnotations().get(HELM_ANNOTATION_NAME)
                            )
                    )
                    .collect(Collectors.toList());
        }

        List<ResourceNode> resources = Lists.newArrayList();
        for (KubeResource kubeResource : kubeResources) {
            final Optional<NhctlDescribeService> nhctlDescribeService =
                    ArrayUtils.isEmpty(nhctlDescribeServices)
                            ? Optional.empty()
                            : Arrays.stream(nhctlDescribeServices)
                            .filter(svc -> StringUtils.equals(
                                    svc.getRawConfig().getName(),
                                    kubeResource.getMetadata().getName()))
                            .findFirst();
            if (StringUtils.equalsIgnoreCase(kubeResource.getKind(), "Deployment")
                    && nhctlDescribeService.isPresent()) {
                NhctlDescribeService nhctlDescribe = nhctlDescribeService.get();
                final Optional<NocalhostRepo> nocalhostRepo =
                        nocalhostSettings.getRepos().stream()
                                .filter(repo ->
                                        Objects.equals(
                                                nocalhostSettings.getBaseUrl(), repo.getHost()
                                        )
                                                && Objects.equals(
                                                nocalhostSettings.getUserInfo().getEmail(),
                                                repo.getEmail()
                                        )
                                                && Objects.equals(applicationName, repo.getAppName())
                                                && Objects.equals(devSpace.getId(), repo.getDevSpaceId())
                                                && Objects.equals(
                                                nhctlDescribe.getRawConfig().getName(),
                                                repo.getDeploymentName()
                                        )
                                )
                                .findFirst();
                if (nhctlDescribe.isDeveloping()) {
                    nocalhostRepo.ifPresent(repos ->
                            UserDataKeyHelper.addAliveDeployments(
                                    project,
                                    new AliveDeployment(
                                            devSpace,
                                            applicationName,
                                            nhctlDescribe.getRawConfig().getName(),
                                            repos.getRepoPath()
                                    )
                            )
                    );
                } else {
                    nocalhostRepo.ifPresent(repos ->
                            UserDataKeyHelper.removeAliveDeployments(
                                    project,
                                    new AliveDeployment(
                                            devSpace,
                                            applicationName,
                                            nhctlDescribe.getRawConfig().getName(),
                                            repos.getRepoPath()
                                    )
                            )
                    );
                }
                resources.add(new ResourceNode(kubeResource, nhctlDescribe));
            } else if (StringUtils.equalsIgnoreCase(kubeResource.getKind(), "StatefulSet")) {
                String metadataName = kubeResource.getMetadata().getName();
                KubeResource statefulsetKubeResource = kubectlCommand.getResource("" +
                        "StatefulSet/" + metadataName, "", devSpace);
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
}
