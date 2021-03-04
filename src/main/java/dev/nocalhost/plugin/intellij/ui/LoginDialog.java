package dev.nocalhost.plugin.intellij.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;

public class LoginDialog extends DialogWrapper {
    private JPanel loginPanel;
    private JBTextField hostTextField;
    private JBTextField emailTextField;
    private JBPasswordField passwordField;

    public LoginDialog() {
        super(true);

        setTitle("Login Nocalhost Server");

        hostTextField.getEmptyText().appendText("Input your api server url");
        emailTextField.getEmptyText().appendText("Input your email");
        passwordField.getEmptyText().appendText("Input your password");

        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        if (StringUtils.isNotEmpty(nocalhostSettings.getBaseUrl())) {
            hostTextField.setText(nocalhostSettings.getBaseUrl());
        }

        hostTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                fixUrl(hostTextField);
            }
        });
        DocumentListener listener = new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                clearErrors();
            }
        };
        emailTextField.getDocument().addDocumentListener(listener);
        passwordField.getDocument().addDocumentListener(listener);
        setOKButtonText("Login");
        init();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!StringUtils.isNotEmpty(getHost())) {
            return new ValidationInfo("Server url cannot be empty", hostTextField);
        }
        if (!StringUtils.isNotEmpty(getEmail())) {
            return new ValidationInfo("Email cannot be empty", emailTextField);
        }
        if (!StringUtils.isNotEmpty(getPassword())) {
            return new ValidationInfo("Password cannot be empty", passwordField);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return getPanel();
    }

    @Override
    @NotNull
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
    }

    @Override
    protected void doHelpAction() {
        BrowserUtil.browse("https://nocalhost.dev");
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return hostTextField.getText().isEmpty() ? hostTextField : emailTextField;
    }

    @Override
    protected void doOKAction() {
        final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
        try {
            nocalhostApi.login(getHost(), getEmail(), getPassword());
            super.doOKAction();
        } catch (Exception e) {
            if (e instanceof NocalhostApiException && ((NocalhostApiException) e).getCode() == 200) {
                setErrorText(((NocalhostApiException) e).getMsg());
            } else {
                setErrorText(e.getMessage());
            }
        }
    }

    public void clearErrors() {
        setErrorText(null);
    }

    public JComponent getPanel() {
        return loginPanel;
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
