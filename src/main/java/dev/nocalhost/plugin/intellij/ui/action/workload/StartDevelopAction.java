package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.google.gson.reflect.TypeToken;

import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedGitCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlProfileGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlProfileSetOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.nhctl.NhctlDevContainerListCommand;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.DevModeService;
import dev.nocalhost.plugin.intellij.task.ExecutionTask;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;
import dev.nocalhost.plugin.intellij.ui.dialog.ImageChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.ListChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import dev.nocalhost.plugin.intellij.utils.PathsUtil;
import icons.NocalhostIcons;

import static dev.nocalhost.plugin.intellij.utils.Constants.DEV_MODE_DUPLICATE;

public class StartDevelopAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(NocalhostSettings.class);

    private final OutputCapturedGitCommand outputCapturedGitCommand;
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final String mode;
    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private final AtomicReference<String> projectPathReference = new AtomicReference<>();
    private final AtomicReference<String> selectedContainer = new AtomicReference<>();
    private final AtomicReference<String> selectedImage = new AtomicReference<>();

    private final String action;

    public StartDevelopAction(Project project, ResourceNode node, String mode) {
        this(
                StringUtils.equals(mode, DEV_MODE_DUPLICATE) ? "Start DevMode (Duplicate)" : "Start DevMode",
                project,
                node,
                StringUtils.equals(mode, DEV_MODE_DUPLICATE) ? NocalhostIcons.Status.DevCopy : NocalhostIcons.Status.DevStart,
                mode,
                ""
        );
    }

    public StartDevelopAction(String title, Project project, ResourceNode node, Icon icon, String mode, String action) {
        super(title, "", icon);
        this.node = node;
        this.mode = mode;
        this.action = action;
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        outputCapturedGitCommand = project.getService(OutputCapturedGitCommand.class);
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDevStartOptions devStartOptions = new NhctlDevStartOptions(kubeConfigPath, namespace);
                devStartOptions.setControllerType(node.getKubeResource().getKind());
                devStartOptions.setAuthCheck(true);
                nhctlCommand.devStart(node.applicationName(), devStartOptions);

                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);

                if (NhctlDescribeServiceUtil.developStarted(nhctlDescribeService)) {
                    // remote run | remote debug
                    if (StringUtils.isNotEmpty(action)) {
                        ProgressManager
                                .getInstance()
                                .run(new ExecutionTask(project, makeDevModeService(project.getBasePath()), action));
                        return;
                    }
                    // currently the service in the duplicate mode
                    if (StringUtils.equals(nhctlDescribeService.getDevModeType(), DEV_MODE_DUPLICATE)) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showMessageDialog(
                                        "Dev mode has been started.",
                                        "Start DevMode",
                                        null
                                )
                        );
                        return;
                    }
                    // currently the service in the replace mode
                    if ( ! StringUtils.equals(mode, DEV_MODE_DUPLICATE)) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showMessageDialog("Dev mode has been started.",
                                        "Start DevMode", null));
                        return;
                    }
                }

                getContainers();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading service status error",
                        "Error occurs while loading service status", e);
            }
        });
    }

    private void getContainers() {
        List<String> containers;

        try {
            var cmd = new NhctlDevContainerListCommand();
            cmd.setNamespace(namespace);
            cmd.setKubeConfig(kubeConfigPath);
            cmd.setDeployment(node.resourceName());
            cmd.setApplication(node.applicationName());
            cmd.setControllerType(node.getKubeResource().getKind());
            containers = DataUtils.GSON.fromJson(cmd.execute(), TypeToken.getParameterized(List.class, String.class).getType());
        } catch (Exception ex) {
            ErrorUtil.dealWith(project, "Failed to get containers", "Error occurs while get containers", ex);
            return;
        }

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
                NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setControllerType(node.getKubeResource().getKind());
                opts.setContainer(selectedContainer.get());
                opts.setInfo(true);
                String associatedPath = nhctlCommand.devAssociate(node.applicationName(), opts);
                if (StringUtils.isNotEmpty(associatedPath)
                        && Files.exists(Paths.get(associatedPath))) {
                    projectPathReference.set(associatedPath);
                    getImage();
                } else {
                    selectCodeSource();
                }
            } catch (NocalhostExecuteCmdException e) {
                ErrorUtil.dealWith(project, "Loading service profile error",
                        "Error occurs while loading service profile", e);
                selectCodeSource();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading service profile error",
                        "Error occurs while loading service profile", e);
            }
        });
    }

    private void selectCodeSource() {
        ApplicationManager.getApplication().invokeLater(() -> {
            int choice = Messages.showDialog(
                    project,
                    "To start dev mode, you must specify source code directory.",
                    "Start DevMode",
                    new String[]{
                            "Clone from Git Repo",
                            "Open Local Directly",
                            "Cancel"},
                    0,
                    Messages.getQuestionIcon());
            switch (choice) {
                case 0:
                    getGitUrl();
                    break;
                case 1:
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
                        project, "Specify git url.", "Start DevMode", null);
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
        projectPathReference.set(path);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(
                        kubeConfigPath, namespace);
                opts.setLocalSync(path);
                opts.setDeployment(node.resourceName());
                opts.setContainer(selectedContainer.get());
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
                NhctlProfileGetOptions opts = new NhctlProfileGetOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                opts.setContainer(selectedContainer.get());
                opts.setKey("image");
                String image = nhctlCommand.profileGet(node.applicationName(), opts);
                if (StringUtils.isNotEmpty(image)) {
                    startDevelop();
                    return;
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
                        opts.setContainer(selectedContainer.get());
                        opts.setKey("image");
                        opts.setValue(imageChooseDialog.getSelectedImage());
                        outputCapturedNhctlCommand.profileSet(node.applicationName(), opts);
                        selectedImage.set(imageChooseDialog.getSelectedImage());
                        startDevelop();
                    } catch (Exception e) {
                        ErrorUtil.dealWith(project, "Setting dev image",
                                "Error occurs while setting dev image", e);
                    }
                });
            }
        });
    }

    private DevModeService makeDevModeService(String openProjectPath) {
        openProjectPath = Paths.get(openProjectPath).toString();
        if (node.getClusterNode().getNocalhostAccount() != null) {
            return DevModeService.builder()
                    .server(node.getClusterNode().getNocalhostAccount().getServer())
                    .username(node.getClusterNode().getNocalhostAccount().getUsername())
                    .clusterId(node.getClusterNode().getServiceAccount().getClusterId())
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getNamespace())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .serviceType(node.getKubeResource().getKind())
                    .containerName(selectedContainer.get())
                    .image(selectedImage.get())
                    .projectPath(openProjectPath)
                    .action(action)
                    .mode(mode)
                    .build();
        } else {
            return DevModeService.builder()
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getNamespace())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .serviceType(node.getKubeResource().getKind())
                    .containerName(selectedContainer.get())
                    .image(selectedImage.get())
                    .projectPath(openProjectPath)
                    .action(action)
                    .mode(mode)
                    .build();
        }
    }

    private void startDevelop() {
        DevModeService devModeService = makeDevModeService(projectPathReference.get());
        ApplicationManager.getApplication().invokeLater(() -> {
            if (PathsUtil.isSame(projectPathReference.get(), project.getBasePath())) {
                ProgressManager.getInstance().run(
                        new StartingDevModeTask(project, devModeService));
            } else {
                Project[] openProjects = ProjectManagerEx.getInstanceEx().getOpenProjects();
                for (Project openProject : openProjects) {
                    if (PathsUtil.isSame(projectPathReference.get(), openProject.getBasePath())) {
                        ToolWindow toolWindow = ToolWindowManager.getInstance(openProject)
                                .getToolWindow(ToolWindowId.PROJECT_VIEW);
                        if (toolWindow != null) {
                            toolWindow.activate(() -> {
                                ProgressManager.getInstance().run(
                                        new StartingDevModeTask(openProject, devModeService));
                            });
                            return;
                        }
                    }
                }

                nocalhostSettings.setDevModeServiceToProjectPath(devModeService);

                var task = new OpenProjectTask();
                RecentProjectsManagerBase.getInstanceEx().openProject(Paths.get(projectPathReference.get()), task.withRunConfigurators());
            }
        });
    }
}
