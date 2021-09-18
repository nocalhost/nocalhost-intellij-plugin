package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

public class OverrideSyncAction extends DumbAwareAction {
    private final Project project;

    public OverrideSyncAction(@NotNull Project project) {
        super("Override remote changing according to local files");
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // TODO
    }
}
