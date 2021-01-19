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

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevEndOptions;
import dev.nocalhost.plugin.intellij.ui.tree.WorkloadNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class EndDevelop implements ActionListener {
    private final WorkloadNode node;
    private final Project project;

    public EndDevelop(WorkloadNode node, Project project) {
        this.node = node;
        this.project = project;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Ending DevMode", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                NhctlDevEndOptions opts = new NhctlDevEndOptions();
                opts.setDeployment(node.getName());
                opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(node.getDevSpace()).toString());

                try {
                    nhctlCommand.devEnd(node.getDevSpace().getContext().getApplicationName(), opts);

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
