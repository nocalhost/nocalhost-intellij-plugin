package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
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
import lombok.SneakyThrows;

public class StartDevelopAction extends DumbAwareAction {
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
                String sourceDirectory = startDevelopDialog.getSourceDirectory().toAbsolutePath().toString();
                if (startDevelopDialog.isCloneFromGit()) {
                    sourceDirectory = startDevelopDialog.getSourceDirectory().resolve(node.resourceName()).toAbsolutePath().toString();
                }
                if (!StringUtils.equals(nhctlDescribeService.getAssociate(), sourceDirectory)) {
                    NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(kubeConfigPath, namespace);
                    opts.setAssociate(sourceDirectory);
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

                startDevelop(startDevelopDialog, sourceDirectory);
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Updating service profile error",
                        "Error occurs while updating service profile", e);
            }
        });
    }

    private void startDevelop(StartDevelopDialog startDevelopDialog, String projectPath) {
        String containerName = startDevelopDialog.getSelectedContainer();

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
                    .serviceType(node.getKubeResource().getKind())
                    .containerName(containerName)
                    .projectPath(projectPath)
                    .build();
        } else {
            serviceProjectPath = ServiceProjectPath.builder()
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getName())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .serviceType(node.getKubeResource().getKind())
                    .containerName(containerName)
                    .projectPath(projectPath)
                    .build();
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            if (startDevelopDialog.isCloneFromGit()) {
                ProgressManager.getInstance().run(new Task.Backgroundable(
                        project, "Git clone " + startDevelopDialog.getGitUrl(), false) {
                    @Override
                    public void onSuccess() {
                        startDevMode(serviceProjectPath);
                    }

                    @Override
                    public void onThrowable(@NotNull Throwable e) {
                        ErrorUtil.dealWith(project, "Cloning git repository error",
                                "Error occurs while cloning git repository", e);
                    }

                    @SneakyThrows
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        gitCommand.clone(startDevelopDialog.getSourceDirectory(),
                                startDevelopDialog.getGitUrl(), node.resourceName(), project);
                    }
                });
            } else {
                startDevMode(serviceProjectPath);
            }
        });
    }

    public void startDevMode(ServiceProjectPath serviceProjectPath) {
        String projectPath = serviceProjectPath.getProjectPath();
        if (StringUtils.equals(projectPath, project.getBasePath())) {
            ProgressManager.getInstance().run(new StartingDevModeTask(project, serviceProjectPath));
        } else {
            nocalhostSettings.setDevModeServiceToProjectPath(serviceProjectPath);
            ProjectManagerEx.getInstanceEx().openProject(Path.of(projectPath), new OpenProjectTask());
        }
    }
}
