package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NocalhostSettingComponent {
    private final JPanel settingPanel;
    private final TextFieldWithBrowseButton nhctlBinary;
    private final TextFieldWithBrowseButton kubectlBinary;


    public NocalhostSettingComponent() {
        nhctlBinary = new TextFieldWithBrowseButton(new JBTextField());
        kubectlBinary = new TextFieldWithBrowseButton(new JBTextField());
        nhctlBinary.addBrowseFolderListener("", "Select nhctl binary", null,
                new FileChooserDescriptor(false, true, false, false, false, false),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        kubectlBinary.addBrowseFolderListener("", "Select kubectl binary", null,
                new FileChooserDescriptor(false, true, false, false, false, false),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        settingPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("nhctl: "), nhctlBinary, 1, false)
                .addLabeledComponent(new JBLabel("kubectl: "), kubectlBinary, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return settingPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return nhctlBinary;
    }

    @NotNull
    public String getNhctl() {
        return nhctlBinary.getText();
    }

    public void setNhctl(String nhctl) {
        nhctlBinary.setText(nhctl);
    }

    @NotNull
    public String getKubectl() {
        return kubectlBinary.getText();
    }

    public void setKubectl(String kubectl) {
        kubectlBinary.setText(kubectl);
    }
}
