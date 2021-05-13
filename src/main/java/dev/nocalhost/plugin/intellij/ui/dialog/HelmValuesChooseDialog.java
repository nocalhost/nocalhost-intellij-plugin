package dev.nocalhost.plugin.intellij.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextArea;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.ui.HelmValuesChooseState;
import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;

public class HelmValuesChooseDialog extends DialogWrapper {
    private JPanel dialogPanel;
    private JRadioButton useDefaultValuesRadioButton;
    private JRadioButton specifyValuesYamlRadioButton;
    private JRadioButton specifyValuesRadioButton;
    private TextFieldWithBrowseButton specifyValuesYamlTextField;
    private JBTextArea specifyValuesTextArea;

    private boolean specifyValuesYamlSelected;
    private String specifyValuesYamlPath;
    private boolean specifyValuesSelected;
    private Map<String, String> specifyValues;

    public HelmValuesChooseDialog(Project project) {
        super(true);
        init();
        setTitle("Select Helm Values");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(useDefaultValuesRadioButton);
        buttonGroup.add(specifyValuesYamlRadioButton);
        buttonGroup.add(specifyValuesRadioButton);

        useDefaultValuesRadioButton.addChangeListener(e -> updateComponentEnabled());
        specifyValuesYamlRadioButton.addChangeListener(e -> updateComponentEnabled());
        specifyValuesRadioButton.addChangeListener(e -> updateComponentEnabled());

        useDefaultValuesRadioButton.setSelected(true);
        specifyValuesYamlTextField.setEnabled(false);
        specifyValuesTextArea.setEnabled(false);

        specifyValuesTextArea.setLineWrap(true);
        specifyValuesTextArea.setWrapStyleWord(true);

        specifyValuesYamlTextField.addBrowseFolderListener("Select the value file path", "", project, FileChooseUtil.singleFileChooserDescriptor());

        specifyValuesTextArea.getEmptyText().appendText("eg: key1=val1,key2=val2");
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPanel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (specifyValuesYamlRadioButton.isSelected() && !StringUtils.isNotEmpty(specifyValuesYamlTextField.getText())) {
            return new ValidationInfo("Must specify a values.yaml file.", specifyValuesYamlTextField);
        }
        if (specifyValuesRadioButton.isSelected()) {
            if (StringUtils.isNotEmpty(specifyValuesTextArea.getText())) {
                for (String entry : specifyValuesTextArea.getText().split(",")) {
                    if (entry.split("=").length != 2) {
                        return new ValidationInfo("Values format cannot be accepted");
                    }
                }
            } else {
                return new ValidationInfo("Values cannot be empty");
            }
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        specifyValuesYamlSelected = specifyValuesYamlRadioButton.isSelected();
        if (specifyValuesYamlSelected) {
            specifyValuesYamlPath = specifyValuesYamlTextField.getText();
        }

        specifyValuesSelected = specifyValuesRadioButton.isSelected();
        if (specifyValuesSelected && StringUtils.isNotEmpty(specifyValuesTextArea.getText())) {
            specifyValues = Arrays.stream(specifyValuesTextArea.getText().split(","))
                    .map(e -> e.split("="))
                    .collect(Collectors.toMap(e -> e[0].trim(), e -> e[1].trim()));
        }

        super.doOKAction();
    }

    private void updateComponentEnabled() {
        specifyValuesYamlTextField.setEnabled(specifyValuesYamlRadioButton.isSelected());

        specifyValuesTextArea.setEnabled(specifyValuesRadioButton.isSelected());
        if (specifyValuesRadioButton.isSelected()) {
            specifyValuesTextArea.grabFocus();
        }
    }

    public HelmValuesChooseState getHelmValuesChooseState() {
        return new HelmValuesChooseState(
                specifyValuesYamlSelected,
                specifyValuesYamlPath,
                specifyValuesSelected,
                specifyValues);
    }
}
