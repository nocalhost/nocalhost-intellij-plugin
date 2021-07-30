package dev.nocalhost.plugin.intellij.configuration.python;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.EnvironmentUtil;
import com.jetbrains.python.debugger.remote.PyRemoteDebugCommandLineState;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetResource;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.configuration.NocalhostDevInfo;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.settings.NocalhostProjectSettings;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;
import dev.nocalhost.plugin.intellij.ui.console.NocalhostConsoleManager;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class NocalhostPythonProfileState extends PyRemoteDebugCommandLineState {
    private static final String DEFAULT_SHELL = "sh";
    private static final Logger LOG = Logger.getInstance(NocalhostPythonProfileState.class);
    private final List<Disposable> disposables = Lists.newArrayList();
    private final AtomicReference<NocalhostDevInfo> refDevInfo = new AtomicReference<>(null);

    public NocalhostPythonProfileState(@NotNull Project project, @NotNull ExecutionEnvironment env) {
        super(project, env);
    }

    protected ProcessHandler startProcess() {
        return new NocalhostPythonDevProcessHandler(getEnvironment(), this);
    }

    public void prepare() throws ExecutionException {
        ServiceProjectPath devService = getDevModeService();
        if (devService == null) {
            throw new ExecutionException("Service is not in dev mode.");
        }

        NhctlDescribeService desService = getNhctlDescribeService(devService);
        if (!desService.isDeveloping() || !isProjectPathMatched(desService)) {
            throw new ExecutionException("Service is not in dev mode.");
        }

        List<ServiceContainer> containers = desService.getRawConfig().getContainers();
        ServiceContainer container = containers.isEmpty() ? null : containers.get(0);
        if (StringUtils.isNotEmpty(devService.getContainerName())) {
            for (ServiceContainer c : containers) {
                if (StringUtils.equals(devService.getContainerName(), c.getName())) {
                    container = c;
                    break;
                }
            }
        }
        if (container == null) {
            throw new ExecutionException("Service container config not found.");
        }

        NocalhostDevInfo.Command command = new NocalhostDevInfo.Command(resolveRunCommand(container), resolveDebugCommand(container));
        if (!StringUtils.isNotEmpty(command.getDebug())) {
            throw new ExecutionException("Debug command is not configured");
        }

        String port = resolveDebugPort(container);
        if ( ! StringUtils.isNotEmpty(port)) {
            throw new ExecutionException("Remote debug port is not configured.");
        }

        refDevInfo.set(new NocalhostDevInfo(
            command,
            null,
            container.getDev().getShell(),
            devService
        ));
    }

    private ServiceProjectPath getDevModeService() {
        return getEnvironment().getProject().getService(NocalhostProjectSettings.class).getDevModeService();
    }

    private NhctlDescribeService getNhctlDescribeService(ServiceProjectPath serviceProjectPath) throws ExecutionException {
        try {
            NhctlCommand command = ServiceManager.getService(NhctlCommand.class);
            Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(serviceProjectPath.getRawKubeConfig());
            NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, serviceProjectPath.getNamespace());
            opts.setDeployment(serviceProjectPath.getServiceName());
            opts.setType(serviceProjectPath.getServiceType());
            return command.describe(
                    serviceProjectPath.getApplicationName(),
                    opts,
                    NhctlDescribeService.class);
        } catch (Exception ex) {
            throw new ExecutionException(ex);
        }
    }

    private boolean isProjectPathMatched(NhctlDescribeService nhctlDescribeService) {
        String projectPath = getEnvironment().getProject().getBasePath();
        for (String path : nhctlDescribeService.getLocalAbsoluteSyncDirFromDevStartPlugin()) {
            if (StringUtils.equals(projectPath, path)) {
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

    public void doStartupDebug() throws ExecutionException, IOException, NocalhostExecuteCmdException, InterruptedException {
        NocalhostDevInfo info = refDevInfo.get();
        if (info == null) {
            throw new ExecutionException("Call prepare() before this method");
        }
        ServiceProjectPath devService = info.getDevModeService();
        String shell = StringUtils.isNotEmpty(info.getShell()) ? info.getShell() : DEFAULT_SHELL;
        String debug = info.getCommand().getDebug();
        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(devService.getRawKubeConfig());

        List<String> lines = Lists.newArrayList(
            NhctlUtil.binaryPath(), "exec", devService.getApplicationName(),
            "--deployment", info.getDevModeService().getServiceName(),
            "--command", shell, "--command", "-c", "--command", debug,
            "--kubeconfig", kubeConfigPath.toString(),
            "--namespace", devService.getNamespace()
        );

        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath, devService.getNamespace());
        nhctlDescribeOptions.setDeployment(devService.getServiceName());
        nhctlDescribeOptions.setType(devService.getServiceType());

        NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                devService.getApplicationName(),
                nhctlDescribeOptions,
                NhctlDescribeService.class);

        if ( ! nhctlDescribeService.isDeveloping()) {
            throw new ExecutionException("Service is not in dev mode.");
        }

        List<ServiceContainer> containers = nhctlDescribeService.getRawConfig().getContainers();
        ServiceContainer container = containers.isEmpty() ? null : containers.get(0);
        if (StringUtils.isNotEmpty(devService.getContainerName())) {
            for (ServiceContainer c : containers) {
                if (StringUtils.equals(devService.getContainerName(), c.getName())) {
                    container = c;
                    break;
                }
            }
        }
        if (container == null) {
            throw new ExecutionException("Service container config not found.");
        }

        // SSH tunnel
        createTunnel(container);
        // Wait for SSH tunnel to be created
        Thread.sleep(2000);
        Disposable disposable = NocalhostConsoleManager.openTerminalWindow(
            getEnvironment().getProject(),
            "debug",
            new GeneralCommandLine(lines)
        );
        if (disposable != null) {
            disposables.add(disposable);
        }
    }

    public void doDestroyDebug() {
        disposables.forEach(it -> it.dispose());
        disposables.clear();
    }

    private String getDevPodName() throws IOException, InterruptedException, ExecutionException, NocalhostExecuteCmdException {
        ServiceProjectPath service = getDevModeService();
        NhctlCommand command = ServiceManager.getService(NhctlCommand.class);
        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(service.getRawKubeConfig());

        NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(kubeConfigPath, service.getNamespace());
        Optional<NhctlGetResource> deployments = command
                .getResources(service.getServiceType(), nhctlGetOptions)
                .stream()
                .filter(e -> StringUtils.equals(e.getKubeResource().getMetadata().getName(), service.getServiceName()))
                .findFirst();
        if (deployments.isEmpty()) {
            throw new ExecutionException("Service not found");
        }

        Optional<NhctlGetResource> pods = command
                .getResources("Pods", nhctlGetOptions, deployments.get().getKubeResource().getSpec().getSelector().getMatchLabels())
                .stream()
                .filter(e -> e.getKubeResource().getSpec().getContainers().stream().anyMatch(c -> StringUtils.equals(c.getName(), "nocalhost-dev")))
                .findFirst();
        if (pods.isEmpty()) {
            throw new ExecutionException("Pod not found");
        }

        return pods.get().getKubeResource().getMetadata().getName();
    }

    private String resolveDebugPort(ServiceContainer serviceContainer) {
        if (serviceContainer == null
                || serviceContainer.getDev() == null
                || serviceContainer.getDev().getDebug() == null) {
            return null;
        }
        return serviceContainer.getDev().getDebug().getRemoteDebugPort();
    }

    private void createTunnel(ServiceContainer container) throws ExecutionException, NocalhostExecuteCmdException, IOException, InterruptedException {
        String port = resolveDebugPort(container);
        Project project = getEnvironment().getProject();
        ServiceProjectPath service = getDevModeService();
        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(service.getRawKubeConfig());

        GeneralCommandLine cmd = new GeneralCommandLine(Lists.newArrayList(
                NhctlUtil.binaryPath(), "ssh", "reverse",
                "--pod", getDevPodName(),
                "--local", port,
                "--remote", port,
                "--sshport", "50022",
                "--namespace", service.getNamespace(),
                "--kubeconfig", kubeConfigPath.toString()
        ));

        Process process;

        try {
            process = cmd.createProcess();
        } catch (ExecutionException ex) {
            throw new NocalhostExecuteCmdException(cmd.getCommandLineString(), -1, ex.getMessage());
        }

        NocalhostOutputAppendNotifier bus = project
                .getMessageBus()
                .syncPublisher(NocalhostOutputAppendNotifier.NOCALHOST_OUTPUT_APPEND_NOTIFIER_TOPIC);
        bus.action("[cmd] " + cmd.getCommandLineString() + System.lineSeparator());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            StringBuilder sb = new StringBuilder();
            InputStreamReader reader = new InputStreamReader(process.getInputStream(), Charsets.UTF_8);
            try (BufferedReader br = new BufferedReader(reader)) {
                String line;
                while ((line = br.readLine()) != null) {
                    bus.action(line + System.lineSeparator());
                    sb.append(line).append(System.lineSeparator());
                }
                int code = process.waitFor();;
                if (code != 0) {
                    bus.action("Process finished with exit code " + code + System.lineSeparator());
                }
            } catch (Exception ex) {
                LOG.error(ex);
            }
        });

        disposables.add(() -> process.destroy());
    }
}
