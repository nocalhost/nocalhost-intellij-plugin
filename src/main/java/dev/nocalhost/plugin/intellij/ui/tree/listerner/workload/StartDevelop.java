package dev.nocalhost.plugin.intellij.ui.tree.listerner.workload;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.commands.GitCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.MessageUtil;

public class StartDevelop implements ActionListener {
    private static final Logger LOG = Logger.getInstance(StartDevelop.class);

    private final ResourceNode node;

    public StartDevelop(ResourceNode node) {
        this.node = node;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        final String kubeconfigPath = KubeConfigUtil.kubeConfigPath(node.devSpace()).toString();
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
                MessageUtil.showMessageDialog("Dev mode has been started.");
                return;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return;
        }

        int exitCode = MessageDialogBuilder.yesNoCancel("To start develop, you must specify source code directory.", "")
                .yesText("Clone from Git Repo")
                .noText("Open local directly")
                .guessWindowAndAsk();
        switch (exitCode) {
            case Messages.YES:
                final FileChooserDescriptor gitSourceDirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                gitSourceDirChooser.setShowFileSystemRoots(true);
                FileChooser.chooseFiles(gitSourceDirChooser, null, null, paths -> {
                    Path parentDir = paths.get(0).toNioPath();

                    final GitCommand gitCommand = ServiceManager.getService(GitCommand.class);
                    final String gitUrl = nhctlDescribeService.getRawConfig().getGitUrl();

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
                                gitCommand.clone(parentDir, gitUrl, node.resourceName());

                                gitDir = parentDir.resolve(node.resourceName());

                                final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

                                nocalhostSettings.getDevModeProjectBasePath2Service().put(
                                        gitDir.toString(),
                                        new DevModeService(node.devSpace().getId(), node.devSpace().getDevSpaceId(), node.resourceName())
                                );
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                });

                break;
            case Messages.NO:
                final FileChooserDescriptor sourceDirChooser = FileChooserDescriptorFactory.createSingleFolderDescriptor();
                sourceDirChooser.setShowFileSystemRoots(true);
                FileChooser.chooseFiles(sourceDirChooser, null, null, paths -> {
                    Path bashPath = paths.get(0).toNioPath();

                    final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

                    nocalhostSettings.getDevModeProjectBasePath2Service().put(
                            bashPath.toString(),
                            new DevModeService(node.devSpace().getId(), node.devSpace().getDevSpaceId(), node.resourceName())
                    );

                    ProjectManagerEx.getInstanceEx().openProject(bashPath, new OpenProjectTask());
                });
                break;
            default:
        }
    }
}
