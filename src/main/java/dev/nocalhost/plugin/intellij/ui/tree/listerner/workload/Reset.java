package dev.nocalhost.plugin.intellij.ui.tree.listerner.workload;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlResetOptions;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class Reset implements ActionListener {
    private final ResourceNode node;
    private final Project project;

    public Reset(ResourceNode node, Project project) {
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final String workloadName = node.getKubeResource().getMetadata().getName();
        final DevSpace devSpace = ((DevSpaceNode) node.getParent().getParent().getParent()).getDevSpace();


        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Resetting " + workloadName, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                NhctlResetOptions opts = new NhctlResetOptions();
                opts.setDeployment(workloadName);
                opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());

                try {
                    nhctlCommand.reset(devSpace.getContext().getApplicationName(), opts);
                    Notifications.Bus.notify(new Notification("Nocalhost.Notification", workloadName + " reset complete", "", NotificationType.INFORMATION), project);

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        });
    }
}
