package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.nhctl.NhctlSyncStatusCommand;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class OverrideSyncAction extends DumbAwareAction {
    private final Project project;
    private final NhctlDevAssociateQueryResult result;

    public OverrideSyncAction(@NotNull Project project, @NotNull NhctlDevAssociateQueryResult result) {
        super("Override Remote Changing According to Local Files");
        this.result = result;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var cmd = new NhctlSyncStatusCommand();
                cmd.setOverride(true);
                cmd.setNamespace(result.getServicePack().getNamespace());
                cmd.setKubeConfig(Paths.get(result.getKubeconfigPath()));
                cmd.setDeployment(result.getServicePack().getServiceName());
                cmd.setControllerType(result.getServicePack().getServiceType());
                cmd.setApplicationName(result.getServicePack().getApplicationName());
                cmd.execute();
            } catch (Exception ex) {
                ErrorUtil.dealWith(project, "Override sync", "Error occurred while sync override", ex);
            }
        });
    }
}
