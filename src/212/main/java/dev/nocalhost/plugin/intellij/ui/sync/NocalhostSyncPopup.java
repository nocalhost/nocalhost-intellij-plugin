package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.dvcs.ui.BranchActionGroupPopup;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.popup.PopupFactoryImpl;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import dev.nocalhost.plugin.intellij.topic.NocalhostSyncUpdateNotifier;

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
        var popup = new BranchActionGroupPopup("Nocalhost Sync Manage", project, (action) -> false, actions, "Nocalhost.Sync.Manage", DataManager.getInstance().getDataContext(statusBar.getComponent()));
        project.getMessageBus().connect(popup).subscribe(
                NocalhostSyncUpdateNotifier.NOCALHOST_SYNC_UPDATE_NOTIFIER_TOPIC,
                results -> {
                    List<Object> items = popup.getListStep().getValues();
                    items.forEach(x -> {
                        var item = (PopupFactoryImpl.ActionItem) x;
                        var group = (CurrentServiceActionGroup) item.getAction();
                        results.forEach(it -> {
                            if (StringUtils.equals(it.getSha(), group.getSha())) {
                                group.setDesc(it.getSyncthingStatus().getMessage());
                            }
                        });
                    });
                    popup.update();
                }
        );
        return popup;
    }
}
