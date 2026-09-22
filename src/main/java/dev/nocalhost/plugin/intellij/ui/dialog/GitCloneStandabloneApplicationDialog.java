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

public class GitCloneStandabloneApplicationDialog extends DialogWrapper {
    private JPanel contentPane;
    private JLabel gitUrlLabel;
    private JLabel branchLabel;
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
        setTitle(NocalhostI18n.get("dialog.configStandaloneGit"));
        gitUrlLabel.setText(NocalhostI18n.get("common.gitUrl"));
        branchLabel.setText(NocalhostI18n.get("prompt.whichBranchToDeploy"));
        defaultBranchRadioButton.setText(NocalhostI18n.get("common.defaultBranch"));
        inputTheBranchOfRadioButton.setText(NocalhostI18n.get("common.inputBranch"));

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
            return new ValidationInfo(NocalhostI18n.get("validation.gitUrl"), gitUrlTextField);
        }
        if (inputTheBranchOfRadioButton.isSelected()
                && !StringUtils.isNotEmpty(gitRefTextField.getText())) {
            return new ValidationInfo(NocalhostI18n.get("validation.gitBranch"), gitRefTextField);
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
