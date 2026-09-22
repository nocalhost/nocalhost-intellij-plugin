package dev.nocalhost.plugin.intellij.configuration;

import com.google.common.collect.Lists;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.terminal.TerminalExecutionConsole;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardEndOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlRawConfig;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatusOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.configuration.java.NocalhostJavaDebugRunner;
import dev.nocalhost.plugin.intellij.configuration.php.NocalhostPhpDebugRunner;
import dev.nocalhost.plugin.intellij.data.NocalhostContext;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.i18n.NocalhostI18n;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.service.NocalhostDebugReconnectService;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.JdwpUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import dev.nocalhost.plugin.intellij.utils.ProgressUtil;

import static java.nio.charset.StandardCharsets.UTF_8;

public class NocalhostProfileState extends CommandLineState {
    private static final Logger LOG = Logger.getInstance(NocalhostProfileState.class);

    private static final String DEFAULT_SHELL = "sh";

    /** 调试目标地址：调试端口转发只暴露在本机回环上。 */
    private static final String DEBUG_HOST = "127.0.0.1";

    /** JDWP 探活超时时间（毫秒），用于判断远端 JVM 是否还活着。 */
    private static final int JDWP_PROBE_TIMEOUT_MILLIS = 3000;

    /**
     * 在 ProcessHandler 上标注运行配置名，便于其它流程（例如 ExecutionTask 的“先杀掉旧会话”）
     * 识别并停止 attach-only 会话。
     */
    public static final Key<String> KEY_RUN_PROFILE_NAME = Key.create("nocalhost.debug.run.profile.name");

    private final List<Disposable> disposables = Lists.newArrayList();
    private final AtomicReference<NocalhostRunnerContext> refContext = new AtomicReference<>(null);

    /**
     * 本次运行是否为 attach-only（断线重连）：为 true 时不会执行 nhctl exec 去拉起远端进程，
     * 只恢复调试端口转发并 attach 到已经在跑的 JVM 上。
     */
    private volatile boolean attachOnly;

