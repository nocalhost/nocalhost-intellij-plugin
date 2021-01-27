package dev.nocalhost.plugin.intellij.task;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class PortForwardTask extends Task.Backgroundable {
    Project project;
    DevSpace devSpace;
    NhctlDescribeService nhctlDescribeService;
    DevModeService devModeService;

    public PortForwardTask(@Nullable Project project, DevSpace devSpace, DevModeService devModeService, NhctlDescribeService nhctlDescribeService) {
        super(project, "Port forwarding", false);
        this.project = project;
        this.devSpace = devSpace;
        this.devModeService = devModeService;
        this.nhctlDescribeService = nhctlDescribeService;
    }


    @Override
    public void onSuccess() {
        super.onSuccess();
        Notifications.Bus.notify(new Notification("Nocalhost.Notification", "Port forward finished", "", NotificationType.INFORMATION), project);
    }


    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        final String kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace).toString();
        final String appName = devSpace.getContext().getApplicationName();

        try {
            // nhctl port-forward ...
            NhctlPortForwardStartOptions nhctlPortForwardOptions = new NhctlPortForwardStartOptions();
            nhctlPortForwardOptions.setDeployment(devModeService.getName());
            nhctlPortForwardOptions.setWay(NhctlPortForwardStartOptions.Way.DEV_PORTS);
            nhctlPortForwardOptions.setDevPorts(nhctlDescribeService.getRawConfig().getDevPorts());
            nhctlPortForwardOptions.setKubeconfig(kubeconfigPath);
            nhctlCommand.startPortForward(appName, nhctlPortForwardOptions);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
