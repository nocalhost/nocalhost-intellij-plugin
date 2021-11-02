package dev.nocalhost.plugin.intellij.configuration;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardEndOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlRawConfig;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatusOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.configuration.php.NocalhostPhpDebugRunner;
import dev.nocalhost.plugin.intellij.data.NocalhostContext;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class NocalhostProfileState extends CommandLineState {
    private static final Logger LOG = Logger.getInstance(NocalhostProfileState.class);

    private static final String DEFAULT_SHELL = "sh";

    private final List<Disposable> disposables = Lists.newArrayList();
    private final AtomicReference<NocalhostRunnerContext> refContext = new AtomicReference<>(null);

    public NocalhostProfileState(ExecutionEnvironment environment) {
        super(environment);
    }

    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        NocalhostRunnerContext dev = refContext.get();
        if (dev == null) {
            throw new ExecutionException("Call prepare() before this method");
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
        return new NocalhostDevProcessHandler(new GeneralCommandLine(commandLine).withCharset(Charsets.UTF_8), getEnvironment(), this);
    }

    public String getDebugPort() {
        NocalhostRunnerContext dev = refContext.get();
        return dev.getDebug().getLocalPort();
    }

    public void prepare() throws ExecutionException {
        try {
            var context = NocalhostContextManager.getInstance(getEnvironment().getProject()).getContext();
            if (context == null) {
                throw new ExecutionException("Nocalhost context is null.");
            }

            var desService = NhctlUtil.getDescribeService(context);
            if ( ! NhctlDescribeServiceUtil.developStarted(desService)) {
                throw new ExecutionException("Service is not in dev mode.");
            }
            if ( ! isProjectPathMatched(desService)) {
                throw new ExecutionException("Project path does not match.");
            }
            if ( ! isSyncStatusIdle()) {
                throw new ExecutionException("File sync has not yet completed.");
            }

            NhctlRawConfig nhctlRawConfig = getNhctlConfig(context);
            List<ServiceContainer> containers = nhctlRawConfig.getContainers();
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
                throw new ExecutionException("Service container config not found.");
            }

            NocalhostRunnerContext.Debug debug = null;
            NocalhostRunnerContext.Command command = new NocalhostRunnerContext.Command(resolveRunCommand(container), resolveDebugCommand(container));
            if (isDebugExecutor()) {
                if (!StringUtils.isNotEmpty(command.getDebug())) {
                    throw new ExecutionException("Debug command not configured");
                }

                String runnerId = getEnvironment().getRunner().getRunnerId();
                if (NocalhostPhpDebugRunner.RUNNER_ID.equals(runnerId)) {
                    // PHP remote debugging use SSH tunnel
                    doCreateTunnel(container);
                } else {
                    String remotePort = resolveDebugPort(container);
                    if (!StringUtils.isNotEmpty(remotePort)) {
                        throw new ExecutionException("Remote debug port not configured.");
                    }
                    String localPort = startDebugPortForward(context, remotePort);
                    debug = new NocalhostRunnerContext.Debug(remotePort, localPort);
                }
            } else {
                if (!StringUtils.isNotEmpty(command.getRun())) {
                    throw new ExecutionException("Run command not configured");
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
            InputStreamReader reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8);
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

            NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(context.getKubeConfigPath(), context.getNamespace());
            nhctlDescribeOptions.setDeployment(context.getServiceName());
            nhctlDescribeOptions.setType(context.getServiceType());
            NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(context.getApplicationName(), nhctlDescribeOptions, NhctlDescribeService.class);

            for (NhctlPortForward pf : nhctlDescribeService.getDevPortForwardList()) {
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
        NocalhostRunnerContext.Debug debug = dev.getDebug();
        if (debug == null) {
            return;
        }

        NocalhostContext context = dev.getContext();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

                NhctlPortForwardEndOptions nhctlPortForwardEndOptions = new NhctlPortForwardEndOptions(context.getKubeConfigPath(), context.getNamespace());
                nhctlPortForwardEndOptions.setPort(debug.getLocalPort() + ":" + debug.getRemotePort());
                nhctlPortForwardEndOptions.setDeployment(context.getServiceName());
                nhctlPortForwardEndOptions.setType(context.getServiceType());

                nhctlCommand.endPortForward(context.getApplicationName(), nhctlPortForwardEndOptions);
            } catch (Exception e) {
                LOG.error(e);
            }
        });
    }

    private boolean isSyncStatusIdle() throws IOException, NocalhostExecuteCmdException, InterruptedException {
        NocalhostContext nocalhostContext = NocalhostContextManager.getInstance(getEnvironment().getProject()).getContext();
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication()
                .getService(NhctlCommand.class);
        NhctlSyncStatusOptions opts = new NhctlSyncStatusOptions(nocalhostContext.getKubeConfigPath(),
                nocalhostContext.getNamespace());
        opts.setDeployment(nocalhostContext.getServiceName());
        opts.setControllerType(nocalhostContext.getServiceType());
        String status = nhctlCommand.syncStatus(nocalhostContext.getApplicationName(), opts);
        NhctlSyncStatus nhctlSyncStatus = DataUtils.GSON.fromJson(status, NhctlSyncStatus.class);
        return StringUtils.equals(nhctlSyncStatus.getStatus(), "idle");
    }

    private NhctlRawConfig getNhctlConfig(NocalhostContext nocalhostContext)
            throws InterruptedException, NocalhostExecuteCmdException, IOException {
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
        NhctlConfigOptions opts = new NhctlConfigOptions(nocalhostContext.getKubeConfigPath(), nocalhostContext.getNamespace());
        opts.setDeployment(nocalhostContext.getServiceName());
        opts.setControllerType(nocalhostContext.getServiceType());
        return nhctlCommand.getConfig(nocalhostContext.getApplicationName(), opts, NhctlRawConfig.class);
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
            throw new ExecutionException("Call prepare() before this method");
        }
        if (dev.getContainer().getDev().isHotReload()) {
            disposables.add(new HotReload(getEnvironment()).withExec());
        }
    }

    public void destroy() {
        disposables.forEach(x -> x.dispose());
        disposables.clear();
        stopDebugPortForward();
    }
}
