package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.dvcs.ui.BranchActionGroupPopup;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;

import org.jetbrains.annotations.NotNull;

public class NocalhostSyncPopup {
    private final Project project;
    private final ActionGroup actions;
    private NocalhostSyncPopup(@NotNull Project project, @NotNull ActionGroup actions) {
        this.project = project;
        this.actions = actions;
    }

    public static NocalhostSyncPopup getInstance(@NotNull Project project, @NotNull ActionGroup actions) {
        return new NocalhostSyncPopup(project, actions);
    }

    public BranchActionGroupPopup asListPopup() {
        var statusBar = WindowManager.getInstance().getStatusBar(project);
        return new BranchActionGroupPopup("Nocalhost Sync Manage", project, (action) -> false, actions, "Nocalhost.Sync.Manage", DataManager.getInstance().getDataContext(statusBar.getComponent()));
    }
}
