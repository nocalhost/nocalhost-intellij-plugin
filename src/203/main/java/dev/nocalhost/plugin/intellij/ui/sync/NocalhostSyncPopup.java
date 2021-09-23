package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.dvcs.ui.BranchActionGroupPopup;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.apache.commons.lang3.StringUtils;
import com.intellij.ui.popup.PopupFactoryImpl;

import java.util.List;
import dev.nocalhost.plugin.intellij.topic.NocalhostSyncUpdateNotifier;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;

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
        var popup = new BranchActionGroupPopup("Nocalhost Sync Manage", project, (action) -> false, actions, "Nocalhost.Sync.Manage");
        project.getMessageBus().connect(popup).subscribe(
                NocalhostSyncUpdateNotifier.NOCALHOST_SYNC_UPDATE_NOTIFIER_TOPIC,
                results -> update(popup, results)
        );
        return popup;
    }

    private void update(@NotNull BranchActionGroupPopup popup, @NotNull List<NhctlDevAssociateQueryResult> results) {
        List<Object> items = popup.getListStep().getValues();
        items.forEach(x -> {
            var item = (PopupFactoryImpl.ActionItem) x;
            var group = (ServiceActionGroup) item.getAction();
            results.forEach(it -> {
                if (StringUtils.equals(it.getSha(), group.getSha())) {
                    group.setResult(it);
                }
            });
        });
        popup.update();
    }
}
