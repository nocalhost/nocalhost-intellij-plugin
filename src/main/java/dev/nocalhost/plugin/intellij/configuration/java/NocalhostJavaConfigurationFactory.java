package dev.nocalhost.plugin.intellij.configuration.java;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

public class NocalhostJavaConfigurationFactory extends ConfigurationFactory {

    protected NocalhostJavaConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new NocalhostJavaConfiguration(project, this);
    }
}
