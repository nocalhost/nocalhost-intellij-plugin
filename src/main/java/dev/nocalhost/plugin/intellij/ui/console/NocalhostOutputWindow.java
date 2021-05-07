package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;

import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;

public class NocalhostOutputWindow {

    private final LogPanel panel;
    private final ConsoleView consoleView;

    public NocalhostOutputWindow(Project project) {

        consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        Disposer.register(project, consoleView);

        panel = new LogPanel(false);
        panel.add(consoleView.getComponent());

        AnAction[] consoleActions = consoleView.createConsoleActions();
        AnAction[] consoleViewActions = ArrayUtils.subarray(consoleActions, 2, consoleActions.length);
        DefaultActionGroup actionGroup = new DefaultActionGroup(consoleViewActions);

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Nocalhost.Output.Window.Toolbar", actionGroup, false);
        panel.setToolbar(actionToolbar.getComponent());

        project.getMessageBus().connect().subscribe(
                NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC,
                this::appendOutput
        );
    }


    private void appendOutput(String text) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            consoleView.print(text, ConsoleViewContentType.LOG_INFO_OUTPUT);
        });
    }

    public JComponent getPanel() {
        return panel;
    }
}
