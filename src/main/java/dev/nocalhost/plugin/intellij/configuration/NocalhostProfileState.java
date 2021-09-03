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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlConfigOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardEndOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlRawConfig;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatus;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncStatusOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.configuration.php.NocalhostPhpDebugRunner;
import dev.nocalhost.plugin.intellij.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.service.NocalhostProjectService;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputAppendNotifier;
import dev.nocalhost.plugin.intellij.utils.DataUtils;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class NocalhostProfileState extends CommandLineState {
    private static final Logger LOG = Logger.getInstance(NocalhostProfileState.class);

    private static final String DEFAULT_SHELL = "sh";

    private final List<Disposable> disposables = Lists.newArrayList();
    private final AtomicReference<NocalhostDevInfo> devInfoHolder = new AtomicReference<>(null);
    private final NocalhostProjectService nocalhostProjectService;

    public NocalhostProfileState(ExecutionEnvironment environment) {
        super(environment);
        nocalhostProjectService = environment.getProject().getService(NocalhostProjectService.class);
    }

    @Override
    protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        NocalhostDevInfo nocalhostDevInfo = devInfoHolder.get();
        if (nocalhostDevInfo == null) {
            throw new ExecutionException("Call prepareDevInfo() before this method");
        }
        ServiceProjectPath devModeService = nocalhostDevInfo.getDevModeService();

        String shell = StringUtils.isNotEmpty(nocalhostDevInfo.getShell()) ? nocalhostDevInfo.getShell() : DEFAULT_SHELL;
        String command = isDebugExecutor() ? nocalhostDevInfo.getCommand().getDebug() : nocalhostDevInfo.getCommand().getRun();

        List<String> commandLine = Lists.newArrayList(
                NhctlUtil.binaryPath(), "exec", devModeService.getApplicationName(),
                "--deployment", nocalhostDevInfo.getDevModeService().getServiceName(),
                "--controller-type", nocalhostDevInfo.getDevModeService().getServiceType(),
                "--command", shell, "--command", "-c", "--command", command,
                "--kubeconfig", devModeService.getKubeConfigPath().toString(),
                "--namespace", devModeService.getNamespace()
        );
        return new NocalhostDevProcessHandler(new GeneralCommandLine(commandLine), getEnvironment(), this);
    }

    public String getDebugPort() {
        NocalhostDevInfo nocalhostDevInfo = devInfoHolder.get();
        return nocalhostDevInfo.getDebug().getLocalPort();
    }

    public void prepareDevInfo() throws ExecutionException {
        try {
            var devService = NhctlUtil.getDevModeService(getEnvironment().getProject());
            if (devService == null) {
                throw new ExecutionException("Service is not in dev mode.");
            }

            var desService = NhctlUtil.getDescribeService(devService);
            if (!NhctlDescribeServiceUtil.developStarted(desService) || !projectPathMatched(desService)) {
                throw new ExecutionException("Service is not in dev mode.");
            }

            if (!isSyncStatusIdle()) {
                throw new ExecutionException("File sync has not yet completed.");
            }

            NhctlRawConfig nhctlRawConfig = getNhctlConfig(devService);
            List<ServiceContainer> containers = nhctlRawConfig.getContainers();
            ServiceContainer svc = containers.isEmpty() ? null : containers.get(0);
            if (StringUtils.isNotEmpty(devService.getContainerName())) {
                for (ServiceContainer c : containers) {
                    if (StringUtils.equals(devService.getContainerName(), c.getName())) {
                        svc = c;
                        break;
                    }
                }
            }
            if (svc == null) {
                throw new ExecutionException("Service container config not found.");
            }

            NocalhostDevInfo.Command command = new NocalhostDevInfo.Command(resolveRunCommand(svc), resolveDebugCommand(svc));
            NocalhostDevInfo.Debug debug = null;
            if (isDebugExecutor()) {
                if (!StringUtils.isNotEmpty(command.getDebug())) {
                    throw new ExecutionException("Debug command not configured");
                }

                String runnerId = getEnvironment().getRunner().getRunnerId();
                if (NocalhostPhpDebugRunner.RUNNER_ID.equals(runnerId)) {
                    // PHP remote debugging use SSH tunnel
                    doCreateTunnel(svc);
                } else {
                    String remotePort = resolveDebugPort(svc);
                    if (!StringUtils.isNotEmpty(remotePort)) {
                        throw new ExecutionException("Remote debug port not configured.");
                    }
                    String localPort = startDebugPortForward(devService, remotePort);
                    debug = new NocalhostDevInfo.Debug(remotePort, localPort);
                }
            } else {
                if (!StringUtils.isNotEmpty(command.getRun())) {
                    throw new ExecutionException("Run command not configured");
                }
            }

            devInfoHolder.set(new NocalhostDevInfo(
                    debug,
                    svc.getDev().getShell(),
                    command,
                    svc,
                    devService
            ));
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException | ExecutionException e) {
            throw new ExecutionException(e);
        }
    }

    private void doCreateTunnel(ServiceContainer container) throws ExecutionException, NocalhostExecuteCmdException, IOException, InterruptedException {
        Project project = getEnvironment().getProject();
        String debugPort = resolveDebugPort(container);
        ServiceProjectPath service = nocalhostProjectService.getServiceProjectPath();

        if (!StringUtils.isNotEmpty(debugPort)) {
            throw new ExecutionException("Remote debug port not configured.");
        }

        GeneralCommandLine cmd = new GeneralCommandLine(Lists.newArrayList(
                NhctlUtil.binaryPath(), "ssh", "reverse",
                "--pod", getDevPodName(),
                "--local", debugPort,
                "--remote", debugPort,
                "--sshport", "50022",
                "--namespace", service.getNamespace(),
                "--kubeconfig", service.getKubeConfigPath().toString()
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
        bus.action("[cmd] " + cmd + System.lineSeparator());

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
                    bus.action("Process finished with exit code " + code + System.lineSeparator());
                }
            } catch (Exception ex) {
                LOG.error(ex);
            }
        });

        disposables.add(() -> process.destroy());
    }

    private boolean isDebugExecutor() {
        return StringUtils.equals(DefaultDebugExecutor.EXECUTOR_ID, getEnvironment().getExecutor().getId());
    }

    private String startDebugPortForward(ServiceProjectPath devModeService, String remotePort) throws ExecutionException {
        NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

        try {
            NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(devModeService.getKubeConfigPath(), devModeService.getNamespace());
            List<NhctlGetResource> deployments = nhctlCommand.getResources(devModeService.getServiceType(), nhctlGetOptions);
            Optional<NhctlGetResource> deploymentOptional = deployments.stream()
                    .filter(e -> StringUtils.equals(e.getKubeResource().getMetadata().getName(), devModeService.getServiceName()))
                    .findFirst();
            if (deploymentOptional.isEmpty()) {
                throw new ExecutionException("Service not found");
            }

            List<NhctlGetResource> pods = nhctlCommand.getResources("Pods", nhctlGetOptions,
                    deploymentOptional.get().getKubeResource().getSpec().getSelector().getMatchLabels());

            Optional<NhctlGetResource> podOptional = pods.stream().filter(e -> e.getKubeResource().getSpec().getContainers().stream().anyMatch(c -> StringUtils.equals(c.getName(), "nocalhost-dev"))).findFirst();
            if (podOptional.isEmpty()) {
                throw new ExecutionException("Pod not found");
            }
            String podName = podOptional.get().getKubeResource().getMetadata().getName();

            NhctlPortForwardStartOptions nhctlPortForwardStartOptions = new NhctlPortForwardStartOptions(devModeService.getKubeConfigPath(), devModeService.getNamespace());
            nhctlPortForwardStartOptions.setDevPorts(List.of(":" + remotePort));
            nhctlPortForwardStartOptions.setWay(NhctlPortForwardStartOptions.Way.MANUAL);
            nhctlPortForwardStartOptions.setDeployment(devModeService.getServiceName());
            nhctlPortForwardStartOptions.setType(devModeService.getServiceType());
            nhctlPortForwardStartOptions.setPod(podName);
            nhctlCommand.startPortForward(devModeService.getApplicationName(), nhctlPortForwardStartOptions);

            NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(devModeService.getKubeConfigPath(), devModeService.getNamespace());
            nhctlDescribeOptions.setDeployment(devModeService.getServiceName());
            nhctlDescribeOptions.setType(devModeService.getServiceType());
            NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(devModeService.getApplicationName(), nhctlDescribeOptions, NhctlDescribeService.class);

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
        NocalhostDevInfo nocalhostDevInfo = devInfoHolder.get();
        NocalhostDevInfo.Debug debug = nocalhostDevInfo.getDebug();
        if (debug == null) {
            return;
        }

        ServiceProjectPath devModeService = nocalhostDevInfo.getDevModeService();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

                NhctlPortForwardEndOptions nhctlPortForwardEndOptions = new NhctlPortForwardEndOptions(devModeService.getKubeConfigPath(), devModeService.getNamespace());
                nhctlPortForwardEndOptions.setPort(debug.getLocalPort() + ":" + debug.getRemotePort());
                nhctlPortForwardEndOptions.setDeployment(devModeService.getServiceName());
                nhctlPortForwardEndOptions.setType(devModeService.getServiceType());

                nhctlCommand.endPortForward(devModeService.getApplicationName(), nhctlPortForwardEndOptions);
            } catch (Exception e) {
                LOG.error(e);
            }
        });
    }

    private String getDevPodName() throws ExecutionException, IOException, NocalhostExecuteCmdException, InterruptedException {
        ServiceProjectPath service = nocalhostProjectService.getServiceProjectPath();
        NhctlCommand command = ApplicationManager.getApplication().getService(NhctlCommand.class);
        NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(service.getKubeConfigPath(), service.getNamespace());

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

    private boolean isSyncStatusIdle() throws IOException, NocalhostExecuteCmdException, InterruptedException {
        ServiceProjectPath serviceProjectPath = nocalhostProjectService.getServiceProjectPath();
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication()
                .getService(NhctlCommand.class);
        NhctlSyncStatusOptions opts = new NhctlSyncStatusOptions(serviceProjectPath.getKubeConfigPath(),
                serviceProjectPath.getNamespace());
        opts.setDeployment(serviceProjectPath.getServiceName());
        opts.setControllerType(serviceProjectPath.getServiceType());
        String status = nhctlCommand.syncStatus(serviceProjectPath.getApplicationName(), opts);
        NhctlSyncStatus nhctlSyncStatus = DataUtils.GSON.fromJson(status, NhctlSyncStatus.class);
        return StringUtils.equals(nhctlSyncStatus.getStatus(), "idle");
    }

    private NhctlDescribeService getNhctlDescribeService(ServiceProjectPath serviceProjectPath)
            throws InterruptedException, NocalhostExecuteCmdException, IOException {
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
        NhctlDescribeOptions opts = new NhctlDescribeOptions(serviceProjectPath.getKubeConfigPath(), serviceProjectPath.getNamespace());
        opts.setDeployment(serviceProjectPath.getServiceName());
        opts.setType(serviceProjectPath.getServiceType());
        return nhctlCommand.describe(
                serviceProjectPath.getApplicationName(),
                opts,
                NhctlDescribeService.class);
    }

    private NhctlRawConfig getNhctlConfig(ServiceProjectPath serviceProjectPath)
            throws InterruptedException, NocalhostExecuteCmdException, IOException {
        final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
        NhctlConfigOptions opts = new NhctlConfigOptions(serviceProjectPath.getKubeConfigPath(), serviceProjectPath.getNamespace());
        opts.setDeployment(serviceProjectPath.getServiceName());
        opts.setControllerType(serviceProjectPath.getServiceType());
        return nhctlCommand.getConfig(serviceProjectPath.getApplicationName(), opts, NhctlRawConfig.class);
    }

    private boolean projectPathMatched(NhctlDescribeService nhctlDescribeService) {
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
        var context = devInfoHolder.get();
        if (context == null) {
            throw new ExecutionException("Call prepareDevInfo() before this method");
        }
        if (context.getContainer().getDev().isHotReload()) {
            disposables.add(new HotReload(getEnvironment()).withExec());
        }
    }

    public void destroy() {
        disposables.forEach(x -> x.dispose());
        disposables.clear();
        stopDebugPortForward();
    }
}
