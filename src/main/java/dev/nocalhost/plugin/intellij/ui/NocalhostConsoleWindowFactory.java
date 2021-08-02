package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.topic.NocalhostExceptionPrintNotifier;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostErrorWindow;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostLogs;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostOutputWindow;

public class NocalhostConsoleWindowFactory implements ToolWindowFactory, DumbAware {

    private Project project;
    private ToolWindow toolWindow;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

        toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                Object component = event.getContent().getComponent();
                if (component instanceof NocalhostLogs) {
                    NocalhostLogs nocalhostLogs = (NocalhostLogs) component;
                    nocalhostLogs.terminateCommandProcess();
                }
            }
        });

        createOutputWindow();

        project.getMessageBus().connect().subscribe(
                NocalhostExceptionPrintNotifier.NOCALHOST_EXCEPTION_PRINT_NOTIFIER_TOPIC,
                this::errorPrint
        );
    }

    private void errorPrint(String title, String contentMsg, String eMessage) {
        NocalhostErrorWindow nocalhostErrorWindow = new NocalhostErrorWindow(project, title, contentMsg, eMessage);
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = ContentFactory.SERVICE.getInstance().createContent(nocalhostErrorWindow, nocalhostErrorWindow.getTitle(), false);
        content.setDisposer(nocalhostErrorWindow);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }

    private void createOutputWindow() {
        NocalhostOutputWindow nocalhostOutputWindow = new NocalhostOutputWindow(project);

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = ContentFactory.SERVICE.getInstance().createContent(nocalhostOutputWindow, "OUTPUT", false);
        content.setCloseable(false);
        content.setDisposer(nocalhostOutputWindow);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }
}
