package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.nhctl.NhctlSyncStatusCommand;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class OverrideSyncAction extends DumbAwareAction {
    private final Project project;

    public OverrideSyncAction(@NotNull Project project) {
        super("Override Remote Changing According to Local Files");
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        var service = NhctlUtil.getDevModeService(project);
        if (service != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    var cmd = new NhctlSyncStatusCommand();
                    cmd.setOverride(true);
                    cmd.setNamespace(service.getNamespace());
                    cmd.setDeployment(service.getServiceName());
                    cmd.setKubeConfig(service.getKubeConfigPath());
                    cmd.setControllerType(service.getServiceType());
                    cmd.setApplicationName(service.getApplicationName());
                    cmd.execute();
                } catch (Exception ex) {
                    ErrorUtil.dealWith(project, "Override sync", "Error occurred while sync override", ex);
                }
            });
        }
    }
}
