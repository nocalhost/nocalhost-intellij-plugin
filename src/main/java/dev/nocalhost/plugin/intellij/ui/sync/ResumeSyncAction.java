package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.nhctl.NhctlSyncCommand;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class ResumeSyncAction extends DumbAwareAction {
    private final Project project;
    private final NhctlDevAssociateQueryResult result;

    public ResumeSyncAction(@NotNull Project project, @NotNull NhctlDevAssociateQueryResult result) {
        super("Resume File Sync");
        this.result = result;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var cmd = new NhctlSyncCommand();
                cmd.setResume(true);
                cmd.setNamespace(result.getServicePack().getNamespace());
                cmd.setKubeConfig(Paths.get(result.getKubeconfigPath()));
                cmd.setContainer(result.getServicePack().getContainer());
                cmd.setDeployment(result.getServicePack().getServiceName());
                cmd.setControllerType(result.getServicePack().getServiceType());
                cmd.setApplicationName(result.getServicePack().getApplicationName());
                cmd.execute();
            } catch (Exception ex) {
                ErrorUtil.dealWith(project, "Failed to resume sync", "Error occurred while resume sync.", ex);
            }
        });
    }
}
