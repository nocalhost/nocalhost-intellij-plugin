package dev.nocalhost.plugin.intellij.ui.action.devspace;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeApplication;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ApplyAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ApplyAction.class);

    private final Project project;
    private final DevSpaceNode node;

    public ApplyAction(Project project, DevSpaceNode node) {
        super("Apply");
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions();
        nhctlDescribeOptions.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.getDevSpace()).toString());
        try {
            NhctlDescribeApplication nhctlDescribeApplication = nhctlCommand.describe(
                    node.getDevSpace().getContext().getApplicationName(),
                    nhctlDescribeOptions,
                    NhctlDescribeApplication.class
            );
            if (nhctlDescribeApplication.getSvcProfile().stream().anyMatch(NhctlDescribeService::isDeveloping)) {
                Messages.showErrorDialog("Cannot apply to a developing deployment, please exit the dev mode before apply.", "Apply Kubernetes Configuration");
                return;
            }
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            LOG.error("error occurred while describing application", e);
            return;
        }

        final List<Path> chosenPaths = FileChooseUtil.chooseFilesAndDirectories(project);
        if (chosenPaths.size() <= 0) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Modal(project, "Applying Kubernetes Configuration", false) {
            private final List<String> errorMessages = Lists.newArrayList();

            @Override
            public void onFinished() {
                if (errorMessages.size() > 0) {
                    Messages.showErrorDialog(String.join(", ", errorMessages), "Errors while applying kubernetes configuration");
                }
            }

            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                final double perFraction = 1.0 / chosenPaths.size();
                final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);

                double fraction = 0.0;
                progressIndicator.setIndeterminate(false);
                progressIndicator.setFraction(fraction);

                for (Path path : chosenPaths) {
                    progressIndicator.setText("Applying " + path.toString());

                    try {
                        kubectlCommand.apply(path, node.getDevSpace());
                    } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                        errorMessages.add(e.getMessage());
                    }

                    fraction += perFraction;
                    progressIndicator.setFraction(fraction);
                }
            }
        });
    }
}
