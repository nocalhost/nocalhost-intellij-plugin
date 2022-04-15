package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.ide.BrowserUtil;
import com.google.gson.reflect.TypeToken;
import com.intellij.ide.RecentProjectsManagerBase;
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
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedGitCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlProfileGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlProfileSetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlRawConfig;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
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
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import dev.nocalhost.plugin.intellij.utils.PathsUtil;
import icons.NocalhostIcons;

import static dev.nocalhost.plugin.intellij.utils.Constants.DEV_MODE_DUPLICATE;

public class StartDevelopAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
    private final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(NocalhostSettings.class);

    private final OutputCapturedGitCommand outputCapturedGitCommand;
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final String mode;
    private final boolean mesh;
    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private final AtomicReference<String> projectPathReference = new AtomicReference<>();
    private final AtomicReference<String> selectedContainer = new AtomicReference<>();
    private final AtomicReference<String> selectedImage = new AtomicReference<>();

    private final String action;

    private String header = "";

    public StartDevelopAction(Project project, ResourceNode node) {
        this(
                "Start DevMode",
                project,
                node,
                NocalhostIcons.Status.DevStart,
                "",
                "",
                false
        );
    }

    public static StartDevelopAction duplicate(Project project, ResourceNode node) {
        return new StartDevelopAction(
                "Start DevMode (Duplicate)",
                project,
                node,
                NocalhostIcons.Status.DevCopy,
                DEV_MODE_DUPLICATE,
                "",
                false
        );
    }

    public static StartDevelopAction duplicateMesh(Project project, ResourceNode node) {
        return new StartDevelopAction(
                "Start Mesh (Duplicate)",
                project,
                node,
                NocalhostIcons.Status.DevCopy,
                DEV_MODE_DUPLICATE,
                "",
                true
        );
    }

    public StartDevelopAction(String title, Project project, ResourceNode node, Icon icon, String mode, String action, boolean mesh) {
        super(title, "", icon);
        this.node = node;
        this.mode = mode;
        this.mesh = mesh;
        this.action = action;
        this.project = project;
        this.kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
        outputCapturedGitCommand = project.getService(OutputCapturedGitCommand.class);
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDevStartOptions devStartOptions = new NhctlDevStartOptions(kubeConfigPath, namespace);
                devStartOptions.setControllerType(node.controllerType());
                devStartOptions.setAuthCheck(true);
                nhctlCommand.devStart(node.applicationName(), devStartOptions);

                var desService = NhctlUtil.getDescribeService(project, node.resourceName(), node.controllerType(), namespace, node.applicationName(), kubeConfigPath);
                if (NhctlDescribeServiceUtil.developStarted(desService)) {
                    // remote run | remote debug
                    if (StringUtils.isNotEmpty(action)) {
                        if (PathsUtil.isSame(project.getBasePath(), desService.getAssociate())) {
                            ProgressManager
                                    .getInstance()
                                    .run(new ExecutionTask(project, makeDevModeService(project.getBasePath()), action));
                            return;
                        }
                        // https://nocalhost.coding.net/p/nocalhost/assignments/issues/624/detail
                        startDevelop(desService.getAssociate());
                        return;
                    }
                    // the service in the `duplicate` mode currently
                    if (StringUtils.equals(desService.getDevModeType(), DEV_MODE_DUPLICATE)) {
                        ApplicationManager.getApplication().invokeLater(() ->
                                Messages.showMessageDialog(
                                        "Dev mode has been started.",
                                        "Start DevMode",
                                        null
                                )
                        );
                        return;
                    }
                    // the service in the `replace` mode currently
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
        try {
            var cmd = new NhctlDevContainerListCommand(project);
            cmd.setNamespace(namespace);
            cmd.setKubeConfig(kubeConfigPath);
            cmd.setDeployment(node.resourceName());
            cmd.setApplication(node.applicationName());
            cmd.setControllerType(node.controllerType());
            List<String> containers = DataUtils.GSON.fromJson(cmd.execute(), TypeToken.getParameterized(List.class, String.class).getType());
            if (containers.size() > 1) {
                selectContainer(containers);
            } else if (containers.size() == 1) {
                selectedContainer.set(containers.get(0));
                getAssociate();
            }
        } catch (Exception ex) {
            ErrorUtil.dealWith(project, "Failed to get containers", "Error occurs while get containers", ex);
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
                opts.setControllerType(node.controllerType());
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
                var opts = new NhctlConfigOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setControllerType(node.controllerType());

                var devConfig = ApplicationManager
                        .getApplication()
                        .getService(NhctlCommand.class)
                        .getConfig(node.applicationName(), opts, NhctlRawConfig.class);

                var container = devConfig.getContainers()
                        .stream()
                        .filter(x -> StringUtils.equals(x.getName(), selectedContainer.get()))
                        .findFirst();
                if (container.isPresent()) {
                    ServiceContainer serviceContainer = container.get();
                    if (serviceContainer.getDev() != null
                            && StringUtils.isNotEmpty(serviceContainer.getDev().getGitUrl())) {
                        cloneGitRepository(serviceContainer.getDev().getGitUrl());
                        return;
                    }
                }

                if (devConfig.getContainers().size() == 1
                        && StringUtils.equals(devConfig.getContainers().get(0).getName(), "")) {
                    ServiceContainer serviceContainer = devConfig.getContainers().get(0);
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
                                opts.setType(node.controllerType());
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
                opts.setControllerType(node.controllerType());
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
                opts.setType(node.controllerType());
                opts.setContainer(selectedContainer.get());
                opts.setKey("image");
                String image = nhctlCommand.profileGet(node.applicationName(), opts);
                if (StringUtils.isNotEmpty(image)) {
                    if (mesh) {
                        getHeader();
                        return;
                    }
                    startDevelop(projectPathReference.get());
                    return;
                }
                selectImage();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading dev image",
                        "Error occurs while loading dev image", e);
            }
        });
    }

    private void getHeader() {
        ApplicationManager.getApplication().invokeLater(() -> {
            header = Messages.showInputDialog(
                    project,
                    "Please input header, eg: foo=bar",
                    "Start DevMode",
                    null
            );
            if (StringUtils.isNotEmpty(header)) {
                startDevelop(projectPathReference.get());
            }
        });
    }

    private void selectImage() {
        ApplicationManager.getApplication().invokeLater(() -> {
            var yes = MessageDialogBuilder
                    .yesNo(
                            "Start DevMode",
                            "There is no development configuration for container `" + selectedContainer.get() + "`, please select an operation."
                    )
                    .yesText("Still enter development mode")
                    .noText("Set development configuration with form")
                    .ask(project);
            if ( ! yes) {
                openDevConfigTools();
                return;
            }
            ImageChooseDialog imageChooseDialog = new ImageChooseDialog(project);
            if (imageChooseDialog.showAndGet()) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        NhctlProfileSetOptions opts = new NhctlProfileSetOptions(kubeConfigPath,
                                namespace);
                        opts.setDeployment(node.resourceName());
                        opts.setType(node.controllerType());
                        opts.setContainer(selectedContainer.get());
                        opts.setKey("image");
                        opts.setValue(imageChooseDialog.getSelectedImage());
                        outputCapturedNhctlCommand.profileSet(node.applicationName(), opts);
                        selectedImage.set(imageChooseDialog.getSelectedImage());
                        if (mesh) {
                            getHeader();
                            return;
                        }
                        startDevelop(projectPathReference.get());
                    } catch (Exception e) {
                        ErrorUtil.dealWith(project, "Setting dev image",
                                "Error occurs while setting dev image", e);
                    }
                });
            }
        });
    }

    private DevModeService makeDevModeService(String openProjectPath) {
        if (node.getClusterNode().getNocalhostAccount() != null) {
            return DevModeService.builder()
                    .server(node.getClusterNode().getNocalhostAccount().getServer())
                    .username(node.getClusterNode().getNocalhostAccount().getUsername())
                    .clusterId(node.getClusterNode().getServiceAccount().getClusterId())
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getNamespace())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .serviceType(node.controllerType())
                    .containerName(selectedContainer.get())
                    .image(selectedImage.get())
                    .projectPath(Paths.get(openProjectPath).toString())
                    .action(action)
                    .header(header)
                    .mode(mode)
                    .build();
        }

        return DevModeService.builder()
             .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
             .namespace(node.getNamespaceNode().getNamespace())
             .applicationName(node.applicationName())
             .serviceName(node.resourceName())
             .serviceType(node.controllerType())
             .containerName(selectedContainer.get())
             .image(selectedImage.get())
             .projectPath(Paths.get(openProjectPath).toString())
             .action(action)
             .header(header)
             .mode(mode)
             .build();
    }

    private void startDevelop(@NotNull String path) {
        var devModeService = makeDevModeService(path);
        ApplicationManager.getApplication().invokeLater(() -> {
            if ( ! PathsUtil.isExists(path)) {
                NocalhostNotifier
                        .getInstance(project)
                        .notifyError("Failed to start dev", "The associated directory does not exist: [" + path + "]");
                return;
            }
            if (PathsUtil.isSame(path, project.getBasePath())) {
                ProgressManager
                        .getInstance()
                        .run(new StartingDevModeTask(project, devModeService));
                return;
            }

            var projects = ProjectManagerEx.getInstanceEx().getOpenProjects();
            for (var op : projects) {
                if (PathsUtil.isSame(path, op.getBasePath())) {
                    ToolWindow toolWindow = ToolWindowManager.getInstance(op)
                                                             .getToolWindow(ToolWindowId.PROJECT_VIEW);
                    if (toolWindow != null) {
                        toolWindow.activate(() -> {
                            ProgressManager.getInstance().run(new StartingDevModeTask(op, devModeService));
                        });
                        return;
                    }
                }
            }

            nocalhostSettings.setDevModeServiceToProjectPath(devModeService);

            var task = new OpenProjectTask();
            RecentProjectsManagerBase.getInstanceEx().openProject(Paths.get(path), task.withRunConfigurators());
        });
    }

    private void openDevConfigTools() {
        try {
            var x = new URIBuilder("https://nocalhost.dev/tools");
            x.addParameter("from", "daemon");
            x.addParameter("name", node.resourceName());
            x.addParameter("type", node.controllerType());
            x.addParameter("namespace", namespace);
            x.addParameter("container", selectedContainer.get());
            x.addParameter("kubeconfig", kubeConfigPath.toString());
            x.addParameter("application", node.applicationName());
            BrowserUtil.browse(x.build().toString());
        } catch (Exception ex) {
            ErrorUtil.dealWith(project, "Failed to open browser",
                    "Error occurred while opening browser", ex);
        }
    }
}
