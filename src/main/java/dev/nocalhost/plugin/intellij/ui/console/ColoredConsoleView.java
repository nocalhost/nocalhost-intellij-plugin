package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class ColoredConsoleView extends ConsoleViewImpl implements AnsiEscapeDecoder.ColoredTextAcceptor {
    private final AnsiEscapeDecoder ansiEscapeDecoder = new AnsiEscapeDecoder();

    public ColoredConsoleView(@NotNull Project project) {
        super(project, true);
    }

    @Override
    public void coloredTextAvailable(@NotNull String text, @NotNull Key attributes) {
        super.print(text, ConsoleViewContentType.getConsoleViewType(attributes));
    }

    @Override
    public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
        ansiEscapeDecoder.escapeText(text, getProcessOutputType(contentType), this);
    }

    @Override
    public AnAction @NotNull [] createConsoleActions() {
        AnAction[] consoleActions = super.createConsoleActions();
        return ArrayUtils.subarray(consoleActions, 2, consoleActions.length);
    }

    private Key<?> getProcessOutputType(ConsoleViewContentType contentType) {
        return StringUtils.endsWith(contentType.toString(), "ERROR_OUTPUT")
                ? ProcessOutputTypes.STDERR : ProcessOutputTypes.STDOUT;
    }
}
