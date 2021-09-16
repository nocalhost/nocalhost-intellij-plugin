package dev.nocalhost.plugin.intellij.configuration.node;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;

public class NocalhostNodeDebugRunner implements ProgramRunner<RunnerSettings> {
    @Override
    public @NotNull
    @NonNls String getRunnerId() {
        return "NocalhostNodeDebugRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof NocalhostNodeConfiguration;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        FileDocumentManager.getInstance().saveAllDocuments();
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, state -> {
            var _state = (NocalhostProfileState) state;
            _state.prepareDevInfo();

            var conf = (NocalhostNodeConfiguration)environment.getRunProfile();
            conf.setPort(Integer.parseInt(_state.getDebugPort()));

            var socket = conf.computeDebugAddress(state);
            var result = state.execute(environment.getExecutor(), this);

            return XDebuggerManager
                    .getInstance(environment.getProject())
                    .startSession(environment, new XDebugProcessStarter() {
                        @Override
                        public @NotNull XDebugProcess start(@NotNull XDebugSession session) {
                            return conf.createDebugProcess(socket, session, result, environment);
                        }
                    })
                    .getRunContentDescriptor();
        });
    }
}
