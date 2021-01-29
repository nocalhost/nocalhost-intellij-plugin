package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;

public class NocalhostOutputWindow {
    private final Project project;
    private final ToolWindow toolWindow;

    private JPanel panel;
    private JBTextArea textArea;
    private JBScrollPane scrollPane;

    public NocalhostOutputWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

        panel = new SimpleToolWindowPanel(true);
        textArea = new JBTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setAutoscrolls(true);
        scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane);

        project.getMessageBus().connect().subscribe(
                NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER,
                this::appendOutput
        );
    }


    private void appendOutput(String text) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            textArea.append(text);
            int length = textArea.getDocument().getLength();
            textArea.select(length, length);
        });
    }

    public JPanel getPanel() {
        return panel;
    }
}
