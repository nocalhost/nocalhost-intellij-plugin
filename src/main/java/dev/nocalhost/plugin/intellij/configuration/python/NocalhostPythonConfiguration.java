package dev.nocalhost.plugin.intellij.configuration.python;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.debugger.remote.PyRemoteDebugConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.configuration.NocalhostConfiguration;
import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;

public class NocalhostPythonConfiguration extends PyRemoteDebugConfiguration implements NocalhostConfiguration {
    protected NocalhostPythonConfiguration(@NotNull Project project,
                                           @NotNull ConfigurationFactory factory,
                                           String name) {
        super(project, factory, name);
    }

    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) {
        if (StringUtils.equals(DefaultDebugExecutor.EXECUTOR_ID, executor.getId())) {
            return new NocalhostPythonProfileState(this.getProject(), env);
        }
        return new NocalhostProfileState(env);
    }
}
