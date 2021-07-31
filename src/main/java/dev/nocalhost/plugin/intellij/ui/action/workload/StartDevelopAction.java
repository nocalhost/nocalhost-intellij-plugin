package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedGitCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlProfileSetOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;
import dev.nocalhost.plugin.intellij.ui.dialog.ImageChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.ListChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.KubeResourceUtil;
import dev.nocalhost.plugin.intellij.utils.PathsUtil;
import icons.NocalhostIcons;

public class StartDevelopAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(NocalhostSettings.class);

    private final OutputCapturedGitCommand outputCapturedGitCommand;
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private final AtomicReference<String> projectPath = new AtomicReference<>();
    private final AtomicReference<String> selectedContainer = new AtomicReference<>();

    public StartDevelopAction(Project project, ResourceNode node) {
        super("Start Develop", "", NocalhostIcons.Status.DevStart);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        outputCapturedGitCommand = project.getService(OutputCapturedGitCommand.class);
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);
                if (nhctlDescribeService.isDeveloping()) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showMessageDialog("Dev mode has been started.",
                                    "Start Develop", null));
                    return;
                }

                getContainers();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading service status error",
                        "Error occurs while loading service status", e);
            }
        });
    }

    private void getContainers() {
        List<String> containers = KubeResourceUtil.resolveContainers(node.getKubeResource());
        if (containers.size() > 1) {
            selectContainer(containers);
        } else if (containers.size() == 1) {
            selectedContainer.set(containers.get(0));
            getAssociate();
        }
    }

    private void selectContainer(List<String> containers) {
        ApplicationManager.getApplication().invokeLater(() -> {
            ListChooseDialog listChooseDialog = new ListChooseDialog(project, "Select Container",
                    containers);
            if (listChooseDialog.showAndGet()) {
                selectedContainer.set(listChooseDialog.getSelectedValue());
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
                    projectPath.set(nhctlDescribeService.getAssociate());
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
                        .filter(e -> StringUtils.equals(e.getName(), selectedContainer.get()))
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
                if (e instanceof NocalhostExecuteCmdException) {
                    return;
                }
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
                        project, "Specify git url.", "Start Develop", null);
            }
            if (StringUtils.isNotEmpty(gitUrl)) {
                Path gitParent = FileChooseUtil.chooseSingleDirectory(project, "",
                        "Select parent directory for git repository.");
                if (gitParent != null) {
                    String finalGitUrl = gitUrl;
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        try {
                            outputCapturedGitCommand.clone(gitParent.resolve(node.resourceName()), finalGitUrl);

                            if (!StringUtils.equals(url, finalGitUrl)) {
                                NhctlProfileSetOptions opts = new NhctlProfileSetOptions(kubeConfigPath, namespace);
                                opts.setDeployment(node.resourceName());
                                opts.setType(node.getKubeResource().getKind());
                                opts.setContainer(selectedContainer.get());
                                opts.setKey("gitUrl");
                                opts.setValue(finalGitUrl);
                                outputCapturedNhctlCommand.profileSet(node.applicationName(), opts);
                            }

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
        projectPath.set(path);
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
                        .filter(e -> StringUtils.equals(e.getName(), selectedContainer.get()))
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
                if (e instanceof NocalhostExecuteCmdException) {
                    return;
                }
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
                        opts.setContainer(selectedContainer.get());
                        opts.setKey("image");
                        opts.setValue(imageChooseDialog.getSelectedImage());
                        outputCapturedNhctlCommand.profileSet(node.applicationName(), opts);

                        startDevelop();
                    } catch (Exception e) {
                        if (e instanceof NocalhostExecuteCmdException) {
                            return;
                        }
                        ErrorUtil.dealWith(project, "Setting dev image",
                                "Error occurs while setting dev image", e);
                    }
                });
            }
        });
    }

    private void startDevelop() {
        projectPath.set(Paths.get(projectPath.get()).toString());
        ServiceProjectPath serviceProjectPath;
        if (node.getClusterNode().getNocalhostAccount() != null) {
            serviceProjectPath = ServiceProjectPath.builder()
                    .server(node.getClusterNode().getNocalhostAccount().getServer())
                    .username(node.getClusterNode().getNocalhostAccount().getUsername())
                    .clusterId(node.getClusterNode().getServiceAccount().getClusterId())
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getNamespace())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .serviceType(node.getKubeResource().getKind())
                    .containerName(selectedContainer.get())
                    .projectPath(projectPath.get())
                    .build();
        } else {
            serviceProjectPath = ServiceProjectPath.builder()
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getNamespace())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .serviceType(node.getKubeResource().getKind())
                    .containerName(selectedContainer.get())
                    .projectPath(projectPath.get())
                    .build();
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            if (PathsUtil.isSame(projectPath.get(), project.getBasePath())) {
                ProgressManager.getInstance().run(
                        new StartingDevModeTask(project, serviceProjectPath));
            } else {
                Project[] openProjects = ProjectManagerEx.getInstanceEx().getOpenProjects();
                for (Project openProject : openProjects) {
                    if (PathsUtil.isSame(projectPath.get(), openProject.getBasePath())) {
                        ToolWindow toolWindow = ToolWindowManager.getInstance(openProject)
                                .getToolWindow(ToolWindowId.PROJECT_VIEW);
                        if (toolWindow != null) {
                            toolWindow.activate(() -> {
                                ProgressManager.getInstance().run(
                                        new StartingDevModeTask(openProject, serviceProjectPath));
                            });
                            return;
                        }
                    }
                }
                nocalhostSettings.setDevModeServiceToProjectPath(serviceProjectPath);
                ProjectManagerEx.getInstanceEx().openProject(Paths.get(projectPath.get()),
                        new OpenProjectTask());
            }
        });
    }
}
