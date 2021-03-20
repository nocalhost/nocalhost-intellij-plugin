package dev.nocalhost.plugin.intellij.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import icons.NocalhostIcons;

public class NocalhostConfigurationType implements ConfigurationType {
    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Nocalhost";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getConfigurationTypeDescription() {
        return "Nocalhost Configuration Type";
    }

    @Override
    public Icon getIcon() {
        return NocalhostIcons.LogoColorful;
    }

    @Override
    public @NotNull
    @NonNls
    String getId() {
        return "NocalhostConfiguration";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new NocalhostConfigurationFactory(this)};
    }
}
