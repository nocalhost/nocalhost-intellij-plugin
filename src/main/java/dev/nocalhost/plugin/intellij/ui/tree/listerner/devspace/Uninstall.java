package dev.nocalhost.plugin.intellij.ui.tree.listerner.devspace;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.MessageDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlUninstallOptions;
import dev.nocalhost.plugin.intellij.helpers.NhctlHelper;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.MessageUtil;

public class Uninstall implements ActionListener {

    private final DevSpaceNode node;

    public Uninstall(DevSpaceNode node) {
        this.node = node;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final DevSpace devSpace = node.getDevSpace();

        try {
            if (!NhctlHelper.isApplicationInstalled(devSpace)) {
                MessageUtil.showMessageDialog("Application has not been installed.");
                return;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return;
        }

        final String appName = devSpace.getContext().getApplicationName();
        if (!MessageDialogBuilder.yesNo("Uninstall application: " + appName, "").guessWindowAndAsk()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Uninstalling application: " + appName, false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

                NhctlUninstallOptions opts = new NhctlUninstallOptions();
                opts.setForce(true);
                opts.setKubeconfig(KubeConfigUtil.kubeConfigPath(devSpace).toString());
                try {
                    nhctlCommand.uninstall(appName, opts);

                    final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
                    nocalhostApi.syncInstallStatus(devSpace, 0);

                    final Application application = ApplicationManager.getApplication();
                    DevSpaceListUpdatedNotifier publisher = application.getMessageBus()
                            .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC);
                    publisher.action();

                    Notifications.Bus.notify(new Notification("Nocalhost.Notification", "Application " + appName + " uninstalled", "", NotificationType.INFORMATION));
                } catch (InterruptedException interruptedException) {
                    interruptedException.printStackTrace();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
    }
}
