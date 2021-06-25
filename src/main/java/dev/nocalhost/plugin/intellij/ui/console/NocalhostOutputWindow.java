package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;

import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;

public class NocalhostOutputWindow extends LogsToolWindowPanel implements Disposable {

    private final ConsoleView consoleView;

    public NocalhostOutputWindow(Project project) {
        super(false);

        consoleView = new OutputConsoleView(project);
        Disposer.register(this, consoleView);
        add(consoleView.getComponent());

        DefaultActionGroup actionGroup = new DefaultActionGroup(consoleView.createConsoleActions());
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(
                "Nocalhost.Output.Window.Toolbar", actionGroup, false);
        setToolbar(actionToolbar.getComponent());

        project.getMessageBus().connect().subscribe(
                NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC,
                this::appendOutput
        );
    }

    private void appendOutput(String text) {
        ApplicationManager.getApplication().invokeLater(() -> {
            consoleView.print(text, ConsoleViewContentType.LOG_INFO_OUTPUT);
        });
    }

    @Override
    public void dispose() {

    }
}
