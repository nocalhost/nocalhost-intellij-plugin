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
import lombok.Getter;

public class ConfigStandaloneHelmRepoApplicationDialog extends DialogWrapper {
    private JPanel contentPane;
    private JLabel nameLabel;
    private JLabel chartUrlLabel;
    private JLabel versionLabel;
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

    public ConfigStandaloneHelmRepoApplicationDialog(Project project) {
        super(project, true);

        setTitle(NocalhostI18n.get("dialog.configStandaloneHelm"));
        nameLabel.setText(NocalhostI18n.get("common.name"));
        chartUrlLabel.setText(NocalhostI18n.get("helm.chartUrl"));
        versionLabel.setText(NocalhostI18n.get("prompt.whichVersionToInstall"));
        defaultVersionRadioButton.setText(NocalhostI18n.get("common.defaultVersion"));
        inputTheVersionOfRadioButton.setText(NocalhostI18n.get("common.inputChartVersion"));

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
            return new ValidationInfo(NocalhostI18n.get("validation.appName"), nameTextField);
        }
        if (!StringUtils.isNotEmpty(chartUrlTextField.getText())) {
            return new ValidationInfo(NocalhostI18n.get("validation.chartUrl"), chartUrlTextField);
        }
        if (inputTheVersionOfRadioButton.isSelected()
                && !StringUtils.isNotEmpty(versionTextField.getText())) {
            return new ValidationInfo(NocalhostI18n.get("validation.chartVersion"), versionTextField);
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
