package dev.nocalhost.plugin.intellij.ui;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleExecuteNotifier;
import dev.nocalhost.plugin.intellij.ui.console.Action;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostConsoleWindow;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostLogWindow;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostTerminalWindow;
import dev.nocalhost.plugin.intellij.ui.tree.WorkloadNode;

public class NocalhostConsoleWindowFactory implements ToolWindowFactory, DumbAware {

    private Project project;
    private ToolWindow toolWindow;

    private ContentManager contentManager;

    private List<Content> contents;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

        contents = Lists.newArrayList();
        contentManager = toolWindow.getContentManager();
        final Application application = ApplicationManager.getApplication();
        application.getMessageBus().connect().subscribe(
                NocalhostConsoleExecuteNotifier.NOCALHOST_CONSOLE_EXECUTE_NOTIFIER_TOPIC,
                this::updateTab
        );
    }

    private void updateTab(WorkloadNode node, Action action) {
        NocalhostConsoleWindow nocalhostConsoleWindow;
        switch (action) {
            case LOGS:
                nocalhostConsoleWindow = new NocalhostLogWindow(project, toolWindow, node);
                break;
            case TERMINAL:
                nocalhostConsoleWindow = new NocalhostTerminalWindow(project, toolWindow, node);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + action);
        }
        JComponent panel = nocalhostConsoleWindow.getPanel();
        String title = nocalhostConsoleWindow.getTitle();
        if (panel == null || StringUtils.isBlank(title)) {
            return;
        }
        Content content = ContentFactory.SERVICE.getInstance().createContent(panel, title, false);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }
}
