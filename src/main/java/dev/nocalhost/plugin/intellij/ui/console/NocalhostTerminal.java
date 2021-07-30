package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.terminal.JBTerminalSystemSettingsProviderBase;
import com.jediterm.terminal.ProcessTtyConnector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.io.IOException;
import java.io.OutputStream;

public class NocalhostTerminal extends ShellTerminalWidget {
    private static final Logger LOG = Logger.getInstance(NocalhostTerminal.class);

    public NocalhostTerminal(@NotNull Project project,
                             @NotNull JBTerminalSystemSettingsProviderBase settingsProvider,
                             @NotNull Disposable parent) {
        super(project, settingsProvider, parent);
    }

    public void executeCommand(GeneralCommandLine command) throws IOException {
        super.executeCommand(command.getCommandLineString());
    }

    public void terminateCommandProcess() {
        ProcessTtyConnector processTtyConnector = ShellTerminalWidget.getProcessTtyConnector(
                getTtyConnector());
        if (processTtyConnector != null) {
            Process process = processTtyConnector.getProcess();
            if (process.isAlive()) {
                OutputStream outputStream = process.getOutputStream();
                try {
                    outputStream.write(3);
                    outputStream.flush();
                } catch (IOException e) {
                    LOG.warn("Fail to send ctrl+c to remote process", e);
                }
                process.destroy();
            }
        }
    }
}
