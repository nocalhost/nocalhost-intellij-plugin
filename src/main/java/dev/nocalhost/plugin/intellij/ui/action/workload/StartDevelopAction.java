package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.GitCommand;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlProfileSetOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;
import dev.nocalhost.plugin.intellij.ui.dialog.StartDevelopDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import icons.NocalhostIcons;

public class StartDevelopAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(StartDevelopAction.class);

    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
    private final GitCommand gitCommand = ServiceManager.getService(GitCommand.class);
    private final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private List<String> containers;
    private NhctlDescribeService nhctlDescribeService;

    public StartDevelopAction(Project project, ResourceNode node) {
        super("Start Develop", "", NocalhostIcons.Status.DevStart);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getName();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                KubeResource deployment = kubectlCommand.getResource("deployment",
                        node.resourceName(), kubeConfigPath, namespace);
                KubeResourceList podList = kubectlCommand.getResourceList("pods",
                        deployment.getSpec().getSelector().getMatchLabels(), kubeConfigPath, namespace);
                List<KubeResource> pods = podList.getItems().stream()
                        .filter(KubeResource::canSelector)
                        .collect(Collectors.toList());
                containers = pods.get(0)
                        .getSpec()
                        .getContainers()
                        .stream()
                        .map(KubeResource.Spec.Container::getName)
                        .collect(Collectors.toList());

                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);

                showStartDevelopDialog();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading service profile error",
                        "Error occurs while loading service profile", e);
            }
        });
    }

    private void showStartDevelopDialog() {
        ApplicationManager.getApplication().invokeLater(() -> {
            StartDevelopDialog startDevelopDialog = new StartDevelopDialog(project, containers, nhctlDescribeService);
            if (!startDevelopDialog.showAndGet()) {
                return;
            }
            updateConfig(startDevelopDialog);
        });
    }

    private void updateConfig(StartDevelopDialog startDevelopDialog) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path sourceDirectory = startDevelopDialog.getSourceDirectory();

                if (!StringUtils.isNotEmpty(nhctlDescribeService.getAssociate())) {
                    if (startDevelopDialog.isCloneFromGit()) {
                        gitCommand.clone(startDevelopDialog.getSourceDirectory(),
                                startDevelopDialog.getGitUrl(), node.resourceName(), project);
                        sourceDirectory = startDevelopDialog.getSourceDirectory().resolve(node.resourceName());
                    }

                    NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(kubeConfigPath, namespace);
                    opts.setAssociate(sourceDirectory.toString());
                    opts.setDeployment(node.resourceName());
                    opts.setControllerType(node.getKubeResource().getKind());
                    nhctlCommand.devAssociate(node.applicationName(), opts);
                }

                Optional<ServiceContainer> serviceContainerOptional = nhctlDescribeService
                        .getRawConfig()
                        .getContainers()
                        .stream()
                        .filter(e -> StringUtils.equals(e.getName(), startDevelopDialog.getSelectedContainer()))
                        .findFirst();
                if (serviceContainerOptional.isEmpty()
                        || !StringUtils.equals(serviceContainerOptional.get().getDev().getImage(), startDevelopDialog.getSelectedImage())) {
                    NhctlProfileSetOptions opts = new NhctlProfileSetOptions(kubeConfigPath, namespace);
                    opts.setDeployment(node.resourceName());
                    opts.setType(node.getKubeResource().getKind());
                    opts.setContainer(startDevelopDialog.getSelectedContainer());
                    opts.setKey("image");
                    opts.setValue(startDevelopDialog.getSelectedImage());
                    nhctlCommand.profileSet(node.applicationName(), opts);
                }

                startDevelop(startDevelopDialog.getSelectedContainer(), sourceDirectory);
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Updating service profile error",
                        "Error occurs while updating service profile", e);
            }
        });
    }

    private void startDevelop(String containerName, Path projectPath) {
        ServiceProjectPath serviceProjectPath;
        if (node.getClusterNode().getNocalhostAccount() != null) {
            serviceProjectPath = ServiceProjectPath.builder()
                    .server(node.getClusterNode().getNocalhostAccount().getServer())
                    .username(node.getClusterNode().getNocalhostAccount().getUsername())
                    .clusterId(node.getClusterNode().getServiceAccount().getClusterId())
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getName())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .containerName(containerName)
                    .build();
        } else {
            serviceProjectPath = ServiceProjectPath.builder()
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getName())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .containerName(containerName)
                    .build();
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            if (StringUtils.equals(projectPath.toString(), project.getBasePath())) {
                ProgressManager.getInstance().run(new StartingDevModeTask(project, serviceProjectPath));
            } else {
                serviceProjectPath.setProjectPath(projectPath.toString());
                nocalhostSettings.setDevModeServiceToProjectPath(serviceProjectPath);

                ProjectManagerEx.getInstanceEx().openProject(projectPath, new OpenProjectTask());
            }
        });
    }
}
