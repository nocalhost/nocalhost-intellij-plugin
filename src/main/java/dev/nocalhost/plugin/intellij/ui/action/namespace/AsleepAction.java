package dev.nocalhost.plugin.intellij.ui.action.namespace;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.node.NamespaceNode;
import icons.NocalhostIcons;

public class AsleepAction extends DumbAwareAction {
    private final Project project;
    private final NamespaceNode node;

    public AsleepAction(@NotNull Project project, @NotNull NamespaceNode node) {
        super("Sleep", "", NocalhostIcons.Menu.Asleep);
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        if ( ! MessageDialogBuilder.yesNo("Sleep", "Confirm to enter sleep mode?").ask(project)) {
            return;
        }
        var account = node.getClusterNode().getNocalhostAccount();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ApplicationManager.getApplication()
                                  .getService(NocalhostApi.class)
                                  .forceAsleep(account.getServer(), account.getJwt(), node.getNamespacePack().getSpaceId());
                NocalhostNotifier.getInstance(project)
                        .notifySuccess("Sleep successful", "");
            } catch (Exception ex) {
                NocalhostNotifier.getInstance(project)
                                 .notifyError("Failed to sleep", ex.getMessage());
            }
        });
    }
}

