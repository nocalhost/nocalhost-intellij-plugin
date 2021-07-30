package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
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

public class EndDevelopAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(EndDevelopAction.class);

    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
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
        this.namespace = node.getNamespaceNode().getNamespace();
        outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (!nhctlDescribeService.isDeveloping()) {
                        Messages.showMessageDialog("Dev mode has been ended.", "End Develop", null);
                        return;
                    }

                    if (nhctlDescribeService.isPossess()) {
                        endDevelop();
                    } else {
                        if (MessageDialogBuilder.yesNo(
                                "End Develop",
                                "You are not the dev possessor of this service, are you sure to exit the DevMode?"
                        ).ask(project)) {
                            endDevelop();
                        }
                    }
                });
            } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                if (e instanceof NocalhostExecuteCmdException) {
                    return;
                }
                LOG.error("error occurred while checking if service was in development", e);
            }
        });
    }

    private void endDevelop() {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Ending develop", false) {
            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC).action();

                NocalhostNotifier.getInstance(project).notifySuccess("DevMode ended", "");
            }

            @Override
            public void onThrowable(@NotNull Throwable e) {
                if (e instanceof NocalhostExecuteCmdException) {
                    return;
                }
                LOG.error("error occurred while ending develop", e);
            }

            @SneakyThrows
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                NhctlDevEndOptions opts = new NhctlDevEndOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setControllerType(node.getKubeResource().getKind());

                outputCapturedNhctlCommand.devEnd(node.applicationName(), opts);
            }
        });
    }
}
