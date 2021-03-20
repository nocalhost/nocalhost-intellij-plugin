package dev.nocalhost.plugin.intellij.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class NocalhostConfigurationFactory extends ConfigurationFactory {
    private static final String FACTORY_NAME = "Nocalhost configuration factory";

    public NocalhostConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new NocalhostConfiguration(project, this, "Nocalhost");
    }

    @Override
    public @NotNull
    @NonNls
    String getId() {
        return FACTORY_NAME;
    }

    @Override
    public @NotNull @Nls String getName() {
        return FACTORY_NAME;
    }
}
