package dev.nocalhost.plugin.intellij.ui;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.ui.FormBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.utils.FileChooseUtil;

public class NocalhostSettingComponent {
    private final JPanel settingPanel;
    private final JBTextField nhctlField;
    private final TextFieldWithBrowseButton nhctlBinary;
    private final JButton nhctlTestButton;
    private final JPanel nhctlPanel;
    private final JBTextField kubeField;
    private final TextFieldWithBrowseButton kubectlBinary;
    private final JButton kubectlTestButton;
    private final JPanel kubectlPanel;
    private final JBTextArea showVersion;


    public NocalhostSettingComponent() {
        nhctlField = new JBTextField();
        nhctlField.getEmptyText().appendText("nhctl");
        nhctlBinary = new TextFieldWithBrowseButton(nhctlField);
        kubeField = new JBTextField();
        kubeField.getEmptyText().appendText("kubectl");
        kubectlBinary = new TextFieldWithBrowseButton(kubeField);
        nhctlBinary.addBrowseFolderListener("", "Select nhctl binary", null,
                FileChooseUtil.singleFileChooserDescriptor(),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        kubectlBinary.addBrowseFolderListener("", "Select kubectl binary", null,
                FileChooseUtil.singleFileChooserDescriptor(),
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
        nhctlTestButton = new JButton("Test");
        nhctlTestButton.addActionListener(new TestNhctlListener());
        nhctlPanel = new JPanel(new BorderLayout());
        nhctlPanel.add(nhctlBinary, BorderLayout.CENTER);
        nhctlPanel.add(nhctlTestButton, BorderLayout.EAST);
        kubectlTestButton = new JButton("Test");
        kubectlTestButton.addActionListener(new TestKubectlListener());
        kubectlPanel = new JPanel(new BorderLayout());
        kubectlPanel.add(kubectlBinary, BorderLayout.CENTER);
        kubectlPanel.add(kubectlTestButton, BorderLayout.EAST);

        showVersion = new JBTextArea();
        showVersion.setColumns(20);
        showVersion.setLineWrap(true);
        showVersion.setWrapStyleWord(true);
        showVersion.setEnabled(false);
        showVersion.setVisible(false);

        settingPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("nhctl: "), nhctlPanel, 1, false)
                .addLabeledComponent(new JBLabel("kubectl: "), kubectlPanel, 1, false)
                .addComponent(showVersion)
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

    private class TestNhctlListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String nhctl = StringUtils.isBlank(nhctlBinary.getText()) ? "nhctl" : nhctlBinary.getText();
            final Map<String, String> environment = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
            environment.put("DISABLE_SPINNER", "true");
            if (SystemInfo.isMac || SystemInfo.isLinux) {
                if (StringUtils.contains(nhctl, "/")) {
                    String path = environment.get("PATH");
                    path = nhctl.substring(0, nhctl.lastIndexOf("/")) + ":" + path;
                    environment.put("PATH", path);
                }
            }
            GeneralCommandLine commandLine = new GeneralCommandLine().withEnvironment(environment);
            commandLine.setExePath(nhctl);
            commandLine.addParameter("version");
            commandLine.setCharset(CharsetToolkit.getDefaultSystemCharset());

            try {
                Process process = commandLine.createProcess();
                String output = CharStreams.toString(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
                showVersion.setText(output);
            } catch (IOException | ExecutionException ioException) {
                showVersion.setText(ioException.getMessage());
            } finally {
                showVersion.setVisible(true);
            }
        }
    }

    private class TestKubectlListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String kubectl = StringUtils.isBlank(kubectlBinary.getText()) ? "kubectl" : kubectlBinary.getText();
            final Map<String, String> environment = new HashMap<>(EnvironmentUtil.getEnvironmentMap());
            environment.put("DISABLE_SPINNER", "true");
            if (SystemInfo.isMac || SystemInfo.isLinux) {
                if (StringUtils.contains(kubectl, "/")) {
                    String path = environment.get("PATH");
                    path = kubectl.substring(0, kubectl.lastIndexOf("/")) + ":" + path;
                    environment.put("PATH", path);
                }
            }
            GeneralCommandLine commandLine = new GeneralCommandLine().withEnvironment(environment);
            commandLine.setExePath(kubectl);
            commandLine.addParameter("version");
            commandLine.setCharset(CharsetToolkit.getDefaultSystemCharset());
            try {
                Process process = commandLine.createProcess();
                String output = CharStreams.toString(new InputStreamReader(process.getInputStream(), Charsets.UTF_8));
                showVersion.setText(output);
            } catch (IOException | ExecutionException ioException) {
                showVersion.setText(ioException.getMessage());
            } finally {
                showVersion.setVisible(true);
            }
        }
    }
}
