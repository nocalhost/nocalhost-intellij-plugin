package dev.nocalhost.plugin.intellij.configuration.go;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import icons.NocalhostIcons;

public class NocalhostGoConfigurationType implements ConfigurationType {
    @Override
    @NotNull
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getDisplayName() {
        return "Nocalhost Go";
    }

    @Override
    @Nls(capitalization = Nls.Capitalization.Sentence)
    public String getConfigurationTypeDescription() {
        return NocalhostI18n.get("config.go.description");
    }

    @Override
    public Icon getIcon() {
        return NocalhostIcons.ConfigurationLogo;
    }

    @Override
    @NotNull
    @NonNls
    public String getId() {
        return "NocalhostGoConfigurationType";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{new NocalhostGoConfigurationFactory(this)};
    }
}
