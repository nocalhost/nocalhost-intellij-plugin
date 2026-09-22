package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.ui.dialog.ManagerNocalhostAccountsDialog;

public class ManageNocalhostAccountsAction extends DumbAwareAction {
    private final Project project;

    public ManageNocalhostAccountsAction(Project project) {
        super(NocalhostI18n.get("action.manageAccounts"), "", AllIcons.CodeWithMe.Users);
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new ManagerNocalhostAccountsDialog(project).showAndGet();
    }
}
