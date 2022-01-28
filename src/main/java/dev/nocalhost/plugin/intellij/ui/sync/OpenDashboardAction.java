package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;

public class OpenDashboardAction extends DumbAwareAction {
    private final Project project;
    private final NhctlDevAssociateQueryResult result;

    public OpenDashboardAction(@NotNull Project project, @NotNull NhctlDevAssociateQueryResult result) {
        super("Open Sync Dashboard");
        this.result = result;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        try {
            var gui = result.getSyncthingStatus().getGui();
            BrowserUtil.browse("http" + "://" + gui);
        } catch (Exception ex) {
            NocalhostNotifier
                    .getInstance(project)
                    .notifyError("Failed to open sync dashboard", "Error occurred while opening sync dashboard", ex.getMessage());
        }
    }
}
