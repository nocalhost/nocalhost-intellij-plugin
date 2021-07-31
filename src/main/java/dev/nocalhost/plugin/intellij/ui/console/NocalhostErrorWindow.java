package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;

import lombok.Getter;

public class NocalhostErrorWindow extends LogsToolWindowPanel implements Disposable {

    @Getter
    private final String title;

    public NocalhostErrorWindow(Project project, String title, String content, String eMessage) {
        super(false);

        this.title = title;

        ConsoleView consoleView = new ErrorConsoleView(project);
        Disposer.register(this, consoleView);
        add(consoleView.getComponent());

        consoleView.print(content + "\n" + eMessage, ConsoleViewContentType.LOG_ERROR_OUTPUT);

        DefaultActionGroup actionGroup = new DefaultActionGroup(consoleView.createConsoleActions());
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(
                "Nocalhost.Error.Window.Toolbar", actionGroup, false);
        actionToolbar.setTargetComponent(this);
        setToolbar(actionToolbar.getComponent());
    }

    @Override
    public void dispose() {

    }
}
