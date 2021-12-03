package dev.nocalhost.plugin.intellij.ui.action.namespace;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import icons.NocalhostIcons;

public class WakeupAction extends DumbAwareAction {
    private final Project project;
    private final NamespaceNode node;

    public WakeupAction(@NotNull Project project, @NotNull NamespaceNode node) {
        super("Wakeup", "", NocalhostIcons.Menu.Wakeup);
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        var account = node.getClusterNode().getNocalhostAccount();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ApplicationManager.getApplication()
                                  .getService(NocalhostApi.class)
                                  .forceWakeup(account.getServer(), account.getJwt(), node.getNamespacePack().getSpaceId());
            } catch (Exception ex) {
                NocalhostNotifier.getInstance(project)
                                 .notifyError("Failed to wakeup", ex.getMessage());
            }
        });
    }
}

