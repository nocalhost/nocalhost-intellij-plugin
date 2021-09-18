package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

public class ResumeSyncAction extends DumbAwareAction {
    private final Project project;

    public ResumeSyncAction(@NotNull Project project) {
        super("Resume file sync");
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // TODO
    }
}
