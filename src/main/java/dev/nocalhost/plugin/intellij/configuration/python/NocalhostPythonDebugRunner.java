package dev.nocalhost.plugin.intellij.configuration.python;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebuggerManager;
import com.jetbrains.python.debugger.remote.PyRemoteDebugConfiguration;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ServerSocket;

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

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        PyRemoteDebugConfiguration conf = (PyRemoteDebugConfiguration) environment.getRunProfile();
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, state -> {
            ((NocalhostPythonProfileState) state).prepare();
            ServerSocket socket = createServerSocket(conf.getPort());
            ExecutionResult result = state.execute(environment.getExecutor(), NocalhostPythonDebugRunner.this);
            XDebugProcessStarter starter = new XDebugProcessStarterImpl(environment, conf, result, socket);
            return XDebuggerManager
                    .getInstance(environment.getProject())
                    .startSession(environment, starter)
                    .getRunContentDescriptor();
        });
    }
}
