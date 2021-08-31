package dev.nocalhost.plugin.intellij.configuration.node;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;

import com.jetbrains.debugger.wip.JSRemoteDebugConfiguration;

import dev.nocalhost.plugin.intellij.configuration.NocalhostConfiguration;
import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;
import dev.nocalhost.plugin.intellij.configuration.NocalhostSettingsEditor;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.execution.ExecutionResult;
import com.intellij.xdebugger.XDebugSession;
import java.net.InetSocketAddress;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.options.SettingsEditor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.debugger.DebuggableRunConfiguration;
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.execution.configurations.LocatableConfigurationBase;

public class NocalhostNodeConfiguration
        extends LocatableConfigurationBase<NocalhostNodeConfiguration>
        implements RunConfigurationWithSuppressedDefaultRunAction, JSRunProfileWithCompileBeforeLaunchOption, DebuggableRunConfiguration, NocalhostConfiguration
{
    private JSRemoteDebugConfiguration conf;

    public int getPort() {
        return conf.getPort();
    }

    public void setPort(final int value) {
        conf.setPort(value);
    }

    @NotNull
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new NocalhostSettingsEditor();
    }

    @NotNull
    public RunProfileState getState(@NotNull final Executor executor, @NotNull final ExecutionEnvironment env) {
        return new NocalhostProfileState(env);
    }

    @NotNull
    public InetSocketAddress computeDebugAddress(@NotNull final RunProfileState state) {
        return conf.computeDebugAddress(state);
    }

    @NotNull
    public XDebugProcess createDebugProcess(@NotNull final InetSocketAddress socketAddress, @NotNull final XDebugSession session, @Nullable final ExecutionResult executionResult, @NotNull final ExecutionEnvironment environment) {
        return conf.createDebugProcess(socketAddress, session, executionResult, environment);
    }

    public NocalhostNodeConfiguration(@NotNull final Project project, @NotNull final ConfigurationFactory factory, @NotNull final String name) {
        super(project, factory, name);
        this.conf = new JSRemoteDebugConfiguration(project, factory, name);
        this.conf.setHost("127.0.0.1");
    }
}

