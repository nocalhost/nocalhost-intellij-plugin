package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevEndOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import icons.NocalhostIcons;
import lombok.SneakyThrows;

public class EndDevelopAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(EndDevelopAction.class);

    private final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
    private final OutputCapturedNhctlCommand outputCapturedNhctlCommand;

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public EndDevelopAction(Project project, ResourceNode node) {
        super("End Develop", "", NocalhostIcons.Status.DevEnd);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getName();
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
            opts.setDeployment(node.resourceName());
            NhctlDescribeService nhctlDescribeService;
            try {
                nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(),
                        opts,
                        NhctlDescribeService.class);
                if (!nhctlDescribeService.isDeveloping()) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Messages.showMessageDialog("Dev mode has been ended.", "End develop", null);
                    });
                    return;
                }

                ApplicationManager.getApplication().invokeLater(this::endDevelop);
            } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                LOG.error("error occurred while checking if service was in development", e);
                return;
            }
        });
    }

    private void endDevelop() {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Ending DevMode", false) {
            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

                NocalhostNotifier.getInstance(project).notifySuccess("DevMode ended", "");
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                LOG.error("error occurred while ending develop", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                NhctlDevEndOptions opts = new NhctlDevEndOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());

                outputCapturedNhctlCommand.devEnd(node.applicationName(), opts);
            }
        });
    }
}
