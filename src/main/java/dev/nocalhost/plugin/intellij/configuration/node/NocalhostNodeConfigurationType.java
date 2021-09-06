package dev.nocalhost.plugin.intellij.configuration.node;

import com.intellij.openapi.project.Project;
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy;
import com.intellij.execution.configurations.ConfigurationFactory;

import icons.NocalhostIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import com.intellij.execution.RunConfigurationConverter;
import com.intellij.openapi.project.DumbAware;
import com.intellij.execution.configurations.ConfigurationTypeBase;

public final class NocalhostNodeConfigurationType extends ConfigurationTypeBase implements DumbAware, RunConfigurationConverter
{
    public NocalhostNodeConfigurationType() {
        super("NocalhostNodeConfigurationType", "Nocalhost Node.js", null, NocalhostIcons.ConfigurationLogo);
        this.addFactory(new ConfigurationFactory(this) {
            @NotNull
            public RunConfigurationSingletonPolicy getSingletonPolicy() {
                return RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY;
            }

            @NotNull
            public String getId() {
                return getType().getDisplayName();
            }

            @NotNull
            public NocalhostNodeConfiguration createTemplateConfiguration(@NotNull final Project project) {
                return new NocalhostNodeConfiguration(project, this, getId());
            }
        });
    }

    @Override
    public boolean convertRunConfigurationOnDemand(@NotNull Element element) {
        return false;
    }
}
