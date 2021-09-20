package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.dvcs.ui.BranchActionGroupPopup;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.project.Project;
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
        return new BranchActionGroupPopup("Nocalhost Sync Manage", project, (action) -> false, actions, "Nocalhost.Sync.Manage");
    }
}
