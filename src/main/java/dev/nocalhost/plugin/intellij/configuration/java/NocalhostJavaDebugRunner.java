package dev.nocalhost.plugin.intellij.configuration.java;

import com.intellij.debugger.DebugEnvironment;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.DefaultDebugEnvironment;
import com.intellij.debugger.engine.DebugProcessAdapterImpl;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaDebugProcess;
import com.intellij.debugger.engine.RemoteDebugProcessHandler;
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
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.impl.XDebugSessionImpl;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.configuration.NocalhostProfileState;
import dev.nocalhost.plugin.intellij.configuration.XDebugSessionCompat;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.service.NocalhostDebugReconnectService;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.ui.action.workload.ReconnectDebugAction;

public class NocalhostJavaDebugRunner implements ProgramRunner<RunnerSettings> {
    private static final Logger LOG = Logger.getInstance(NocalhostJavaDebugRunner.class);

    private static final long DEFAULT_POLL_TIMEOUT = 10 * 60 * 1000; // 10 minute

    /** 会话存活超过该时长后，认为上一次连接是稳定的，重置自动重连的次数预算。 */
    private static final long STABLE_SESSION_MILLIS = 30 * 1000L;

    /** 断开后等一小会儿再自动重连，给网络/端口转发一点恢复时间。 */
    private static final long AUTO_REATTACH_DELAY_MILLIS = 2000L;

    /** 运行器 id：{@code NocalhostProfileState} 用它判断“是否 Java 调试”，从而只对 JDWP 启用断线重连。 */
    public static final String RUNNER_ID = "NocalhostJavaDebugRunner";

    /**
     * 创建一个“只 attach、不启动本地进程”的 ProcessHandler。
     *
     * <p>用平台自带的远端调试处理器（{@code autoRestart = true}）：当这条连接再次断开且不是
     * 用户主动停止时，平台会把同一个调试会话自动重新 attach 回去，不需要重启远端 JVM。
     *
     * <p>之所以放在这里而不是 {@code NocalhostProfileState}：{@code NocalhostProfileState} 是
     * 所有 IDE（PyCharm/GoLand/PhpStorm...）都会加载的公共类，而 {@code RemoteDebugProcessHandler}
     * 属于 Java 插件；把这段代码隔离在只在 Java 调试流程才会加载的类里，避免非 Java IDE 触发类加载问题。
     */
    public static ProcessHandler createAttachOnlyProcessHandler(@NotNull Project project) {
        return new RemoteDebugProcessHandler(project, true);
    }

