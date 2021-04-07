package dev.nocalhost.plugin.intellij.configuration;

import com.goide.dlv.DlvDisconnectOption;
import com.goide.dlv.protocol.DlvApi;
import com.goide.execution.GoRunUtil;
import com.googlecode.jsonrpc4j.JsonRpcClient;
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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

public class NocalhostGoDlvDebugRunner implements ProgramRunner<RunnerSettings> {
    @Override
    public @NotNull
    @NonNls
    String getRunnerId() {
        return "NocalhostGoDlvDebugRunner";
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
            return attachDlv(state, environment, nocalhostProfileState.getDebugPort());
        });
    }

    private RunContentDescriptor attachDlv(RunProfileState state,
                                           @NotNull ExecutionEnvironment env,
                                           String debugPort) throws ExecutionException {
        NocalhostConfigurationFactory factory = (NocalhostConfigurationFactory) (new NocalhostConfigurationType().getConfigurationFactories())[0];
        NocalhostConfiguration configuration = (NocalhostConfiguration) factory.createTemplateConfiguration(env.getProject());
        Executor executor = DefaultDebugExecutor.getDebugExecutorInstance();
        ExecutionEnvironment environment = ExecutionEnvironmentBuilder.create(env.getProject(), executor, configuration).build();
        NocalhostRunner runner = Objects.requireNonNull(ProgramRunner.PROGRAM_RUNNER_EP.findExtension(NocalhostRunner.class));
        InetSocketAddress socketAddress = new InetSocketAddress("localhost", Integer.parseInt(debugPort));
        ExecutionResult executionResult = state.execute(environment.getExecutor(), runner);

        waitForDebuggerStartup(env.getProject(), debugPort);

        return XDebuggerManager.getInstance(env.getProject()).startSession(env, new XDebugProcessStarter() {
            @Override
            @NotNull
            public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
                return GoRunUtil.createDlvDebugProcess(session, executionResult, socketAddress, true, DlvDisconnectOption.DETACH);
            }
        }).getRunContentDescriptor();
    }

    private void waitForDebuggerStartup(Project project, String debugPort) throws ExecutionException {
        ProgressManager.getInstance().run(new Task.WithResult<String, ExecutionException>(project, "Starting Delve debugger...", true) {

            @SneakyThrows
            @Override
            protected String compute(@NotNull ProgressIndicator indicator) throws ExecutionException {
                indicator.setText("Waiting for remote debug process start...");
                while (true) {
                    Thread.sleep(1000);
                    try (Socket socket = new Socket("127.0.0.1", Integer.parseInt(debugPort))) {
                        JsonRpcClient client = new JsonRpcClient();
                        client.invokeAndReadResponse("RPCServer.GetVersion", null, DlvApi.GetVersion.class, socket.getOutputStream(), socket.getInputStream());
                        break;
                    } catch (Throwable e) {
                        if (StringUtils.contains(e.getMessage(), "No content to map due to end-of-input")) {
                            continue;
                        }
                        if (StringUtils.contains(e.getMessage(), "Connection reset")) {
                            continue;
                        }
                        throw new ExecutionException(e);
                    }
                }
                return "";
            }
        });
    }
}