package dev.nocalhost.plugin.intellij.configuration.node;

import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.javascript.nodejs.debug.NodeDebugRunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.NotNullFactory;
import com.intellij.javascript.debugger.JSDebuggerBundle;
import org.jdom.Attribute;
import java.util.ListIterator;

import icons.NocalhostIcons;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import com.intellij.execution.RunConfigurationConverter;
import com.intellij.openapi.project.DumbAware;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.jetbrains.debugger.wip.JSRemoteDebugConfiguration;

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
