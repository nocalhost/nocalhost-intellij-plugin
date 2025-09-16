package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;

public class ApplicationSettingsConfigurable implements SearchableConfigurable {
    private static NocalhostSettings settings = ApplicationManager.getApplication().getService(NocalhostSettings.class);
    private JPanel panel;
    private JCheckBox checkNhctlVersion;

    public ApplicationSettingsConfigurable() {
        checkNhctlVersion.setSelected(settings.getCheckNhctlVersion());
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "dev.nocalhost.plugin.intellij.ui.ApplicationSettingsConfigurable";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Nocalhost";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return panel;
    }

    @Override
    public boolean isModified() {
        return settings.getCheckNhctlVersion() != checkNhctlVersion.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        settings.setCheckNhctlVersion(checkNhctlVersion.isSelected());
    }
}
