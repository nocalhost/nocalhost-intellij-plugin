package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.task.ConnectNocalhostServerTask;
import dev.nocalhost.plugin.intellij.utils.TextUiUtil;

public class ConnectNocalhostServerDialog extends DialogWrapper {
    private final Project project;

    private JPanel dialogPanel;
    private JBTextField serverTextField;
    private JBTextField usernameTextField;
    private JBPasswordField passwordField;

    public ConnectNocalhostServerDialog(Project project) {
        super(true);
        this.project = project;

        setTitle("Connect to Nocalhost Server");

        serverTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                fixUrl(serverTextField);
            }
        });
        setOKButtonText("Connect");

        TextUiUtil.setCutCopyPastePopup(serverTextField, usernameTextField, passwordField);

        init();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!StringUtils.isNotEmpty(getServer())) {
            return new ValidationInfo("Server cannot be empty", serverTextField);
        }
        if (!StringUtils.isNotEmpty(getUsername())) {
            return new ValidationInfo("Username cannot be empty", usernameTextField);
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
        return serverTextField.getText().isEmpty() ? serverTextField : usernameTextField;
    }

    @Override
    protected void doOKAction() {
        ProgressManager.getInstance().run(new ConnectNocalhostServerTask(project, getServer(),
                getUsername(), getPassword()));
        super.doOKAction();
    }

    public JComponent getPanel() {
        return dialogPanel;
    }

    public String getServer() {
        return serverTextField.getText().trim();
    }

    public String getUsername() {
        return usernameTextField.getText().trim();
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
