package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

public class DeAssociateAction extends DumbAwareAction {
    private final Project project;

    public DeAssociateAction(@NotNull Project project) {
        super("De associate from current dir");
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // TODO
    }
}
