package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.utils.TextUiUtil;

public class KustomizePathDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JLabel specifyLabel;
    private JRadioButton useDefaultValuesRadioButton;
    private JRadioButton specifyOneRadioButton;
    private JBTextField specifyTextField;

    private String specifyPath;

    public KustomizePathDialog(Project project) {
        super(project, true);
        init();
        setTitle(NocalhostI18n.get("dialog.selectKustomizePath"));
        specifyLabel.setText(NocalhostI18n.get("kustomize.specify"));
        useDefaultValuesRadioButton.setText(NocalhostI18n.get("kustomize.useDefault"));
        specifyOneRadioButton.setText(NocalhostI18n.get("kustomize.specifyOne"));

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(useDefaultValuesRadioButton);
        buttonGroup.add(specifyOneRadioButton);

        useDefaultValuesRadioButton.addChangeListener(e -> updateComponentEnabled());
        specifyOneRadioButton.addChangeListener(e -> updateComponentEnabled());

        useDefaultValuesRadioButton.setSelected(true);
        specifyOneRadioButton.setSelected(false);
        specifyTextField.getEmptyText().appendText(NocalhostI18n.get("kustomize.pathHint"));

        TextUiUtil.setCutCopyPastePopup(specifyTextField);
    }

    private void updateComponentEnabled() {
        specifyTextField.setEnabled(specifyOneRadioButton.isSelected());
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (specifyOneRadioButton.isSelected() && StringUtils.isBlank(specifyTextField.getText())) {
            return new ValidationInfo(NocalhostI18n.get("validation.kustomizePath"), specifyTextField);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected void doOKAction() {
        if (useDefaultValuesRadioButton.isSelected()) {
            specifyPath = "";
        } else {
            specifyPath = specifyTextField.getText();
        }
        super.doOKAction();

    }

    public String getSpecifyPath() {
        return specifyPath;
    }
}
