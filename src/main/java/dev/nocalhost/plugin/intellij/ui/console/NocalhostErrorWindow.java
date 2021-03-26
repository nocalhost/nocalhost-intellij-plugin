package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;

import javax.swing.*;

public class NocalhostErrorWindow extends NocalhostConsoleWindow {

    private final LogPanel panel;
    private final String title;

    public NocalhostErrorWindow(Project project, String title, String content, String eMessage) {
        this.title = title;


        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

        panel = new LogPanel(false);
        consoleView.print(
                content + "\n" + eMessage,
                ConsoleViewContentType.LOG_ERROR_OUTPUT);

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
