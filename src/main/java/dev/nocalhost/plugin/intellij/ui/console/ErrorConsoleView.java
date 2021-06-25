package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

public class ErrorConsoleView extends ConsoleViewImpl {
    public ErrorConsoleView(@NotNull Project project) {
        super(project, true);
    }

    @Override
    public AnAction @NotNull [] createConsoleActions() {
        AnAction[] consoleActions = super.createConsoleActions();
        return ArrayUtils.toArray(consoleActions[2], consoleActions[4]);
    }
}
