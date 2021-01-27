package dev.nocalhost.plugin.intellij.task;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncOptions;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class SyncTask extends Task.Backgroundable {
    Project project;
    DevSpace devSpace;
    NhctlDescribeService nhctlDescribeService;
    DevModeService devModeService;

    public SyncTask(@Nullable Project project, DevSpace devSpace, DevModeService devModeService, NhctlDescribeService nhctlDescribeService) {
        super(project, "Syncing file", false);
        this.project = project;
        this.devSpace = devSpace;
        this.devModeService = devModeService;
        this.nhctlDescribeService = nhctlDescribeService;
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        Notifications.Bus.notify(new Notification("Nocalhost.Notification", "Sync file finished", "", NotificationType.INFORMATION), project);
        ProgressManager.getInstance().run(new PortForwardTask(project, devSpace, devModeService, nhctlDescribeService));
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        final String kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace).toString();
        final String appName = devSpace.getContext().getApplicationName();

        try {
            // nhctl sync ...
            NhctlSyncOptions nhctlSyncOptions = new NhctlSyncOptions();
            nhctlSyncOptions.setDeployment(devModeService.getName());
            nhctlSyncOptions.setKubeconfig(kubeconfigPath);
            nhctlCommand.sync(appName, nhctlSyncOptions);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
