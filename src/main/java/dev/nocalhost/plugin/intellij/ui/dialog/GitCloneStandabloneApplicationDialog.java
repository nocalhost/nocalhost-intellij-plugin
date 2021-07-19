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

public class GitCloneStandabloneApplicationDialog extends DialogWrapper {
    private JPanel contentPane;
    private JRadioButton defaultBranchRadioButton;
    private JRadioButton inputTheBranchOfRadioButton;
    private JBTextField gitUrlTextField;
    private JBTextField gitRefTextField;

    @Getter
    private String gitUrl;
    @Getter
    private String gitRef;

    public GitCloneStandabloneApplicationDialog(Project project) {
        super(project, true);
        setTitle("Config Standalone Application via Git Repository");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(defaultBranchRadioButton);
        buttonGroup.add(inputTheBranchOfRadioButton);

        defaultBranchRadioButton.addChangeListener(e -> updateComponentEnabled());
        inputTheBranchOfRadioButton.addChangeListener(e -> updateComponentEnabled());

        defaultBranchRadioButton.setSelected(true);
        inputTheBranchOfRadioButton.setSelected(false);

        TextUiUtil.setCutCopyPastePopup(gitUrlTextField, gitRefTextField);

        init();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!StringUtils.isNotEmpty(gitUrlTextField.getText())) {
            return new ValidationInfo("Must specify git URL", gitUrlTextField);
        }
        if (inputTheBranchOfRadioButton.isSelected()
                && !StringUtils.isNotEmpty(gitRefTextField.getText())) {
            return new ValidationInfo("Must specify git branch", gitRefTextField);
        }
        return null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        gitUrl = gitUrlTextField.getText();
        gitRef = inputTheBranchOfRadioButton.isSelected() ? gitRefTextField.getText() : "";
        super.doOKAction();
    }

    private void updateComponentEnabled() {
        gitRefTextField.setEnabled(inputTheBranchOfRadioButton.isSelected());
    }

}
