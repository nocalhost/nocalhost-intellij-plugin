package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

public class NocalhostWindow {

    public SimpleToolWindowPanel createToolWindowContent(final Project project) {
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);
        ActionToolbar toolbar = createToolbar(project);
        panel.setToolbar(toolbar.getComponent());

        JLabel label = new JLabel();
        label.setText("Nocalhost");
        panel.add(label, BorderLayout.NORTH);
        panel.setVisible(true);
        return panel;
    }

}
