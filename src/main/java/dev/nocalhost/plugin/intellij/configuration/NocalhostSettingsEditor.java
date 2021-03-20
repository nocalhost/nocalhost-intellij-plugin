package dev.nocalhost.plugin.intellij.configuration;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NocalhostSettingsEditor extends SettingsEditor<RunConfiguration> {
    private JPanel contentPane;


    @Override
    protected void resetEditorFrom(@NotNull RunConfiguration s) {

    }

    @Override
    protected void applyEditorTo(@NotNull RunConfiguration s) throws ConfigurationException {

    }

    @Override
    protected @NotNull JComponent createEditor() {
        return contentPane;
    }
}
