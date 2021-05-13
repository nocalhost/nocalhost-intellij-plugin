package dev.nocalhost.plugin.intellij.ui.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.ui.dialog.ConnectNocalhostServerDialog;

public class ConnectNocalhostApiServerAction extends AnAction implements DumbAware {
    private final Project project;

    public ConnectNocalhostApiServerAction(Project project) {
        super("Connect to Nocalhost API Server", "", AllIcons.General.User);
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new ConnectNocalhostServerDialog(project).showAndGet();
    }
}
