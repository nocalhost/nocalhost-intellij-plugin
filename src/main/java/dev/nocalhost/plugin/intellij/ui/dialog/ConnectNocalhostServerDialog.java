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
        if (StringUtils.isEmpty(getServer())) {
            return new ValidationInfo("Server cannot be empty", serverTextField);
        }
        if (StringUtils.isEmpty(getUsername())) {
            return new ValidationInfo("Email address cannot be empty", usernameTextField);
        }
        if (StringUtils.isEmpty(getPassword())) {
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
        setOKActionEnabled(false);
        ProgressManager.getInstance().run(new ConnectNocalhostServerTask(
                project,
                getServer(),
                getUsername(),
                getPassword(),
                () -> close(OK_EXIT_CODE),
                () -> setOKActionEnabled(true))
        );
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
        String text = textField.getText().trim();
        if (!text.isEmpty() && !text.startsWith("/") && !text.startsWith("http:") && !text.startsWith("https:")) {
            text = "http" + "://" + text;
        }
        if (text.endsWith("/") && !text.endsWith(":/") && !text.endsWith("://")) {
            text = text.substring(0, text.length() - 1);
        }
        textField.setText(text);
    }
}
