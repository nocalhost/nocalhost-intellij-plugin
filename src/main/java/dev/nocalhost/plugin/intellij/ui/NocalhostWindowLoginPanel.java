package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.project.Project;

import javax.swing.*;

public class NocalhostWindowLoginPanel {

    private JTextPane loginText;
    private JButton loginButton;
    private JPanel loginPanel;
    private JPanel panel;
    private JPanel mainPanel;


    public NocalhostWindowLoginPanel(Project project) {
        loginText.setEditable(false);
        loginButton.addActionListener(e -> new ConnectNocalhostServerDialog(project).showAndGet());
    }

    public JPanel getPanel() {
        return mainPanel;
    }
}
