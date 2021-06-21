package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.utils.TextUiUtil;
import lombok.Getter;

public class InstallStandaloneHelmRepoApplicationDialog extends DialogWrapper {
    private JPanel contentPane;
    private JRadioButton defaultVersionRadioButton;
    private JRadioButton inputTheVersionOfRadioButton;
    private JBTextField nameTextField;
    private JBTextField chartUrlTextField;
    private JBTextField versionTextField;

    @Getter
    private String name;
    @Getter
    private String chartUrl;
    @Getter
    private String version;

    public InstallStandaloneHelmRepoApplicationDialog(Project project) {
        super(project, true);
        setTitle("Install Standalone Helm Repo Application");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(defaultVersionRadioButton);
        buttonGroup.add(inputTheVersionOfRadioButton);

        defaultVersionRadioButton.addChangeListener(e -> updateComponentEnabled());
        inputTheVersionOfRadioButton.addChangeListener(e -> updateComponentEnabled());

        defaultVersionRadioButton.setSelected(true);
        inputTheVersionOfRadioButton.setSelected(false);

        TextUiUtil.setCutCopyPastePopup(nameTextField, chartUrlTextField, versionTextField);

        init();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!StringUtils.isNotEmpty(nameTextField.getText())) {
            return new ValidationInfo("Must specify application name", nameTextField);
        }
        if (!StringUtils.isNotEmpty(chartUrlTextField.getText())) {
            return new ValidationInfo("Must specify chart url", chartUrlTextField);
        }
        if (inputTheVersionOfRadioButton.isSelected()
                && !StringUtils.isNotEmpty(versionTextField.getText())) {
            return new ValidationInfo("Must specify chart version", versionTextField);
        }
        return null;
    }

    private void updateComponentEnabled() {
        versionTextField.setEnabled(inputTheVersionOfRadioButton.isSelected());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        name = nameTextField.getText();
        chartUrl = chartUrlTextField.getText();
        version = inputTheVersionOfRadioButton.isSelected() ? versionTextField.getText() : "";
        super.doOKAction();
    }
}
