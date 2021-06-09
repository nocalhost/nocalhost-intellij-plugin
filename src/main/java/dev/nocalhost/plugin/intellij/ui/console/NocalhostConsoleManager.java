package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;

import org.jetbrains.plugins.terminal.JBTerminalSystemSettingsProvider;
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner;
import org.jetbrains.plugins.terminal.TerminalProcessOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import dev.nocalhost.plugin.intellij.utils.ErrorUtil;

public final class NocalhostConsoleManager {
    public static void activateOutputWindow(Project project) {
        if (project.isDisposed()) {
            return;
        }
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                        .getToolWindow("Nocalhost Console");
                if (toolWindow == null) {
                    return;
                }
                toolWindow.activate(() -> {
                    ContentManager contentManager = toolWindow.getContentManager();
                    Content content = contentManager.getContent(0);
                    if (content != null) {
                        contentManager.setSelectedContent(content);
                    }
                });
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Activating output window error",
                        "Error occurs while activating output window", e);
            }
        });
    }

    public static void openLogsWindow(Project project, String title, GeneralCommandLine command) {
        if (project.isDisposed()) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                        .getToolWindow("Nocalhost Console");
                if (toolWindow == null) {
                    return;
                }

                ContentManager manager = toolWindow.getContentManager();

                NocalhostLogs logs = createLogs(project, command);

                toolWindow.activate(() -> {
                    Content content = manager.findContent(title);
                    if (content != null) {
                        manager.removeContent(content, true);
                    }

                    content = ContentFactory.SERVICE.getInstance().createContent(logs, title, false);

                    manager.addContent(content);
                    manager.setSelectedContent(content);
                });
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Opening logs window error",
                        "Error occurs while opening logs window", e);
            }
        });
    }

    public static void openTerminalWindow(Project project,
                                          String title,
                                          GeneralCommandLine command) {
        if (project.isDisposed()) {
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ToolWindow toolWindow = ToolWindowManager.getInstance(project)
                        .getToolWindow("Nocalhost Console");
                if (toolWindow == null) {
                    return;
                }

                ContentManager manager = toolWindow.getContentManager();

                NocalhostTerminal terminal = createTerminal(project, command, manager);

                toolWindow.activate(() -> {
                    Content content = manager.findContent(title);
                    if (content != null) {
                        manager.removeContent(content, true);
                    }

                    content = ContentFactory.SERVICE.getInstance().createContent(terminal, title,
                            false);

                    manager.addContent(content);
                    manager.setSelectedContent(content);
                });
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Opening terminal window error",
                        "Error occurs while opening terminal window", e);
            }
        });

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
        TerminalProcessOptions options = new TerminalProcessOptions(project.getBasePath(), 80, 25);
        JBTerminalSystemSettingsProvider settingsProvider = new JBTerminalSystemSettingsProvider();
        JBTerminalWidget widget = new JBTerminalWidget(project, settingsProvider, parent);
        PtyProcess ptyProcess = localTerminalDirectRunner.createProcess(options, widget);
        TtyConnector connector = new PtyProcessTtyConnector(ptyProcess, StandardCharsets.UTF_8);

        NocalhostTerminal nocalhostTerminal = new NocalhostTerminal(
                project, settingsProvider, parent);
        nocalhostTerminal.start(connector);
        nocalhostTerminal.executeCommand(command);
        return nocalhostTerminal;
    }
}
