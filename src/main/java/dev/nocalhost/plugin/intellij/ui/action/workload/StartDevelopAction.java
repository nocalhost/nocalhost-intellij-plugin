package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.commands.GitCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedGitCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
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

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        final String kubeconfigPath = KubeConfigUtil.kubeConfigPath(node.devSpace()).toString();
        DevModeService devModeService = new DevModeService(node.devSpace().getId(), node.devSpace().getDevSpaceId(), node.resourceName());

        NhctlDescribeOptions opts = new NhctlDescribeOptions();
        opts.setDeployment(node.resourceName());
        opts.setKubeconfig(kubeconfigPath);
        NhctlDescribeService nhctlDescribeService;
        try {
            nhctlDescribeService = nhctlCommand.describe(
                    node.devSpace().getContext().getApplicationName(),
                    opts,
                    NhctlDescribeService.class);
            if (nhctlDescribeService.isDeveloping()) {
                Messages.showMessageDialog("Dev mode has been started.", "Start develop", null);
                return;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while checking if service was in development", e);
            return;
        }

        String path = project.getBasePath();
        final GitCommand gitCommand = ServiceManager.getService(GitCommand.class);
        final String gitUrl = nhctlDescribeService.getRawConfig().getGitUrl();
        try {
            String ps = gitCommand.remote(path);
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
                final List<Path> chosenFiles = Lists.newArrayList();

                final FileChooserDescriptor gitSourceDirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                gitSourceDirChooser.setShowFileSystemRoots(true);
                FileChooser.chooseFiles(gitSourceDirChooser, null, null, paths -> {
                    paths.forEach((p) -> chosenFiles.add(p.toNioPath()));
                });

                if (chosenFiles.size() <= 0) {
                    return;
                }

                Path parentDir = chosenFiles.get(0);
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
                            final OutputCapturedGitCommand outputCapturedGitCommand = project.getService(OutputCapturedGitCommand.class);
                            outputCapturedGitCommand.clone(parentDir, gitUrl, node.resourceName());

                            gitDir = parentDir.resolve(node.resourceName());

                            nocalhostSettings.getDevModeProjectBasePath2Service().put(
                                    gitDir.toString(),
                                    devModeService
                            );
                        } catch (IOException | InterruptedException e) {
                            LOG.error("error occurred while cloning git repository", e);
                        }
                    }
                });
            }

            break;
            case Messages.NO: {
                final List<Path> chosenFiles = Lists.newArrayList();

                final FileChooserDescriptor sourceDirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                sourceDirChooser.setShowFileSystemRoots(true);
                FileChooser.chooseFiles(sourceDirChooser, null, null, paths -> {
                    paths.forEach((p) -> chosenFiles.add(p.toNioPath()));


                });

                if (chosenFiles.size() <= 0) {
                    return;
                }

                Path basePath = chosenFiles.get(0);
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
