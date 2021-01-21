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
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevEndOptions;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class EndDevelop implements ActionListener {
    private final ResourceNode node;
    private final Project project;

    public EndDevelop(ResourceNode node, Project project) {
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Ending DevMode", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
                final String workloadName = node.getKubeResource().getMetadata().getName();
                final DevSpace devSpace = ((DevSpaceNode) node.getParent().getParent().getParent()).getDevSpace();


                NhctlDevEndOptions opts = new NhctlDevEndOptions();
                opts.setDeployment(workloadName);
                opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());

                try {
                    nhctlCommand.devEnd(devSpace.getContext().getApplicationName(), opts);

                    Notifications.Bus.notify(new Notification("Nocalhost.Notification", "DevMode ended", "", NotificationType.INFORMATION), project);

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                }
            }
        });
    }
}
