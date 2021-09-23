package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.nhctl.NhctlSyncCommand;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class ResumeSyncAction extends DumbAwareAction {
    private final Project project;

    public ResumeSyncAction(@NotNull Project project) {
        super("Resume File Sync");
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        var service = NhctlUtil.getDevModeService(project);
        if (service != null) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    var cmd = new NhctlSyncCommand();
                    cmd.setResume(true);
                    cmd.setNamespace(service.getNamespace());
                    cmd.setDeployment(service.getServiceName());
                    cmd.setController(service.getContainerName());
                    cmd.setKubeConfig(service.getKubeConfigPath());
                    cmd.setControllerType(service.getServiceType());
                    cmd.setApplicationName(service.getApplicationName());
                    cmd.execute();
                } catch (Exception ex) {
                    ErrorUtil.dealWith(project, "Resume sync", "Error occurred while sync override", ex);
                }
            });
        }
    }
}
