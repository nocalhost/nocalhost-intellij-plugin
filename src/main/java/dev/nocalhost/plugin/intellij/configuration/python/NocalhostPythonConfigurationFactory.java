package dev.nocalhost.plugin.intellij.configuration.python;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class NocalhostPythonConfigurationFactory extends ConfigurationFactory {
    protected NocalhostPythonConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new NocalhostPythonConfiguration(project, this, project.getBasePath());
    }

    @Override
    @NotNull
    @NonNls
    public String getId() {
        return getType().getDisplayName();
    }
}
