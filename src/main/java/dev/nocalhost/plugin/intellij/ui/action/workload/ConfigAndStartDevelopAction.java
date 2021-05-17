package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import dev.nocalhost.plugin.intellij.ui.dialog.DevImageChooseDialog;
import dev.nocalhost.plugin.intellij.ui.dialog.StartDevelopContainerChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import icons.NocalhostIcons;

public class ConfigAndStartDevelopAction extends AnAction {
    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
    private final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    private String selectedDirectory;
    private String selectedContainer;
    private String selectedImage;

    public ConfigAndStartDevelopAction(Project project, ResourceNode node) {
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
                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);
                if (nhctlDescribeService.isDeveloping()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showMessageDialog("Dev mode has been started.",
                                "Start Develop", null);
                    });
                    return;
                }
                if (StringUtils.isNotEmpty(nhctlDescribeService.getAssociate())) {
                    selectedDirectory = nhctlDescribeService.getAssociate();
                    checkContainer();
                } else {
                    selectAssociateDirectory();
                }
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Checking associate directory error",
                        "Error occurs while checking associate directory", e);
            }
        });
    }

    private void selectAssociateDirectory() {
        ApplicationManager.getApplication().invokeLater(() -> {
            Path dir = FileChooseUtil.chooseSingleDirectory(project,
                    "Choose Source Code Directory",
                    "To start develop, you must specify source code directory.");
            if (dir == null) {
                return;
            }
            selectedDirectory = dir.toAbsolutePath().toString();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(kubeConfigPath,
                            namespace);
                    opts.setAssociate(dir.toAbsolutePath().toString());
                    opts.setDeployment(node.resourceName());
                    opts.setControllerType(node.getKubeResource().getKind());
                    nhctlCommand.devAssociate(node.applicationName(), opts);

                    checkContainer();
                } catch (Exception e) {
                    ErrorUtil.dealWith(project, "Setting associate directory error",
                            "Error occurs while setting associate directory", e);
                }
            });
        });
    }

    private void checkContainer() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                KubeResource workload = kubectlCommand.getResource(
                        node.getKubeResource().getKind(),
                        node.resourceName(),
                        kubeConfigPath,
                        namespace);
                KubeResourceList podList = kubectlCommand.getResourceList(
                        "pods",
                        workload.getSpec().getSelector().getMatchLabels(),
                        kubeConfigPath,
                        namespace);
                List<KubeResource> pods = podList.getItems().stream()
                        .filter(KubeResource::canSelector)
                        .collect(Collectors.toList());
                List<String> containers = pods
                        .get(0)
                        .getSpec()
                        .getContainers()
                        .stream()
                        .map(KubeResource.Spec.Container::getName)
                        .collect(Collectors.toList());

                selectContainer(containers);
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Checking container error",
                        "Error occurs while checking container directory", e);
            }
        });
    }

    private void selectContainer(List<String> containers) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (containers.size() > 1) {
                StartDevelopContainerChooseDialog dialog = new StartDevelopContainerChooseDialog(containers);
                if (dialog.showAndGet()) {
                    selectedContainer = dialog.getSelectedContainer();
                } else {
                    return;
                }
            } else {
                selectedContainer = containers.get(0);
            }
            checkImage();
        });
    }

    private void checkImage() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);
                String image = findImage(nhctlDescribeService);
                if (StringUtils.isNotEmpty(image)) {
                    startDevelop();
                } else {
                    selectImage();
                }
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Checking dev image error",
                        "Error occurs while setting associate directory", e);
            }
        });
    }

    private String findImage(NhctlDescribeService nhctlDescribeService) {
        Optional<ServiceContainer> serviceContainerOptional = nhctlDescribeService.getRawConfig()
                .getContainers()
                .stream()
                .filter(e -> StringUtils.equals(e.getName(), selectedContainer))
                .findFirst();
        if (serviceContainerOptional.isPresent()) {
            return serviceContainerOptional.get().getDev().getImage();
        } else {
            if (nhctlDescribeService.getRawConfig().getContainers().size() != 1) {
                return "";
            }
            return nhctlDescribeService.getRawConfig().getContainers().get(0).getDev().getImage();
        }
    }

    private void selectImage() {
        ApplicationManager.getApplication().invokeLater(() -> {
            DevImageChooseDialog devImageChooseDialog = new DevImageChooseDialog(project);
            if (devImageChooseDialog.showAndGet()) {
                selectedImage = devImageChooseDialog.getSelectedImage();
                setImage();
            } else {
                return;
            }
        });
    }

    private void setImage() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlProfileSetOptions opts = new NhctlProfileSetOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                opts.setContainer(selectedContainer);
                opts.setKey("image");
                opts.setValue(selectedImage);
                nhctlCommand.profileSet(node.applicationName(), opts);
                startDevelop();
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Setting dev image error",
                        "Error occurs while setting dev image", e);
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
                    .containerName(selectedContainer)
                    .build();
        } else {
            serviceProjectPath = ServiceProjectPath.builder()
                    .rawKubeConfig(node.getClusterNode().getRawKubeConfig())
                    .namespace(node.getNamespaceNode().getName())
                    .applicationName(node.applicationName())
                    .serviceName(node.resourceName())
                    .containerName(selectedContainer)
                    .build();
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            if (StringUtils.equals(project.getBasePath(), selectedDirectory)) {
                ProgressManager.getInstance().run(new StartingDevModeTask(project,
                        serviceProjectPath));
            } else {
                nocalhostSettings.setDevModeServiceToProjectPath(serviceProjectPath);
                ProjectManagerEx.getInstanceEx().openProject(Path.of(selectedDirectory),
                        new OpenProjectTask());
            }
        });

    }
}
