package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
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

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevEndOptions;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import icons.NocalhostIcons;

public class EndDevelopAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(EndDevelopAction.class);

    private final Project project;
    private final ResourceNode node;

    public EndDevelopAction(Project project, ResourceNode node) {
        super("End Develop", "", NocalhostIcons.Status.DevEnd);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
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
            if (!nhctlDescribeService.isDeveloping()) {
                Messages.showMessageDialog("Dev mode has been ended.", "End develop", null);
                return;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while checking if service was in development", e);
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Ending DevMode", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                NhctlDevEndOptions opts = new NhctlDevEndOptions();
                opts.setDeployment(node.resourceName());
                opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.devSpace()).toString());

                try {
                    final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
                    outputCapturedNhctlCommand.devEnd(node.devSpace().getContext().getApplicationName(), opts);

                    ApplicationManager.getApplication().getMessageBus()
                            .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC)
                            .action();

                    Notifications.Bus.notify(new Notification("Nocalhost.Notification", "DevMode ended", "", NotificationType.INFORMATION), project);
                } catch (IOException | InterruptedException e) {
                    LOG.error("error occurred while ending develop", e);
                }
            }
        });
    }
}
