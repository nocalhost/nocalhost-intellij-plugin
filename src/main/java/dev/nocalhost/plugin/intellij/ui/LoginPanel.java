package dev.nocalhost.plugin.intellij.ui;

import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class LoginPanel {

    private final JPanel panel;
    private final JLabel hostLabel;
    private final JBTextField hostTextField;
    private final JLabel emailLabel;
    private final JTextField emailTextField;
    private final JLabel passwordLabel;
    private final JPasswordField passwordField;

    public LoginPanel(final LoginDialog dialog) {

        panel = new JPanel(new GridLayout(3, 1));

        hostLabel = new JBLabel("Host: ");
        hostTextField = new JBTextField();
        emailLabel = new JBLabel("Email: ");
        emailTextField = new JTextField();
        passwordLabel = new JBLabel("Password: ");
        passwordField = new JPasswordField();
        panel.add(hostLabel);
        panel.add(hostTextField);
        panel.add(emailLabel);
        panel.add(emailTextField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.setVisible(true);


        hostTextField.getEmptyText().setText("http://106.55.223.21:8080/");

        hostTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                LoginPanel.fixUrl(hostTextField);
            }
        });
        DocumentListener listener = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                dialog.clearErrors();
            }
        };
        emailTextField.getDocument().addDocumentListener(listener);
        passwordField.getDocument().addDocumentListener(listener);
    }

    public JComponent getPanel() {
        return panel;
    }

    public void setHost(final String host) {
        hostTextField.setText(host);
    }

    public void setEmail(final String login) {
        emailTextField.setText(login);
    }

    public void setPassword(final String password) {
        passwordField.setText(password);
    }

    public String getHost() {
        return hostTextField.getText().trim();
    }

    public String getEmail() {
        return emailTextField.getText().trim();
    }

    public String getPassword() {
        return String.valueOf(passwordField.getPassword());
    }

    public JComponent getPreferrableFocusComponent() {
        return hostTextField.getText().isEmpty() ? hostTextField : emailTextField;
    }

    public static void fixUrl(JTextField textField) {
        String text = textField.getText();
        if (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        if (!text.isEmpty() && !text.contains("://")) {
            text = "http://" + text;
        }
        textField.setText(text);
    }
}
