package dev.nocalhost.plugin.intellij.ui;

import javax.swing.*;

public class NocalhostWindowLoginPanel {

    private JTextPane loginText;
    private JButton loginButton;
    private JPanel loginPanel;
    private JPanel panel;
    private JPanel mainPanel;


    public NocalhostWindowLoginPanel() {
        loginText.setEditable(false);
        loginButton.addActionListener(e -> new LoginDialog().showAndGet());
    }

    public JPanel getPanel() {
        return mainPanel;
    }
}
