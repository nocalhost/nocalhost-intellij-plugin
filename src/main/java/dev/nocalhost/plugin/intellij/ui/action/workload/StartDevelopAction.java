package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedGitCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
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
import dev.nocalhost.plugin.intellij.ui.dialog.ImageChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.ListChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import icons.NocalhostIcons;

public class StartDevelopAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
    private final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

    private final OutputCapturedGitCommand outputCapturedGitCommand;
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private String projectPath;
    private String selectedContainer;

    public StartDevelopAction(Project project, ResourceNode node) {
        super("Start Develop", "", NocalhostIcons.Status.DevStart);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getName();
        outputCapturedGitCommand = project.getService(OutputCapturedGitCommand.class);
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                KubeResource deployment = kubectlCommand.getResource("deployment",
                        node.resourceName(), kubeConfigPath, namespace);
                KubeResourceList podList = kubectlCommand.getResourceList(
                        "pods",
                        deployment.getSpec().getSelector().getMatchLabels(),
                        kubeConfigPath,
                        namespace);
                List<KubeResource> pods = podList.getItems().stream()
                        .filter(KubeResource::canSelector)
                        .collect(Collectors.toList());
                List<String> containers = pods.get(0)
                        .getSpec()
                        .getContainers()
                        .stream()
                        .map(KubeResource.Spec.Container::getName)
                        .collect(Collectors.toList());

                if (containers.size() > 1) {
                    selectContainer(containers);
                } else {
                    selectedContainer = containers.get(0);
                    getAssociate();
                }
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading containers error",
                        "Error occurs while loading containers", e);
            }
        });
    }

    private void selectContainer(List<String> containers) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ListChooseDialog listChooseDialog = new ListChooseDialog(project, "Select Container",
                    containers);
            if (listChooseDialog.showAndGet()) {
                selectedContainer = listChooseDialog.getSelectedValue();
                getAssociate();
            }
        });
    }

    private void getAssociate() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);

                if (StringUtils.isNotEmpty(nhctlDescribeService.getAssociate())) {
                    projectPath = nhctlDescribeService.getAssociate();
                    getImage();
                } else {
                    selectCodeSource();
                }
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading service profile error",
                        "Error occurs while loading service profile", e);
            }
        });
    }

    private void selectCodeSource() {
        ApplicationManager.getApplication().invokeLater(() -> {
            int exitCode = MessageDialogBuilder
                    .yesNoCancel(
                            "Start develop",
                            "To start develop, you must specify source code directory."
                    )
                    .yesText("Clone from Git Repo")
                    .noText("Open local directly")
                    .guessWindowAndAsk();

            switch (exitCode) {
                case Messages.YES:
                    getGitUrl();
                    break;
                case Messages.NO:
                    selectDirectory();
                    break;
                default:
                    return;
            }
        });
    }

    private void getGitUrl() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);

                Optional<ServiceContainer> serviceContainerOptional = nhctlDescribeService
                        .getRawConfig()
                        .getContainers()
                        .stream()
                        .filter(e -> StringUtils.equals(e.getName(), selectedContainer))
                        .findFirst();
                if (serviceContainerOptional.isPresent()) {
                    ServiceContainer serviceContainer = serviceContainerOptional.get();
                    if (serviceContainer.getDev() != null
                            && StringUtils.isNotEmpty(serviceContainer.getDev().getGitUrl())) {
                        cloneGitRepository(serviceContainer.getDev().getGitUrl());
                        return;
                    }
                }

                if (nhctlDescribeService.getRawConfig().getContainers().size() == 1
                        && StringUtils.equals(nhctlDescribeService.getRawConfig().getContainers().get(0).getName(), "")) {
                    ServiceContainer serviceContainer = nhctlDescribeService.getRawConfig().getContainers().get(0);
                    if (serviceContainer.getDev() != null
                            && StringUtils.isNotEmpty(serviceContainer.getDev().getGitUrl())) {
                        cloneGitRepository(serviceContainer.getDev().getGitUrl());
                        return;
                    }
                }

                cloneGitRepository("");
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading dev image error",
                        "Error occurs while loading dev image", e);
            }
        });
    }

    private void cloneGitRepository(String url) {
        ApplicationManager.getApplication().invokeLater(() -> {
            String gitUrl = url;
            if (!StringUtils.isNotEmpty(gitUrl)) {
                gitUrl = Messages.showInputDialog(
                        project,
                        "Specify git url.",
                        "Start develop",
                        null);
            }
            if (StringUtils.isNotEmpty(gitUrl)) {
                Path gitParent = FileChooseUtil.chooseSingleDirectory(project, "",
                        "Select parent directory for git repository.");
                if (gitParent != null) {
                    String finalGitUrl = gitUrl;
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            outputCapturedGitCommand.clone(gitParent, finalGitUrl, node.resourceName());
                            setAssociate(gitParent.resolve(node.resourceName()).toAbsolutePath()
                                    .toString());
                        } catch (Exception e) {
                            ErrorUtil.dealWith(project, "Cloning git repository error",
                                    "Error occurs while cloning git repository", e);
                        }
                    });
                }
            }
        });
    }

    private void selectDirectory() {
        ApplicationManager.getApplication().invokeLater(() -> {
            Path codeSource = FileChooseUtil.chooseSingleDirectory(project, "",
                    "Select source code directory.");
            if (codeSource != null) {
                setAssociate(codeSource.toString());
            }
        });
    }

    private void setAssociate(String path) {
        projectPath = path;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(
                        kubeConfigPath, namespace);
                opts.setAssociate(path);
                opts.setDeployment(node.resourceName());
                opts.setControllerType(node.getKubeResource().getKind());
                outputCapturedNhctlCommand.devAssociate(node.applicationName(), opts);

                getImage();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Associating source code directory error",
                        "Error occurs while associating source code directory", e);
            }
        });
    }

    private void getImage() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);

                Optional<ServiceContainer> serviceContainerOptional = nhctlDescribeService
                        .getRawConfig()
                        .getContainers()
                        .stream()
                        .filter(e -> StringUtils.equals(e.getName(), selectedContainer))
                        .findFirst();
                if (serviceContainerOptional.isPresent()) {
                    ServiceContainer serviceContainer = serviceContainerOptional.get();
                    if (serviceContainer.getDev() != null
                            && StringUtils.isNotEmpty(serviceContainer.getDev().getImage())) {
                        startDevelop();
                        return;
                    }
                }

                if (nhctlDescribeService.getRawConfig().getContainers().size() == 1
                        && StringUtils.equals(nhctlDescribeService.getRawConfig().getContainers().get(0).getName(), "")) {
                    ServiceContainer serviceContainer = nhctlDescribeService.getRawConfig().getContainers().get(0);
                    if (serviceContainer.getDev() != null
                            && StringUtils.isNotEmpty(serviceContainer.getDev().getImage())) {
                        startDevelop();
                        return;
                    }
                }

                selectImage();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading dev image",
                        "Error occurs while loading dev image", e);
            }
        });
    }

    private void selectImage() {
        ApplicationManager.getApplication().invokeLater(() -> {
            ImageChooseDialog imageChooseDialog = new ImageChooseDialog(project);
            if (imageChooseDialog.showAndGet()) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        NhctlProfileSetOptions opts = new NhctlProfileSetOptions(kubeConfigPath,
                                namespace);
                        opts.setDeployment(node.resourceName());
                        opts.setType(node.getKubeResource().getKind());
                        opts.setContainer(selectedContainer);
                        opts.setKey("image");
                        opts.setValue(imageChooseDialog.getSelectedImage());
                        outputCapturedNhctlCommand.profileSet(node.applicationName(), opts);

                        startDevelop();
                    } catch (Exception e) {
                        ErrorUtil.dealWith(project, "Setting dev image",
                                "Error occurs while setting dev image", e);
                    }
                });
            }
        });
    }

    private void startDevelop() {
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
                    .containerName(selectedContainer)
                    .projectPath(projectPath)
                    .build();
        } else {
            serviceProjectPath = ServiceProjectPath.builder()
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getName())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .serviceType(node.getKubeResource().getKind())
                    .containerName(selectedContainer)
                    .projectPath(projectPath)
                    .build();
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            if (StringUtils.equals(projectPath, project.getBasePath())) {
                ProgressManager.getInstance().run(
                        new StartingDevModeTask(project, serviceProjectPath));
            } else {
                nocalhostSettings.setDevModeServiceToProjectPath(serviceProjectPath);
                ProjectManagerEx.getInstanceEx().openProject(Paths.get(projectPath),
                        new OpenProjectTask());
            }
        });
    }
}
