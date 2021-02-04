package dev.nocalhost.plugin.intellij.ui;

import javax.swing.*;

public class LoginPanel {

    private JTextPane loginText;
    private JButton loginButton;
    private JPanel loginPanel;
    private JPanel panel;
    private JPanel mainPanel;


    public LoginPanel() {
        loginButton.addActionListener(e -> new LoginDialog().showAndGet());
    }

    public JPanel getPanel() {
        return mainPanel;
    }
}
