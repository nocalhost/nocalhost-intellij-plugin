package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.ui.ManagerNocalhostAccountsDialog;

public class ManageNocalhostAccountsAction extends AnAction {
    private final Project project;

    public ManageNocalhostAccountsAction(Project project) {
        super("Manage Nocalhost accounts", "", AllIcons.CodeWithMe.Users);
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new ManagerNocalhostAccountsDialog(project).showAndGet();
    }
}
