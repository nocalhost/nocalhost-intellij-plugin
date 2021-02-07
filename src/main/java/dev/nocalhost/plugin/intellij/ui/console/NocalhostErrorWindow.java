package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;

import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;

public class NocalhostErrorWindow extends NocalhostConsoleWindow {

    private final Project project;
    private final ToolWindow toolWindow;

    private LogPanel panel;
    private ConsoleView consoleView;

    private String title;
    private String content;
    private String eMessage;

    public NocalhostErrorWindow(Project project, ToolWindow toolWindow, String title, String content, String eMessage) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.title = title;
        this.content = content;
        this.eMessage = eMessage;


        consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

        panel = new LogPanel(false);
        consoleView.print(
                content + "\n" + eMessage,
                ConsoleViewContentType.LOG_ERROR_OUTPUT);

        AnAction[] consoleActions = consoleView.createConsoleActions();
        AnAction[] consoleViewActions = ArrayUtils.subarray(consoleActions, 2, consoleActions.length);
        DefaultActionGroup actionGroup = new DefaultActionGroup(consoleViewActions);

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Nocalhost.Output.Window.Toolbar", actionGroup, false);
        panel.setToolbar(actionToolbar.getComponent());

        panel.add(consoleView.getComponent());
    }

    @Override
    public JComponent getPanel() {
        return panel;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
