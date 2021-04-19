package dev.nocalhost.plugin.intellij.configuration.go;

import com.goide.dlv.DlvDebugProcess;
import com.goide.dlv.DlvDisconnectOption;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Objects;

import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;
import dev.nocalhost.plugin.intellij.configuration.NocalhostRunner;

public class NocalhostGoDlvDebugRunner implements ProgramRunner<RunnerSettings> {
    @Override
    public @NotNull
    @NonNls
    String getRunnerId() {
        return "NocalhostGoDlvDebugRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof NocalhostGoConfiguration;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, state -> {
            NocalhostProfileState nocalhostProfileState = (NocalhostProfileState) state;
            nocalhostProfileState.prepareDevInfo();
            FileDocumentManager.getInstance().saveAllDocuments();
            return attachDlv(state, environment, nocalhostProfileState.getDebugPort());
        });
    }

    private RunContentDescriptor attachDlv(RunProfileState state,
                                           @NotNull ExecutionEnvironment env,
                                           String debugPort) throws ExecutionException {
        NocalhostGoConfigurationFactory factory = (NocalhostGoConfigurationFactory) (new NocalhostGoConfigurationType().getConfigurationFactories())[0];
        NocalhostGoConfiguration configuration = (NocalhostGoConfiguration) factory.createTemplateConfiguration(env.getProject());
        Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();
        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(env.getProject(), executor, configuration).build();
        NocalhostRunner runner = Objects.requireNonNull(ProgramRunner.PROGRAM_RUNNER_EP.findExtension(NocalhostRunner.class));
        InetSocketAddress socketAddress = new InetSocketAddress("localhost", Integer.parseInt(debugPort));
        ExecutionResult executionResult = state.execute(environment.getExecutor(), runner);

        return XDebuggerManager.getInstance(env.getProject()).startSession(env, new XDebugProcessStarter() {
            @Override
            @NotNull
            public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
                DlvDebugProcess process = new DlvDebugProcess(
                        session,
                        new NocalhostGoDlvRemoteVmConnection(DlvDisconnectOption.DETACH),
                        executionResult,
                        true);
                process.connect(socketAddress);
                return process;
            }
        }).getRunContentDescriptor();
    }
}