    public NocalhostProfileState(ExecutionEnvironment environment) {
        super(environment);
    }

    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        NocalhostRunnerContext dev = refContext.get();
        if (dev == null) {
            throw new ExecutionException(NocalhostI18n.get("error.callPrepareFirst"));
        }
        if (attachOnly) {
            // 重连：远端 JVM 一直在跑，本地不需要（也不应该）再拉起任何进程。
            // 处理器由 Java 调试侧提供（平台自带的远端调试处理器 + autoRestart），
            // 这样后续再抖动时，平台自己就能把同一个会话重连回去；这里只负责在会话结束时收尾。
            ProcessHandler handler = NocalhostJavaDebugRunner
                    .createAttachOnlyProcessHandler(getEnvironment().getProject());
            handler.addProcessListener(new ProcessListener() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    destroy();
                }
            });
            // 标注运行配置名：ExecutionTask 等流程停止会话时需要识别出这是哪个服务的调试会话
            var runProfile = getEnvironment().getRunProfile();
            handler.putUserData(KEY_RUN_PROFILE_NAME, runProfile == null ? null : runProfile.getName());
            return handler;
        }
        NocalhostContext context = dev.getContext();

        String shell = StringUtils.isNotEmpty(dev.getShell()) ? dev.getShell() : DEFAULT_SHELL;
        String command = isDebugExecutor() ? dev.getCommand().getDebug() : dev.getCommand().getRun();

        List<String> commandLine = Lists.newArrayList(
                NhctlUtil.binaryPath(), "exec", context.getApplicationName(),
                "--deployment", dev.getContext().getServiceName(),
                "--controller-type", dev.getContext().getServiceType(),
                "--command", shell, "--command", "-c", "--command", command,
                "--kubeconfig", context.getKubeConfigPath().toString(),
                "--namespace", context.getNamespace()
        );
        return new NocalhostDevProcessHandler(new GeneralCommandLine(commandLine).withCharset(UTF_8), getEnvironment(), this);
    }

    public String getDebugPort() {
        NocalhostRunnerContext dev = refContext.get();
        return dev.getDebug().getLocalPort();
    }

    @Override
    protected @NotNull ConsoleView createConsole(@NotNull Executor executor) {
        return new TerminalExecutionConsole(getEnvironment().getProject(), null);
    }

    public void prepare() throws ExecutionException {
        // prepare() 是在 EDT 上被调用的（ExecutionManager.startRunProfile 的回调），而它内部是一串
        // 阻塞的 nhctl 子进程调用（nhctl get / sync status / dev pod / port-forward start），
        // 直接在 EDT 上执行会被诊断成 "Freeze in EDT"。
        // 放到模态进度框里执行，真正的工作跑在后台线程上，事件队列继续响应。
        ProgressUtil.<Void, ExecutionException>runWithModalProgress(
                getEnvironment().getProject(),
                NocalhostI18n.get("progress.prepareRunEnvironment"),
                () -> {
                    doPrepare();
                    return null;
                });
    }

    private void doPrepare() throws ExecutionException {
        try {
            var project = getEnvironment().getProject();
            var context = NocalhostContextManager.getInstance(project).getContext();
            if (context == null) {
                throw new ExecutionException(NocalhostI18n.get("error.contextNull"));
            }

            // “断线重连”：远端 JVM 还在跑，本次不再执行 nhctl exec（不重启远端进程），
            // 只要把调试端口转发恢复出来，然后 attach 回去即可。
            attachOnly = isJavaDebugExecutor()
                    && NocalhostDebugReconnectService.getInstance(project).consumeReattachRequest();
            if (attachOnly) {
                doPrepareReattach(project, context);
                return;
            }

            var desService = NhctlUtil.getDescribeService(project, context);
            if ( ! NhctlDescribeServiceUtil.developStarted(desService)) {
                throw new ExecutionException(NocalhostI18n.get("error.serviceNotInDevMode"));
            }
            if ( ! isProjectPathMatched(desService)) {
                throw new ExecutionException(NocalhostI18n.get("error.projectPathNotMatch"));
            }
            if ( ! isSyncStatusIdle()) {
                throw new ExecutionException(NocalhostI18n.get("error.fileSyncNotCompleted"));
            }

            ServiceContainer container = resolveContainer(context);

            NocalhostRunnerContext.Debug debug = null;
            NocalhostRunnerContext.Command command = new NocalhostRunnerContext.Command(resolveRunCommand(container), resolveDebugCommand(container));
            if (isDebugExecutor()) {
                if (!StringUtils.isNotEmpty(command.getDebug())) {
                    throw new ExecutionException(NocalhostI18n.get("error.debugCommandNotConfigured"));
                }

                String runnerId = getEnvironment().getRunner().getRunnerId();
                if (NocalhostPhpDebugRunner.RUNNER_ID.equals(runnerId)) {
                    // PHP remote debugging use SSH tunnel
                    doCreateTunnel(container);
                } else {
                    String remotePort = resolveDebugPort(container);
                    if (!StringUtils.isNotEmpty(remotePort)) {
                        throw new ExecutionException(NocalhostI18n.get("error.remoteDebugPortNotConfigured"));
                    }
                    String localPort = startDebugPortForward(context, remotePort);
                    debug = new NocalhostRunnerContext.Debug(remotePort, localPort);
                }
            } else {
                if (!StringUtils.isNotEmpty(command.getRun())) {
                    throw new ExecutionException(NocalhostI18n.get("error.runCommandNotConfigured"));
                }
            }

            refContext.set(new NocalhostRunnerContext(
                    debug,
                    container.getDev().getShell(),
                    command,
                    context,
                    container
            ));
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException | ExecutionException e) {
            throw new ExecutionException(e);
        }
    }

    /**
     * “断线重连”的轻量 prepare：远端 JVM 还在跑（JDWP 端口仍在监听），
     * 本地不做 dev associate、不等待文件同步，只保证调试端口转发可用，随后 attach 回去。
     */
    private void doPrepareReattach(Project project, NocalhostContext context) throws ExecutionException {
        var desService = NhctlUtil.getDescribeService(project, context);
        if ( ! NhctlDescribeServiceUtil.developStarted(desService)) {
            throw new ExecutionException(NocalhostI18n.get("error.reconnectDevModeStopped"));
        }

        ServiceContainer container = resolveContainer(context);
        String remotePort = resolveDebugPort(container);
        if (StringUtils.isEmpty(remotePort)) {
            throw new ExecutionException(NocalhostI18n.get("error.remoteDebugPortNotConfigured"));
        }

        String localPort = acquireDebugPortForward(context, desService, remotePort);

        echo(project, NocalhostI18n.format("console.reattachDebug", localPort, remotePort));
        refContext.set(new NocalhostRunnerContext(
                new NocalhostRunnerContext.Debug(remotePort, localPort),
                container.getDev().getShell(),
                new NocalhostRunnerContext.Command(resolveRunCommand(container), resolveDebugCommand(container)),
                context,
                container
        ));
        // 记一次 attach-only 会话：在它退出之前，其它会话不要断开这条调试端口转发
        NocalhostDebugReconnectService.getInstance(project).beginReattachSession();
    }

    /**
     * 优先复用 describe 里已有的调试端口转发（上一次调试留下的），
     * 只有连不通时才重建，尽量缩短重连耗时。
     */
    private String acquireDebugPortForward(NocalhostContext context,
                                           NhctlDescribeService desService,
                                           String remotePort) throws ExecutionException {
        String existing = findLocalPort(desService, remotePort);
        if (StringUtils.isNotEmpty(existing) && isJdwpAliveWithRetry(existing)) {
            return existing;
        }
        if (StringUtils.isNotEmpty(existing)) {
            // 转发记录还在，但网络抖动之后隧道已经不通了，先清理再重建
            endDebugPortForward(context, existing, remotePort);
        }
        String localPort = startDebugPortForward(context, remotePort);
        if (StringUtils.isEmpty(localPort)) {
            throw new ExecutionException(NocalhostI18n.get("error.debugPortForwardNotAvailable"));
        }
        if ( ! isJdwpAliveWithRetry(localPort)) {
            throw new ExecutionException(NocalhostI18n.format("error.remoteDebugNotReachable", remotePort));
        }
        return localPort;
    }

    private static String findLocalPort(NhctlDescribeService desService, String remotePort) {
        if (desService.getDevPortForwardList() == null) {
            return null;
        }
        for (NhctlPortForward pf : desService.getDevPortForwardList()) {
            if (StringUtils.equals(pf.getRemoteport(), remotePort)) {
                return pf.getLocalport();
            }
        }
        return null;
    }

    /**
     * 在 describe 结果里挑一个“探活成功”的本地端口。
     * 上一次调试留下的转发记录可能已经死了（网络抖动），优先返回还连得通的。
     */
    private static String findAliveLocalPort(NhctlDescribeService desService, String remotePort) {
        if (desService.getDevPortForwardList() == null) {
            return null;
        }
        for (NhctlPortForward pf : desService.getDevPortForwardList()) {
            if (StringUtils.equals(pf.getRemoteport(), remotePort)
                    && JdwpUtil.isJdwpListenerAlive(DEBUG_HOST, pf.getLocalport(), JDWP_PROBE_TIMEOUT_MILLIS)) {
                return pf.getLocalport();
            }
        }
        return null;
    }

    /**
     * 网络抖动刚恢复时端口转发可能要几百毫秒才可用，探活失败后再试一次，避免误判为“隧道已死”。
     */
    private static boolean isJdwpAliveWithRetry(String localPort) {
        if (JdwpUtil.isJdwpListenerAlive(DEBUG_HOST, localPort, JDWP_PROBE_TIMEOUT_MILLIS)) {
            return true;
        }
        try {
            Thread.sleep(500L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return JdwpUtil.isJdwpListenerAlive(DEBUG_HOST, localPort, JDWP_PROBE_TIMEOUT_MILLIS);
    }

    private ServiceContainer resolveContainer(NocalhostContext context) throws ExecutionException {
        NhctlRawConfig devConfig = NhctlUtil.getDevConfig(context);
        List<ServiceContainer> containers = devConfig.getContainers();
        ServiceContainer container = containers.isEmpty() ? null : containers.get(0);
        if (StringUtils.isNotEmpty(context.getContainerName())) {
            for (ServiceContainer c : containers) {
                if (StringUtils.equals(context.getContainerName(), c.getName())) {
                    container = c;
                    break;
                }
            }
        }
        if (container == null) {
            throw new ExecutionException(NocalhostI18n.get("error.containerConfigNotFound"));
        }
        return container;
    }

    private void echo(Project project, String message) {
        project.getMessageBus()
                .syncPublisher(NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC)
                .action(message + System.lineSeparator());
    }

    /**
     * 本次运行是否为“断线重连”（attach-only）会话：不重启远端进程，只重新 attach。
     */
    public boolean isAttachOnly() {
        return attachOnly;
    }

    private void doCreateTunnel(ServiceContainer container) throws ExecutionException {
        String port = resolveDebugPort(container);
        Project project = getEnvironment().getProject();
        NocalhostContext context = NocalhostContextManager.getInstance(project).getContext();

        if (StringUtils.isEmpty(port)) {
            throw new ExecutionException("Remote debug port not configured.");
        }

        var cmd = new GeneralCommandLine(Lists.newArrayList(
                NhctlUtil.binaryPath(), "ssh", "reverse",
                "--pod", NhctlUtil.getDevPodName(project, context),
                "--local", port,
                "--remote", port,
                "--sshport", "50022",
                "--namespace", context.getNamespace(),
                "--kubeconfig", context.getKubeConfigPath().toString()
        )).withRedirectErrorStream(true);

        var bus = project
                .getMessageBus()
                .syncPublisher(NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC);
        bus.action("[cmd] " + cmd.getCommandLineString() + System.lineSeparator());

        var process = cmd.createProcess();
        disposables.add(() -> process.destroy());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            StringBuilder sb = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(process.getInputStream(), UTF_8);
            try (BufferedReader br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    bus.action(line + System.lineSeparator());
                    sb.append(line).append(System.lineSeparator());
                }
                int code = process.waitFor();
                if (code != 0) {
                    bus.action("[ssh] Process finished with exit code " + code + System.lineSeparator());
                }
            } catch (Exception ex) {
                LOG.error(ex);
            }
        });
    }

    private boolean isDebugExecutor() {
        return StringUtils.equals(DefaultDebugExecutor.EXECUTOR_ID, getEnvironment().getExecutor().getId());
    }

    /**
     * 是否是 Java 远程调试（JDWP）。
     * “断线重连”只对 Java 生效：Go(dlv)/Node(inspector)/PHP(xdebug) 的调试协议与探活方式都不同，
     * 不能复用这里的 JDWP attach 逻辑。
     */
    private boolean isJavaDebugExecutor() {
        var runner = getEnvironment().getRunner();
        return isDebugExecutor()
                && runner != null
                && StringUtils.equals(NocalhostJavaDebugRunner.RUNNER_ID, runner.getRunnerId());
    }

    private String startDebugPortForward(NocalhostContext context, String remotePort) throws ExecutionException {
        NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

        try {
            var podName = NhctlUtil.getDevPodName(getEnvironment().getProject(), context);
            NhctlPortForwardStartOptions nhctlPortForwardStartOptions = new NhctlPortForwardStartOptions(context.getKubeConfigPath(), context.getNamespace());
            nhctlPortForwardStartOptions.setDevPorts(List.of(":" + remotePort));
            nhctlPortForwardStartOptions.setDeployment(context.getServiceName());
            nhctlPortForwardStartOptions.setType(context.getServiceType());
            nhctlPortForwardStartOptions.setPod(podName);
            nhctlCommand.startPortForward(context.getApplicationName(), nhctlPortForwardStartOptions);

            var desService = NhctlUtil.getDescribeService(getEnvironment().getProject(), context);
            String alive = findAliveLocalPort(desService, remotePort);
            if (StringUtils.isNotEmpty(alive)) {
                return alive;
            }
            for (NhctlPortForward pf : desService.getDevPortForwardList()) {
                if (StringUtils.equals(pf.getRemoteport(), remotePort)) {
                    return pf.getLocalport();
                }
            }
            return null;
        } catch (Exception e) {
            throw new ExecutionException(e);
        }
    }

    public void stopDebugPortForward() {
        NocalhostRunnerContext dev = refContext.get();
        if (dev == null) {
            return;
        }
        NocalhostRunnerContext.Debug debug = dev.getDebug();
        if (debug == null) {
            return;
        }

        NocalhostContext context = dev.getContext();
        ApplicationManager.getApplication().executeOnPooledThread(
                () -> endDebugPortForward(context, debug.getLocalPort(), debug.getRemotePort()));
    }

    private void endDebugPortForward(NocalhostContext context, String localPort, String remotePort) {
        try {
            NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

            NhctlPortForwardEndOptions nhctlPortForwardEndOptions = new NhctlPortForwardEndOptions(context.getKubeConfigPath(), context.getNamespace());
            nhctlPortForwardEndOptions.setPort(localPort + ":" + remotePort);
            nhctlPortForwardEndOptions.setDeployment(context.getServiceName());
            nhctlPortForwardEndOptions.setType(context.getServiceType());

            nhctlCommand.endPortForward(context.getApplicationName(), nhctlPortForwardEndOptions);
        } catch (Exception e) {
            LOG.warn("[debug] fail to end debug port-forward " + localPort + ":" + remotePort, e);
        }
    }

    private boolean isSyncStatusIdle() throws IOException, NocalhostExecuteCmdException, InterruptedException {
        NocalhostContext nocalhostContext = NocalhostContextManager.getInstance(getEnvironment().getProject()).getContext();
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
        NhctlSyncStatusOptions opts = new NhctlSyncStatusOptions(nocalhostContext.getKubeConfigPath(),
                nocalhostContext.getNamespace());
        opts.setDeployment(nocalhostContext.getServiceName());
        opts.setControllerType(nocalhostContext.getServiceType());
        String status = nhctlCommand.syncStatus(nocalhostContext.getApplicationName(), opts);
        NhctlSyncStatus nhctlSyncStatus = DataUtils.GSON.fromJson(status, NhctlSyncStatus.class);
        return StringUtils.equals(nhctlSyncStatus.getStatus(), "idle");
    }

    private boolean isProjectPathMatched(NhctlDescribeService nhctlDescribeService) {
        var basePath = Paths.get(getEnvironment().getProject().getBasePath()).toString();
        for (String path : nhctlDescribeService.getLocalAbsoluteSyncDirFromDevStartPlugin()) {
            if (StringUtils.equals(basePath, path)) {
                return true;
            }
        }
        return false;
    }

    private static String resolveRunCommand(ServiceContainer serviceContainer) {
        if (serviceContainer == null
                || serviceContainer.getDev() == null
                || serviceContainer.getDev().getCommand() == null
                || serviceContainer.getDev().getCommand().getRun() == null) {
            return null;
        }
        return String.join(" ", serviceContainer.getDev().getCommand().getRun());
    }

    private static String resolveDebugCommand(ServiceContainer serviceContainer) {
        if (serviceContainer == null
                || serviceContainer.getDev() == null
                || serviceContainer.getDev().getCommand() == null
                || serviceContainer.getDev().getCommand().getDebug() == null) {
            return null;
        }
        return String.join(" ", serviceContainer.getDev().getCommand().getDebug());
    }

    private static String resolveDebugPort(ServiceContainer serviceContainer) {
        if (serviceContainer == null
                || serviceContainer.getDev() == null
                || serviceContainer.getDev().getDebug() == null) {
            return null;
        }
        return serviceContainer.getDev().getDebug().getRemoteDebugPort();
    }

    public void startup() throws ExecutionException {
        var dev = refContext.get();
        if (dev == null) {
            throw new ExecutionException(NocalhostI18n.get("error.callPrepareFirst"));
        }
        if (dev.getContainer().getDev().isHotReload()) {
            disposables.add(new HotReload(getEnvironment()).withExec());
        }
    }

    public void destroy() {
        disposables.forEach(x -> x.dispose());
        disposables.clear();
        var reconnectService = NocalhostDebugReconnectService.getInstance(getEnvironment().getProject());
        if (attachOnly) {
            // 重连会话的端口转发通常是从上一个会话复用来的（也可能由本次新建，供后续重连继续复用），
            // 这里不主动断开，避免误杀下一次重连要用的转发；DevMode 结束时会一并清理。
            reconnectService.endReattachSession();
            return;
        }
        if (reconnectService.hasActiveReattachSession()) {
            // 有 attach-only 会话正在复用这条调试端口转发，别把它拆掉
            return;
        }
        stopDebugPortForward();
    }
}
