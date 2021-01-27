package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TerminalTask extends Task.Backgroundable {
    public TerminalTask(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title) {
        super(project, title);

    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {

    }
}
