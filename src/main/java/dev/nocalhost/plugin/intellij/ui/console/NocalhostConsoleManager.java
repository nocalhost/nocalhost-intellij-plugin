package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;

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

    private static NocalhostLogs createLogs(Project project, GeneralCommandLine command)
            throws com.intellij.execution.ExecutionException {
        NocalhostLogs nocalhostLogs = new NocalhostLogs(project);
        nocalhostLogs.executeCommand(command);
        return nocalhostLogs;
    }
}
