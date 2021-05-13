package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.google.common.collect.Lists;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
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
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostGitException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;
import dev.nocalhost.plugin.intellij.ui.dialog.StartDevelopContainerChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
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

    public StartDevelopAction(Project project, ResourceNode node) {
        super("Start Develop", "", NocalhostIcons.Status.DevStart);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getName();
    }

    private String selectContainer(List<String> containers) {
        StartDevelopContainerChooseDialog dialog = new StartDevelopContainerChooseDialog(containers);
        if (dialog.showAndGet()) {
            return dialog.getSelectedContainer();
        } else {
            return null;
        }
    }

    private String findGitUrl(List<ServiceContainer> containers, String containerName) {
        if (containers.size() == 1 && StringUtils.isBlank(containerName)) {
            return containers.get(0).getDev().getGitUrl();
        }
        for (ServiceContainer container : containers) {
            if (StringUtils.equals(container.getName(), containerName)) {
                return container.getDev().getGitUrl();
            }
        }
        return null;
    }

    private Optional<NhctlDescribeService> getNhctlDescribeService() {
        try {
            NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
            opts.setDeployment(node.resourceName());
            NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                    node.applicationName(), opts, NhctlDescribeService.class);
            return Optional.of(nhctlDescribeService);
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            LOG.error("error occurred while checking if service was in development", e);
        }
        return Optional.empty();
    }

    private List<KubeResource> getDeploymentPods() {
        try {
            KubeResource deployment = kubectlCommand.getResource("deployment",
                    node.resourceName(), kubeConfigPath, namespace);
            KubeResourceList pods = kubectlCommand.getResourceList("pods",
                    deployment.getSpec().getSelector().getMatchLabels(), kubeConfigPath, namespace);
            return pods.getItems().stream()
                    .filter(KubeResource::canSelector)
                    .collect(Collectors.toList());
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            LOG.error("error occurred while listing deployment pods", e);
        }
        return Lists.newArrayList();
    }

    private void selectSourceDirectory(final String gitUrl, final ServiceProjectPath serviceProjectPath) {
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
                case Messages.YES: {
                    if (!StringUtils.isNotEmpty(gitUrl)) {
                        Messages.showMessageDialog("Git url not found", "Clone Repo", null);
                        return;
                    }

                    final Path parentDir = FileChooseUtil.chooseSingleDirectory(project);
                    if (parentDir == null) {
                        return;
                    }

                    ProgressManager.getInstance().run(new Task.Backgroundable(null,
                            "Cloning " + gitUrl, false) {
                        private Path gitDir;

                        @Override
                        public void onSuccess() {
                            super.onSuccess();
                            ProjectManagerEx.getInstanceEx().openProject(gitDir,
                                    new OpenProjectTask());
                        }

                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            try {
                                gitCommand.clone(parentDir, gitUrl, node.resourceName(), project);

                                gitDir = parentDir.resolve(node.resourceName());

                                serviceProjectPath.setProjectPath(gitDir.toString());
                                nocalhostSettings.setDevModeServiceToProjectPath(serviceProjectPath);

                                associateProjectPath(gitDir.toString());
                            } catch (NocalhostGitException e) {
                                LOG.error("error occurred while cloning git repository", e);
                                NocalhostNotifier.getInstance(project).notifyError(
                                        "Nocalhost clone repository error",
                                        "Error occurred while clone repository",
                                        e.getMessage());
                            }
                        }
                    });
                }

                break;
                case Messages.NO: {
                    final Path basePath = FileChooseUtil.chooseSingleDirectory(project);
                    if (basePath == null) {
                        return;
                    }

                    serviceProjectPath.setProjectPath(basePath.toString());
                    nocalhostSettings.setDevModeServiceToProjectPath(serviceProjectPath);

                    associateProjectPath(basePath.toString());

                    ProjectManagerEx.getInstanceEx().openProject(basePath, new OpenProjectTask());
                }
                break;
                default:
            }
        });
    }

    private void associateProjectPath(String projectPath) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(kubeConfigPath, namespace);
                opts.setAssociate(projectPath);
                opts.setDeployment(node.resourceName());
                opts.setControllerType(node.getKubeResource().getKind());
                nhctlCommand.devAssociate(node.applicationName(), opts);
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Associating project path error",
                        "Error occurs while associating project path", e);
            }
        });
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            Optional<NhctlDescribeService> nhctlDescribeServiceOptional = getNhctlDescribeService();
            if (nhctlDescribeServiceOptional.isEmpty()) {
                return;
            }

            NhctlDescribeService nhctlDescribeService = nhctlDescribeServiceOptional.get();
            if (nhctlDescribeService.isDeveloping()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showMessageDialog("Dev mode has been started.",
                            "Start Develop", null);
                });
                return;
            }

            List<KubeResource> pods = getDeploymentPods();
            ApplicationManager.getApplication().invokeLater(() -> {
                String startDevelopContainerName = "";
                if (pods.size() > 0 && pods.get(0).getSpec().getContainers().size() > 1) {
                    startDevelopContainerName = selectContainer(pods
                            .get(0)
                            .getSpec()
                            .getContainers()
                            .stream()
                            .map(KubeResource.Spec.Container::getName)
                            .collect(Collectors.toList()));
                    if (!StringUtils.isNotEmpty(startDevelopContainerName)) {
                        return;
                    }
                }

                final String containerName = startDevelopContainerName;

                String gitUrl = findGitUrl(nhctlDescribeService.getRawConfig().getContainers(),
                        containerName);

                ServiceProjectPath serviceProjectPath;
                if (node.getClusterNode().getNocalhostAccount() != null) {
                    serviceProjectPath = ServiceProjectPath.builder()
                            .server(node.getClusterNode().getNocalhostAccount().getServer())
                            .username(node.getClusterNode().getNocalhostAccount().getUsername())
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

                String projectPath = nhctlDescribeService.getAssociate();

                if (StringUtils.isNotEmpty(projectPath)) {
                    if (StringUtils.equals(projectPath, project.getBasePath())) {
                        ProgressManager.getInstance().run(new StartingDevModeTask(project,
                                serviceProjectPath));
                    } else {
                        serviceProjectPath.setProjectPath(projectPath);
                        nocalhostSettings.setDevModeServiceToProjectPath(serviceProjectPath);

                        ProjectManagerEx.getInstanceEx().openProject(Path.of(projectPath),
                                new OpenProjectTask());
                    }
                    return;
                }

                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        String ps = gitCommand.getRemote(project.getBasePath(), project);
                        final Optional<String> optionalPath = Arrays.stream(ps.split("\n"))
                                .map(p -> p.split("\t")[1].split(" ")[0])
                                .filter(p -> p.equals(gitUrl)).findFirst();
                        if (optionalPath.isPresent()) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                ProgressManager.getInstance().run(new StartingDevModeTask(project,
                                        serviceProjectPath));
                            });
                            return;
                        }
                    } catch (Exception ignored) {
                    }

                    selectSourceDirectory(gitUrl, serviceProjectPath);
                });
            });
        });
    }
}
