package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.ui.AppInstallOrUpgradeOption;

public class AppInstallOrUpgradeOptionDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JLabel messageLabel;
    private JRadioButton defaultRadioButton;
    private JRadioButton specifyOneRadioButton;
    private JBTextField specifyOneTextField;

    private boolean specifyOneSelected;
    private String specifyText;

    private final String specifyOneValidationMessage;

    public AppInstallOrUpgradeOptionDialog(
            Project project,
            String title,
            String messageLabelText,
            String defaultRadioButtonText,
            String specifyOneTextFieldPlaceHolder,
            String specifyOneValidationMessage) {
        super(project);
        init();
        setTitle(title);

        this.specifyOneValidationMessage = specifyOneValidationMessage;

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(defaultRadioButton);
        buttonGroup.add(specifyOneRadioButton);

        specifyOneRadioButton.addChangeListener(e -> {
            specifyOneTextField.setEnabled(specifyOneRadioButton.isSelected());
            if (specifyOneRadioButton.isSelected()) {
                specifyOneTextField.grabFocus();
            }
        });

        specifyOneTextField.setEnabled(false);
        defaultRadioButton.setSelected(true);

        messageLabel.setText(messageLabelText);
        defaultRadioButton.setText(defaultRadioButtonText);
        specifyOneTextField.getEmptyText().appendText(specifyOneTextFieldPlaceHolder);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (specifyOneRadioButton.isSelected() && !StringUtils.isNotEmpty(specifyOneTextField.getText())) {
            return new ValidationInfo(specifyOneValidationMessage, specifyOneTextField);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        specifyOneSelected = specifyOneRadioButton.isSelected();
        specifyText = specifyOneTextField.getText();

        super.doOKAction();
    }

    public AppInstallOrUpgradeOption getAppInstallOrUpgradeOption() {
        return new AppInstallOrUpgradeOption(specifyOneSelected, specifyText);
    }
}
