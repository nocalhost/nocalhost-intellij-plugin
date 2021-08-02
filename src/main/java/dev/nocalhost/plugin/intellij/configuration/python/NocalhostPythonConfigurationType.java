package dev.nocalhost.plugin.intellij.configuration.python;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import icons.NocalhostIcons;

public class NocalhostPythonConfigurationType implements ConfigurationType {
    @Override
    @NotNull
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getDisplayName() {
        return "Nocalhost Python";
    }

    @Override
    @Nls(capitalization = Nls.Capitalization.Sentence)
    public String getConfigurationTypeDescription() {
        return "Run and debug Python application with nocalhost";
    }

    @Override
    public Icon getIcon() {
        return NocalhostIcons.ConfigurationLogo;
    }

    @Override
    @NotNull
    @NonNls
    public String getId() {
        return "NocalhostPythonConfigurationType";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new NocalhostPythonConfigurationFactory(this)};
    }
}