    @Override
    public @NotNull
    @NonNls
    String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof NocalhostJavaConfiguration;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        ExecutionManager.getInstance(environment.getProject()).startRunProfile(environment, state -> {
            NocalhostProfileState nocalhostProfileState = (NocalhostProfileState) state;
            var reconnectService = NocalhostDebugReconnectService.getInstance(environment.getProject());
            try {
                nocalhostProfileState.prepare();
            } catch (ExecutionException e) {
                // prepare 失败（例如远端调试端口已经不可达）：释放“重连在途”标记，别卡住后续重连
                reconnectService.endReattach();
                throw e;
            }
            // 会话已经交给平台执行，释放“重连在途”标记
            reconnectService.endReattach();
            FileDocumentManager.getInstance().saveAllDocuments();
            boolean attachOnly = nocalhostProfileState.isAttachOnly();
            RemoteConnection connection = new RemoteConnection(true, "127.0.0.1", nocalhostProfileState.getDebugPort(), false);
            return attachVirtualMachine(state, environment, connection, attachOnly);
        });
    }

    private RunContentDescriptor attachVirtualMachine(RunProfileState state,
                                                      @NotNull ExecutionEnvironment env,
                                                      RemoteConnection connection,
                                                      boolean attachOnly) throws ExecutionException {
        DebugEnvironment environment = new DefaultDebugEnvironment(env, state, connection, DEFAULT_POLL_TIMEOUT);
        final DebuggerSession debuggerSession = DebuggerManagerEx.getInstanceEx(env.getProject()).attachVirtualMachine(environment);
        if (debuggerSession == null) {
            return null;
        }

        final DebugProcessImpl debugProcess = debuggerSession.getProcess();
        final long attachedAt = System.currentTimeMillis();
        return XDebugSessionCompat.startSession(env, new XDebugProcessStarter() {
            @Override
            @NotNull
            public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
                XDebugSessionImpl sessionImpl = (XDebugSessionImpl) session;
                ExecutionResult executionResult = debugProcess.getExecutionResult();
                sessionImpl.addExtraActions(executionResult.getActions());
                // 调试工具窗口工具栏上的“重连调试”按钮：断线后可以一键 attach 回仍在运行的 JVM。
                sessionImpl.addExtraActions(new ReconnectDebugAction(env.getProject()));
                if (executionResult instanceof DefaultExecutionResult) {
                    sessionImpl.addRestartActions(((DefaultExecutionResult) executionResult).getRestartActions());
                }
                watchDisconnection(env, debugProcess, attachedAt, attachOnly);
                return JavaDebugProcess.create(session, debuggerSession);
            }
        });
    }

    /**
     * 监听调试连接断开事件。
     *
     * <p>网络原因断开时 {@code closedByUser == false}，此时容器里的 JVM 通常还在跑
     * （JDWP 端口仍然监听），插件自动发起一次 attach-only 重连：
     * 只恢复调试端口转发并重新 attach，不会重新执行 {@code nhctl exec} 去重启远端进程。
     *
     * <p>用户主动停止（Stop / 重新 Debug / 结束 DevMode）时不会自动重连。
     */
    private void watchDisconnection(@NotNull ExecutionEnvironment env,
                                    @NotNull DebugProcessImpl debugProcess,
                                    long attachedAt,
                                    boolean attachOnly) {
        debugProcess.addDebugProcessListener(new DebugProcessAdapterImpl() {
            @Override
            public void processDetached(@NotNull DebugProcessImpl process, boolean closedByUser) {
                if (closedByUser) {
                    return;
                }

                Project project = env.getProject();
                var reconnectService = NocalhostDebugReconnectService.getInstance(project);
                if (reconnectService.isAutoReconnectSuppressed()) {
                    return;
                }
                var settings = ApplicationManager.getApplication().getService(NocalhostSettings.class);
                if (settings == null || !settings.getAutoReconnectDebug()) {
                    return;
                }

                // 上一次连接如果稳定存活了一段时间，说明这次只是偶发抖动，重新给足重试预算。
                if (System.currentTimeMillis() - attachedAt >= STABLE_SESSION_MILLIS) {
                    reconnectService.resetAutoReattachAttempts();
                }
                if (reconnectService.nextAutoReattachAttempt() < 0) {
                    NocalhostNotifier
                            .getInstance(project)
                            .notifyError(NocalhostI18n.get("error.reconnectDebug"),
                                    NocalhostI18n.get("error.reconnectDebug.tooManyAttempts"),
                                    new ReconnectDebugAction(project));
                    return;
                }

                LOG.info("[debug] session detached by connection (attachOnly=" + attachOnly + "), scheduling reattach");
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        Thread.sleep(AUTO_REATTACH_DELAY_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    if ( ! reconnectService.tryBeginReattach()) {
                        // 已经有别的重连在途（例如用户刚点了“重连调试”），让那一次去处理，避免重复重启会话
                        LOG.info("[debug] another reattach is in flight, skip auto reattach");
                        return;
                    }
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            reconnectService.requestReattach();
                            ExecutionManager
                                    .getInstance(project)
                                    .restartRunProfile(new ExecutionEnvironmentBuilder(env).build());
                        } catch (Exception e) {
                            reconnectService.endReattach();
                            LOG.warn("[debug] fail to restart debug session for reconnect", e);
                        }
                    });
                });
            }
        });
    }
}
