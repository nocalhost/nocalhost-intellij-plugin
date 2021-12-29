package dev.nocalhost.plugin.intellij.configuration.python;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.debugger.remote.PyRemoteDebugCommandLineState;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import dev.nocalhost.plugin.intellij.commands.data.NhctlRawConfig;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.configuration.HotReload;
import dev.nocalhost.plugin.intellij.configuration.NocalhostRunnerContext;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.service.NocalhostContextManager;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class NocalhostPythonProfileState extends PyRemoteDebugCommandLineState {
    private static final String DEFAULT_SHELL = "sh";
    private static final Logger LOG = Logger.getInstance(NocalhostPythonProfileState.class);
    private final List<Disposable> disposables = Lists.newArrayList();
    private final AtomicReference<NocalhostRunnerContext> refContext = new AtomicReference<>(null);

    public NocalhostPythonProfileState(@NotNull Project project, @NotNull ExecutionEnvironment env) {
        super(project, env);
    }

    protected ProcessHandler startProcess() {
        return new NocalhostPythonDevProcessHandler(getEnvironment(), this);
    }

    public void prepare() throws ExecutionException {
        var context = NocalhostContextManager.getInstance(getEnvironment().getProject()).getContext();
        if (context == null) {
            throw new ExecutionException("Nocalhost context is null.");
        }

        var desService = NhctlUtil.getDescribeService(context);
        if (!NhctlDescribeServiceUtil.developStarted(desService)) {
            throw new ExecutionException("Service is not in dev mode.");
        }
        if ( ! isProjectPathMatched(desService)) {
            throw new ExecutionException("Project path does not match.");
        }

        var devConfig = NhctlUtil.getDevConfig(context);
        var containers = devConfig.getContainers();
        var container = containers.isEmpty() ? null : containers.get(0);
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

        NocalhostRunnerContext.Command command = new NocalhostRunnerContext.Command(resolveRunCommand(container), resolveDebugCommand(container));
        if (!StringUtils.isNotEmpty(command.getDebug())) {
            throw new ExecutionException("Failed to resolve debug command.");
        }

        String port = resolveDebugPort(container);
        if (!StringUtils.isNotEmpty(port)) {
            throw new ExecutionException("Remote debug port is not configured.");
        }

        refContext.set(new NocalhostRunnerContext(
                null,
                container.getDev().getShell(),
                command,
                context,
                container
        ));
    }

    private boolean isProjectPathMatched(@NotNull NhctlDescribeService nhctlDescribeService) {
        var basePath = Paths.get(getEnvironment().getProject().getBasePath()).toString();
        for (String path : nhctlDescribeService.getLocalAbsoluteSyncDirFromDevStartPlugin()) {
            if (StringUtils.equals(basePath, path)) {
                return true;
            }
        }
        return false;
    }

    private String resolveRunCommand(ServiceContainer container) {
        if (container == null
                || container.getDev() == null
                || container.getDev().getCommand() == null
                || container.getDev().getCommand().getRun() == null) {
            return null;
        }
        return String.join(" ", container.getDev().getCommand().getRun());
    }

    private String resolveDebugCommand(ServiceContainer container) {
        if (container == null
                || container.getDev() == null
                || container.getDev().getCommand() == null
                || container.getDev().getCommand().getDebug() == null) {
            return null;
        }
        return String.join(" ", container.getDev().getCommand().getDebug());
    }

    public @Nullable NocalhostRunnerContext getRunnerContext() {
        return refContext.get();
    }

    public void startup() throws ExecutionException, IOException, NocalhostExecuteCmdException, InterruptedException {
        NocalhostRunnerContext dev = refContext.get();
        if (dev == null) {
            throw new ExecutionException("Call prepare() before this method");
        }
        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(dev.getContext().getKubeConfigPath(), dev.getContext().getNamespace());
        nhctlDescribeOptions.setDeployment(dev.getContext().getServiceName());
        nhctlDescribeOptions.setType(dev.getContext().getServiceType());

        NhctlCommand command = ApplicationManager.getApplication().getService(NhctlCommand.class);
        NhctlDescribeService nhctlDescribeService = command.describe(
                dev.getContext().getApplicationName(),
                nhctlDescribeOptions,
                NhctlDescribeService.class);

        if (!NhctlDescribeServiceUtil.developStarted(nhctlDescribeService)) {
            throw new ExecutionException("Service is not in dev mode.");
        }

        String debug = dev.getCommand().getDebug();
        String shell = StringUtils.isNotEmpty(dev.getShell()) ? dev.getShell() : DEFAULT_SHELL;

        List<String> lines = Lists.newArrayList(
                NhctlUtil.binaryPath(), "exec", dev.getContext().getApplicationName(),
                "--deployment", dev.getContext().getServiceName(),
                "--controller-type", dev.getContext().getServiceType(),
                "--command", shell, "--command", "-c", "--command", debug,
                "--kubeconfig", dev.getContext().getKubeConfigPath().toString(),
                "--namespace", dev.getContext().getNamespace()
        );

        createTunnel(dev.getContainer());
        // Wait for SSH tunnel to be created
        Thread.sleep(2000);
        createClient(lines);
        createReload(dev.getContainer());
    }

    public void destroy() {
        disposables.forEach(it -> it.dispose());
        disposables.clear();
    }

    private String resolveDebugPort(ServiceContainer serviceContainer) {
        if (serviceContainer == null
                || serviceContainer.getDev() == null
                || serviceContainer.getDev().getDebug() == null) {
            return null;
        }
        return serviceContainer.getDev().getDebug().getRemoteDebugPort();
    }

    private void createTunnel(ServiceContainer container) throws ExecutionException {
        var port = resolveDebugPort(container);
        var project = getEnvironment().getProject();
        var context = NocalhostContextManager.getInstance(project).getContext();

        if (StringUtils.isEmpty(port)) {
            throw new ExecutionException("Failed to resolve remote debug port.");
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
        bus.action(withNewLine("[cmd] " + cmd.getCommandLineString()));

        var process = cmd.createProcess();
        disposables.add(() -> process.destroy());
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8);
            try (var br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    bus.action(withNewLine(line));
                }
                var code = process.waitFor();
                if (code != 0) {
                    bus.action(withNewLine("[ssh] Process finished with exit code " + code));
                }
            } catch (Exception ex) {
                LOG.error(ex);
            }
        });
    }

    private void createClient(List<String> lines) throws ExecutionException {
        var cmd = new GeneralCommandLine(lines).withRedirectErrorStream(true);
        var bus = getEnvironment()
                .getProject()
                .getMessageBus()
                .syncPublisher(NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC);
        bus.action(withNewLine("[cmd] " + cmd.getCommandLineString()));

        var process = cmd.createProcess();
        disposables.add(() -> {
            var output = process.getOutputStream();
            try {
                output.write(3);
                output.flush();
            } catch (IOException ex) {
                LOG.warn("[exec] Fail to send ctrl+c to remote process", ex);
            }
        });
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8);
            try (var br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    bus.action(withNewLine(line));
                }
                var code = process.waitFor();
                if (code != 0) {
                    bus.action(withNewLine("[exec] Process finished with exit code " + code));
                }
            } catch (Exception ex) {
                LOG.error(ex);
            }
        });
    }

    private void createReload(ServiceContainer container) throws ExecutionException {
        if (container.getDev().isHotReload()) {
            disposables.add(new HotReload(getEnvironment()).withExec());
        }
    }

    private @NotNull String withNewLine(String text) {
        return text + System.lineSeparator();
    }
}
