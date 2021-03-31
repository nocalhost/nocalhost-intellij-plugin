package dev.nocalhost.plugin.intellij.configuration;

import com.intellij.debugger.DebugEnvironment;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.DefaultDebugEnvironment;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.impl.DebuggerSession;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.impl.XDebugSessionImpl;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class NocalhostJavaDebugRunner implements ProgramRunner<RunnerSettings> {
    private static final long DEFAULT_POLL_TIMEOUT = 10 * 60 * 1000; // 10 minute

    @Override
    public @NotNull
    @NonNls
    String getRunnerId() {
        return "NocalhostJavaDebugRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof NocalhostConfiguration;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, state -> {
            NocalhostProfileState nocalhostProfileState = (NocalhostProfileState) state;
            nocalhostProfileState.prepareDevInfo();
            FileDocumentManager.getInstance().saveAllDocuments();
            RemoteConnection connection = new RemoteConnection(true, "127.0.0.1", nocalhostProfileState.getDebugPort(), false);
            return attachVirtualMachine(state, environment, connection);
        });
    }

    private RunContentDescriptor attachVirtualMachine(RunProfileState state,
                                                      @NotNull ExecutionEnvironment env,
                                                      RemoteConnection connection) throws ExecutionException {
        DebugEnvironment environment = new DefaultDebugEnvironment(env, state, connection, DEFAULT_POLL_TIMEOUT);
        final DebuggerSession debuggerSession = DebuggerManagerEx.getInstanceEx(env.getProject()).attachVirtualMachine(environment);
        if (debuggerSession == null) {
            return null;
        }

        final DebugProcessImpl debugProcess = debuggerSession.getProcess();
        return XDebuggerManager.getInstance(env.getProject()).startSession(env, new XDebugProcessStarter() {
            @Override
            @NotNull
            public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
                XDebugSessionImpl sessionImpl = (XDebugSessionImpl) session;
                ExecutionResult executionResult = debugProcess.getExecutionResult();
                sessionImpl.addExtraActions(executionResult.getActions());
                if (executionResult instanceof DefaultExecutionResult) {
                    sessionImpl.addRestartActions(((DefaultExecutionResult) executionResult).getRestartActions());
                }
                return JavaDebugProcess.create(session, debuggerSession);
            }
        }).getRunContentDescriptor();
    }
}
