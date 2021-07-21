package dev.nocalhost.plugin.intellij.configuration.php;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class NocalhostPhpConfigurationFactory extends ConfigurationFactory {
    protected NocalhostPhpConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new NocalhostPhpConfiguration(project, this);
    }

    @Override
    @NotNull
    @NonNls
    public String getId() {
        return getType().getDisplayName();
    }
}
