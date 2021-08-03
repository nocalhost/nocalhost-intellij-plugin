package dev.nocalhost.plugin.intellij.utils;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;

import org.jetbrains.plugins.terminal.TerminalView;

public final class TerminalUtil {
    public static void openTerminal(Project project, String title, GeneralCommandLine commandLine) {
        try {
            TerminalView.getInstance(project)
                    .createLocalShellWidget(project.getBasePath(), title, true)
                    .executeCommand(commandLine.getCommandLineString());
        } catch (Exception e) {
            ErrorUtil.dealWith(project, "Terminal open error",
                    "Error occurs while openning terminal", e);
        }
    }
}
