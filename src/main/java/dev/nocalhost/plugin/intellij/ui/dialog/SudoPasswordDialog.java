package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBPasswordField;

import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.utils.TextUiUtil;
import lombok.Getter;

public class SudoPasswordDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JLabel messageLabel;
    private JBPasswordField passwordField;

    @Getter
    private String password = null;

    public SudoPasswordDialog(Project project, String command) {
        super(project);
        setTitle("Sudo Password");
        messageLabel.setText(command.trim() + " wants to make changes. Type your admin password to allow this.");
        TextUiUtil.setCutCopyPastePopup(passwordField);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected void doOKAction() {
        password = String.valueOf(passwordField.getPassword());
        super.doOKAction();
    }
}
