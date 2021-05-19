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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleExecuteNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleTerminalNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostExceptionPrintNotifier;
import dev.nocalhost.plugin.intellij.ui.console.Action;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostConsoleWindow;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostErrorWindow;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostLogWindow;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostOutputWindow;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostTerminal;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostTerminalWindow;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

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
                if (component instanceof NocalhostTerminal) {
                    NocalhostTerminal nocalhostTerminal = (NocalhostTerminal) component;
                    nocalhostTerminal.terminateProcess();
                }
            }
        });

        createOutputWindow();

        project.getMessageBus().connect().subscribe(
                NocalhostConsoleExecuteNotifier.NOCALHOST_CONSOLE_EXECUTE_NOTIFIER_TOPIC,
                this::updateTab
        );
        project.getMessageBus().connect().subscribe(
                NocalhostConsoleTerminalNotifier.NOCALHOST_CONSOLE_TERMINAL_NOTIFIER_TOPIC,
                this::newTerminal
        );
        project.getMessageBus().connect().subscribe(
                NocalhostExceptionPrintNotifier.NOCALHOST_EXCEPTION_PRINT_NOTIFIER_TOPIC,
                this::errorPrint
        );
    }

    private void errorPrint(String title, String contentMsg, String eMessage) {
        NocalhostErrorWindow nocalhostErrorWindow = new NocalhostErrorWindow(project, title, contentMsg, eMessage);
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = ContentFactory.SERVICE.getInstance().createContent(nocalhostErrorWindow.getPanel(), nocalhostErrorWindow.getTitle(), false);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }

    private void createOutputWindow() {
        NocalhostOutputWindow nocalhostOutputWindow = new NocalhostOutputWindow(project);

        ContentManager contentManager = toolWindow.getContentManager();
        Content content = ContentFactory.SERVICE.getInstance().createContent(nocalhostOutputWindow.getPanel(), "OUTPUT", false);
        content.setCloseable(false);
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }

    private void newTerminal(Path kubeConfigPath,
                             String namespace,
                             String applicationName,
                             String deploymentName) {
        NocalhostConsoleWindow nocalhostConsoleWindow = new NocalhostTerminalWindow(project,
                kubeConfigPath, namespace, applicationName, deploymentName);
        addContent(nocalhostConsoleWindow);
        toolWindow.show();
    }

    private void updateTab(ResourceNode node, Action action) {
        toolWindow.show();
        NocalhostConsoleWindow nocalhostConsoleWindow;
        switch (action) {
            case LOGS:
                nocalhostConsoleWindow = new NocalhostLogWindow(project, node);
                break;
            case TERMINAL:
                nocalhostConsoleWindow = new NocalhostTerminalWindow(project, node);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + action);
        }
        addContent(nocalhostConsoleWindow);
        toolWindow.show();
    }

    private void addContent(final NocalhostConsoleWindow nocalhostConsoleWindow) {
        JComponent panel = nocalhostConsoleWindow.getPanel();
        String title = nocalhostConsoleWindow.getTitle();
        if (panel == null || StringUtils.isBlank(title)) {
            return;
        }
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.findContent(title);
        if (content != null) {
            contentManager.removeContent(content, true);
        }
        content = ContentFactory.SERVICE.getInstance().createContent(panel, title, false);
        contentManager.addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                if (nocalhostConsoleWindow instanceof NocalhostLogWindow) {
                    ((NocalhostLogWindow) nocalhostConsoleWindow).stopProcess();
                }
            }
        });
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
    }
}
