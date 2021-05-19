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
