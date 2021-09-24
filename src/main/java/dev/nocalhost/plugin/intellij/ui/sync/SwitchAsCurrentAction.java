package dev.nocalhost.plugin.intellij.ui.sync;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

public class SwitchAsCurrentAction extends DumbAwareAction {
    private final Project project;

    public SwitchAsCurrentAction(@NotNull Project project) {
        super("Switch This Service as Current Service");
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        // TODO
    }
}
