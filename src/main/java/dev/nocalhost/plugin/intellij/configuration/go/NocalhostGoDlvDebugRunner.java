package dev.nocalhost.plugin.intellij.configuration.go;

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

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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
            @Override
            protected String compute(@NotNull ProgressIndicator indicator) throws ExecutionException {
                indicator.setText("Waiting for remote debug process start...");
                final long startTime = System.currentTimeMillis();
                while (true) {
                    if (System.currentTimeMillis() - startTime > 30 * 1000) {
                        throw new ExecutionException("Wait remote debug port start timeout");
                    }

                    Socket socket = null;
                    try {
                        Thread.sleep(500);
                        socket = new Socket("127.0.0.1", Integer.parseInt(debugPort));
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
                    } finally {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
                return "";
            }
        });
    }
}