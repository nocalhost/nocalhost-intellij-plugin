package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.JBEmptyBorder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;

public class NocalhostOutputWindow {
    private final Project project;
    private final ToolWindow toolWindow;

    private NocalhostOutputWindowPanel panel;
    private JBTextArea textArea;
    private JBScrollPane scrollPane;

    private AtomicBoolean scrollToEnd = new AtomicBoolean(true);

    public NocalhostOutputWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

        panel = new NocalhostOutputWindowPanel(false);
        textArea = new JBTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(new JBEmptyBorder(0));
        panel.add(scrollPane);

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new ClearAction());
        actionGroup.add(new ScrollToEndAction());
        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Nocalhost.Output.Window.Toolbar", actionGroup, false);
        panel.setToolbar(actionToolbar.getComponent());

        project.getMessageBus().connect().subscribe(
                NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC,
                this::appendOutput
        );
    }


    private void appendOutput(String text) {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            int currentPos = textArea.getCaretPosition();
            textArea.append(text);
            if (scrollToEnd.get()) {
                int endPos = textArea.getDocument().getLength();
                textArea.select(endPos, endPos);
            } else {
                textArea.select(currentPos, currentPos);
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }

    private class ClearAction extends DumbAwareAction {
        private ClearAction() {
            super("Clear", "Clear", AllIcons.Actions.GC);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(true);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            textArea.setText("");
        }
    }

    private class ScrollToEndAction extends ToggleAction {
        private ScrollToEndAction() {
            super("Scroll to End", "Scroll to End", AllIcons.RunConfigurations.Scroll_down);
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent e) {
            return scrollToEnd.get();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent e, boolean state) {
            scrollToEnd.set(state);
            if (state) {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        }
    }

    private class NocalhostOutputWindowPanel extends SimpleToolWindowPanel {

        public NocalhostOutputWindowPanel(boolean vertical) {
            super(vertical);
        }

        @Override
        public void setToolbar(@Nullable JComponent c) {
            Component myToolbar = super.getToolbar();
            if (c == null) {
                remove(myToolbar);
            }
            myToolbar = c;
            super.setToolbar(c);
            if (myToolbar instanceof ActionToolbar) {
                ((ActionToolbar) myToolbar).setOrientation(myVertical ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL);
            }

            if (c != null) {
                if (myVertical) {
                    add(c, BorderLayout.NORTH);
                } else {
                    add(c, BorderLayout.EAST);
                }
            }

            revalidate();
            repaint();
        }
    }
}
