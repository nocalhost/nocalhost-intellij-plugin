package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;

public class NocalhostConfigurable implements Configurable {
    private NocalhostSettingComponent nocalhostSettingComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Nocalhost";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return nocalhostSettingComponent.getPreferredFocusedComponent();
    }

    @Override
    public @Nullable JComponent createComponent() {
        nocalhostSettingComponent = new NocalhostSettingComponent();
        return nocalhostSettingComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        boolean modified = !nocalhostSettingComponent.getNhctl().equals(nocalhostSettings.getNhctlBinary());
        modified |= !nocalhostSettingComponent.getKubectl().equals(nocalhostSettings.getKubectlBinary());
        return modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        nocalhostSettings.setNhctlBinary(nocalhostSettingComponent.getNhctl());
        nocalhostSettings.setKubectlBinary(nocalhostSettingComponent.getKubectl());
    }

    @Override
    public void reset() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        nocalhostSettingComponent.setNhctl(nocalhostSettings.getNhctlBinary());
        nocalhostSettingComponent.setKubectl(nocalhostSettings.getKubectlBinary());
    }

    @Override
    public void disposeUIResources() {
        nocalhostSettingComponent = null;
    }
}
