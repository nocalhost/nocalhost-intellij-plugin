package dev.nocalhost.plugin.intellij.configuration.go;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class NocalhostGoConfigurationFactory extends ConfigurationFactory {
    protected NocalhostGoConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new NocalhostGoConfiguration(project, this);
    }

    @Override
    @NotNull
    @NonNls
    public String getId() {
        return "NocalhostGoConfigurationFactory";
    }
}
