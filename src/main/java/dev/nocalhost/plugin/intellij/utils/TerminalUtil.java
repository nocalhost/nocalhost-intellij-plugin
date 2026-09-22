package dev.nocalhost.plugin.intellij.utils;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;

import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;

public final class TerminalUtil {
    public static void openTerminal(Project project, String title, GeneralCommandLine commandLine) {
        try {
            TerminalToolWindowManager.getInstance(project)
                                     .createShellWidget(project.getBasePath(), title, true, true)
                                     .sendCommandToExecute(commandLine.getCommandLineString());
        } catch (Exception e) {
            ErrorUtil.dealWith(project, NocalhostI18n.get("error.terminalOpen"),
                    NocalhostI18n.get("error.terminalOpen.content"), e);
        }
    }
}
