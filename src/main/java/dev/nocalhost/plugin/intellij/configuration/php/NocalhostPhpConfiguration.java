package dev.nocalhost.plugin.intellij.configuration.php;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.jetbrains.php.run.remoteDebug.PhpRemoteDebugRunConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.nocalhost.plugin.intellij.configuration.NocalhostConfiguration;
import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;
import dev.nocalhost.plugin.intellij.configuration.NocalhostSettingsEditor;

public class NocalhostPhpConfiguration
//        extends LocatableConfigurationBase<NocalhostPhpConfiguration>
        extends PhpRemoteDebugRunConfiguration
        implements NocalhostConfiguration {
    protected NocalhostPhpConfiguration(@NotNull Project project,
                                       @NotNull ConfigurationFactory factory) {
        super(project, factory, "");
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new NocalhostSettingsEditor();
    }

    @Override
    public @Nullable RunProfileState getState(
            @NotNull Executor executor,
            @NotNull ExecutionEnvironment environment) {
        return new NocalhostProfileState(environment);
    }
}
