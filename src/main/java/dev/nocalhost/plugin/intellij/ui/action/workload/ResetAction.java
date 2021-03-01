package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlResetOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class ResetAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ResetAction.class);

    private final Project project;
    private final ResourceNode node;

    public ResetAction(Project project, ResourceNode node) {
        super("Reset", "", AllIcons.General.Reset);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Resetting " + node.resourceName(), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                NhctlResetOptions opts = new NhctlResetOptions();
                opts.setDeployment(node.resourceName());
                opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());

                try {
                    nhctlCommand.reset(node.devSpace().getContext().getApplicationName(), opts);

                    NocalhostNotifier.getInstance(project).notifySuccess(node.resourceName() + " reset complete", "");
                } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                    LOG.error("error occurred while resetting workload", e);
                }
            }
        });
    }
}
