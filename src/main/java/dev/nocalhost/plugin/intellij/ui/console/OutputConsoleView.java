package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

public class OutputConsoleView extends ConsoleViewImpl {
    public OutputConsoleView(@NotNull Project project) {
        super(project, true);
    }

    @Override
    public AnAction @NotNull [] createConsoleActions() {
        AnAction[] consoleActions = super.createConsoleActions();
        return ArrayUtils.subarray(consoleActions, 2, consoleActions.length);
    }
}
