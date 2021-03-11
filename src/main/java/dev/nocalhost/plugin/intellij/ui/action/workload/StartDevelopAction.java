package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
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

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.commands.GitCommand;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostGitException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostRepo;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;
import dev.nocalhost.plugin.intellij.ui.StartDevelopContainerChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import icons.NocalhostIcons;

public class StartDevelopAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(StartDevelopAction.class);

    private final Project project;
    private final ResourceNode node;

    public StartDevelopAction(Project project, ResourceNode node) {
        super("Start Develop", "", NocalhostIcons.Status.DevStart);
        this.project = project;
        this.node = node;
    }

    private String selectContainer(List<String> containers) {
        if (containers.size() > 1) {
            StartDevelopContainerChooseDialog dialog = new StartDevelopContainerChooseDialog(containers);
            if (dialog.showAndGet()) {
                return dialog.getSelectedContainer();
            } else {
                return null;
            }
        } else {
            return containers.get(0);
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

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        NhctlDescribeService nhctlDescribeService;
        String startDevelopContainerName = "";
        try {
            final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
            NhctlDescribeOptions opts = new NhctlDescribeOptions();
            opts.setDeployment(node.resourceName());
            opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());
            nhctlDescribeService = nhctlCommand.describe(
                    node.devSpace().getContext().getApplicationName(),
                    opts,
                    NhctlDescribeService.class);
            if (nhctlDescribeService.isDeveloping()) {
                Messages.showMessageDialog("Dev mode has been started.", "Start develop", null);
                return;
            }

            final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
            KubeResource deployment = kubectlCommand.getResource("deployment", node.resourceName(), node.devSpace());
            KubeResourceList pods = kubectlCommand.getResourceList("pods", deployment.getSpec().getSelector().getMatchLabels(), node.devSpace());
            if (pods.getItems().get(0).getSpec().getContainers().size() > 1) {
                startDevelopContainerName = selectContainer(pods
                        .getItems()
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
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            LOG.error("error occurred while checking if service was in development", e);
            return;
        }

        final String containerName = startDevelopContainerName;

        final String gitUrl = findGitUrl(nhctlDescribeService.getRawConfig().getContainers(), containerName);

        DevModeService devModeService = new DevModeService(node.devSpace().getId(), node.devSpace().getDevSpaceId(), node.resourceName(), containerName);

        final String path = project.getBasePath();

        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        final Optional<NocalhostRepo> nocalhostRepo = nocalhostSettings.getRepos().stream()
                                                                       .filter(repos -> repos.getHost().equals(nocalhostSettings.getBaseUrl())
                                                                                && repos.getEmail().equals(nocalhostSettings.getUserInfo().getEmail())
                                                                                && repos.getAppName().equals(node.devSpace().getContext().getApplicationName())
                                                                                && repos.getDeploymentName().equals(devModeService.getServiceName())).findFirst();

        if (nocalhostRepo.isPresent()) {
            final NocalhostRepo repo = nocalhostRepo.get();
            if (repo.getRepoPath().equals(project.getBasePath())) {
                ProgressManager.getInstance().run(new StartingDevModeTask(project, node.devSpace(), devModeService));
            } else {
                nocalhostSettings.getDevModeProjectBasePath2Service().put(
                        repo.getRepoPath(),
                        devModeService
                );

                ProjectManagerEx.getInstanceEx().openProject(Path.of(repo.getRepoPath()), new OpenProjectTask());
            }
            return;
        }

        final GitCommand gitCommand = ServiceManager.getService(GitCommand.class);

        try {
            String ps = gitCommand.getRemote(path, project);
            final Optional<String> optionalPath = Arrays.stream(ps.split("\n")).map(p -> p.split("\t")[1].split(" ")[0]).filter(p -> p.equals(gitUrl)).findFirst();
            if (optionalPath.isPresent()) {
                ProgressManager.getInstance().run(new StartingDevModeTask(project, node.devSpace(), devModeService));
                return;
            }
        } catch (Exception ignored) {
        }

        int exitCode = MessageDialogBuilder.yesNoCancel("Start develop", "To start develop, you must specify source code directory.")
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

                ProgressManager.getInstance().run(new Task.Backgroundable(null, "Cloning " + gitUrl, false) {
                    private Path gitDir;

                    @Override
                    public void onSuccess() {
                        super.onSuccess();
                        ProjectManagerEx.getInstanceEx().openProject(gitDir, new OpenProjectTask());
                    }

                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        try {
                            gitCommand.clone(parentDir, gitUrl, node.resourceName(), project);

                            gitDir = parentDir.resolve(node.resourceName());

                            nocalhostSettings.getDevModeProjectBasePath2Service().put(
                                    gitDir.toString(),
                                    devModeService
                            );
                        } catch (NocalhostGitException e) {
                            LOG.error("error occurred while cloning git repository", e);
                            NocalhostNotifier.getInstance(project).notifyError("Nocalhost clone repository error", "Error occurred while clone repository", e.getMessage());
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

                nocalhostSettings.getDevModeProjectBasePath2Service().put(
                        basePath.toString(),
                        devModeService
                );

                ProjectManagerEx.getInstanceEx().openProject(basePath, new OpenProjectTask());
            }
            break;
            default:
        }
    }
}
