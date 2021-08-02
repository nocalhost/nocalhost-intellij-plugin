package dev.nocalhost.plugin.intellij.configuration.python;

import com.intellij.execution.ExecutionResult;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.python.debugger.PyDebugRunner;
import com.jetbrains.python.debugger.PyRemoteDebugProcess;
import com.jetbrains.python.debugger.remote.PyRemoteDebugConfiguration;
import com.jetbrains.python.debugger.remote.vfs.PyRemotePositionConverter;

import org.jetbrains.annotations.NotNull;

import java.net.ServerSocket;

public class XDebugProcessStarterImpl extends XDebugProcessStarter {
    @NotNull
    private final ExecutionEnvironment environment;

    @NotNull
    private final PyRemoteDebugConfiguration conf;

    @NotNull
    private final ExecutionResult result;

    @NotNull
    private final ServerSocket serverSocket;

    public XDebugProcessStarterImpl(
            @NotNull ExecutionEnvironment environment,
            @NotNull PyRemoteDebugConfiguration conf,
            @NotNull ExecutionResult result,
            @NotNull ServerSocket serverSocket
    ) {
        this.environment = environment;
        this.conf = conf;
        this.result = result;
        this.serverSocket = serverSocket;
    }

    @NotNull
    public final ExecutionEnvironment getEnvironment() {
        return this.environment;
    }

    @NotNull
    public final PyRemoteDebugConfiguration getConf() {
        return this.conf;
    }

    @NotNull
    public final ExecutionResult getResult() {
        return this.result;
    }

    @NotNull
    public final ServerSocket getServerSocket() {
        return this.serverSocket;
    }

    @NotNull
    public XDebugProcess start(@NotNull XDebugSession session) {
        PyRemoteDebugProcess pyDebugProcess = new PyRemoteDebugProcess(session, getServerSocket(), getResult().getExecutionConsole(), getResult().getProcessHandler(), getConf().getSettraceCall(getServerSocket().getLocalPort()));
        pyDebugProcess.setPositionConverter(new PyRemotePositionConverter(pyDebugProcess, getConf().getMappingSettings()));
        PyDebugRunner.createConsoleCommunicationAndSetupActions(getEnvironment().getProject(), getResult(), pyDebugProcess, session);
        return pyDebugProcess;
    }
}
