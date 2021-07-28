package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;

import org.jetbrains.plugins.terminal.JBTerminalSystemSettingsProvider;
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import dev.nocalhost.plugin.intellij.utils.ErrorUtil;

public final class NocalhostConsoleManager {
    public static void activateOutputWindow(Project project) {
        if (project.isDisposed()) {
            return;
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                .getToolWindow("Nocalhost Console");
        if (toolWindow == null) {
            return;
        }
        ApplicationManager.getApplication().invokeAndWait(() -> {
            toolWindow.activate(() -> {
                ContentManager contentManager = toolWindow.getContentManager();
                Content content = contentManager.getContent(0);
                if (content != null) {
                    contentManager.setSelectedContent(content);
                }
            });
        });
    }

    public static void openLogsWindow(Project project, String title, GeneralCommandLine command) {
        if (project.isDisposed()) {
            return;
        }

        try {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                    .getToolWindow("Nocalhost Console");
            if (toolWindow == null) {
                return;
            }

            ContentManager manager = toolWindow.getContentManager();

            NocalhostLogs logs = createLogs(project, command);

            ApplicationManager.getApplication().invokeLater(() -> {
                toolWindow.activate(() -> {
                    Content content = ContentFactory.SERVICE.getInstance().createContent(logs,
                            title, false);
                    content.setDisposer(logs);

                    manager.addContent(content);
                    manager.setSelectedContent(content);
                });
            });
        } catch (Exception e) {
            ErrorUtil.dealWith(project, "Opening logs window error",
                    "Error occurs while opening logs window", e);
        }
    }

    public static Disposable openTerminalWindow(Project project,
                                             String title,
                                             GeneralCommandLine command) {
        if (project.isDisposed()) {
            return null;
        }

        try {
            ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                                                     .getToolWindow("Nocalhost Console");
            if (toolWindow == null) {
                return null;
            }

            ContentManager manager = toolWindow.getContentManager();

            NocalhostTerminal terminal = createTerminal(project, command, manager);
            Content content = ContentFactory.SERVICE.getInstance().createContent(terminal,
                    title, false);

            ApplicationManager.getApplication().invokeLater(() -> {
                toolWindow.activate(() -> {
                    manager.addContent(content);
                    manager.setSelectedContent(content);
                });
            });
            return () -> terminal.terminateProcess();
        } catch (Exception e) {
            ErrorUtil.dealWith(project, "Opening terminal window error",
                    "Error occurs while opening terminal window", e);
        }
        return null;
    }

    private static NocalhostLogs createLogs(Project project, GeneralCommandLine command)
            throws com.intellij.execution.ExecutionException {
        NocalhostLogs nocalhostLogs = new NocalhostLogs(project);
        nocalhostLogs.executeCommand(command);
        return nocalhostLogs;
    }

    private static NocalhostTerminal createTerminal(Project project,
                                                    GeneralCommandLine command,
                                                    Disposable parent)
            throws ExecutionException, IOException {
        LocalTerminalDirectRunner localTerminalDirectRunner = LocalTerminalDirectRunner
                .createTerminalRunner(project);
        PtyProcess ptyProcess = localTerminalDirectRunner.createProcess(project.getBasePath());
        TtyConnector connector = new PtyProcessTtyConnector(ptyProcess, StandardCharsets.UTF_8);

        JBTerminalSystemSettingsProvider settingsProvider = new JBTerminalSystemSettingsProvider();
        NocalhostTerminal nocalhostTerminal = new NocalhostTerminal(
                project, settingsProvider, parent);
        Disposer.register(nocalhostTerminal, settingsProvider);
        nocalhostTerminal.start(connector);
        nocalhostTerminal.executeCommand(command);
        return nocalhostTerminal;
    }
}