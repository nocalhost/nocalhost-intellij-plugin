package dev.nocalhost.plugin.intellij.configuration.python;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.PathMappingSettings;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.python.debugger.remote.PyRemoteDebugConfiguration;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlRawConfig;
import dev.nocalhost.plugin.intellij.data.NocalhostContext;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;

public class NocalhostPythonDebugRunner implements ProgramRunner<RunnerSettings> {
    public static final String NOCALHOST_PYTHON_DEBUG_RUNNER = "NocalhostPythonDebugRunner";

    @NotNull
    public String getRunnerId() {
        return NOCALHOST_PYTHON_DEBUG_RUNNER;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof PyRemoteDebugConfiguration;
    }

    private ServerSocket createServerSocket(int port) throws ExecutionException {
        try {
            return new ServerSocket(port);
        } catch (IOException ex) {
            throw new ExecutionException(ex);
        }
    }

    private @NotNull PathMappingSettings createPathMappingSettings(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        var setting = new PathMappingSettings();
        try {
            var context = NocalhostContextManager.getInstance(environment.getProject()).getContext();
            setting.add(new PathMappingSettings.PathMapping() {
                {
                    setLocalRoot(environment.getProject().getBasePath());
                    setRemoteRoot(getWorkDir(context));
                }
            });
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
        return setting;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        PyRemoteDebugConfiguration conf = (PyRemoteDebugConfiguration) environment.getRunProfile();
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, state -> {
            ((NocalhostPythonProfileState) state).prepare();
            conf.setMappingSettings(createPathMappingSettings(environment));
            ServerSocket socket = createServerSocket(conf.getPort());
            ExecutionResult result = state.execute(environment.getExecutor(), NocalhostPythonDebugRunner.this);
            XDebugProcessStarter starter = new XDebugProcessStarterImpl(environment, conf, result, socket);
            return XDebuggerManager
                    .getInstance(environment.getProject())
                    .startSession(environment, starter)
                    .getRunContentDescriptor();
        });
    }

    private String getWorkDir(@NotNull NocalhostContext service) throws ExecutionException, InterruptedException, NocalhostExecuteCmdException, IOException {
        var opts = new NhctlConfigOptions(service.getKubeConfigPath(), service.getNamespace());
        opts.setDeployment(service.getServiceName());
        opts.setControllerType(service.getServiceType());
        var config = ApplicationManager
                .getApplication()
                .getService(NhctlCommand.class)
                .getConfig(service.getApplicationName(), opts, NhctlRawConfig.class);
        var bucket = config.getContainers();
        var container = bucket.isEmpty() ? null : bucket.get(0);
        try {
            return container.getDev().getWorkDir();
        } catch (Exception ex) {
            throw new ExecutionException("The configuration of the service container is incorrect.");
        }
    }
}
