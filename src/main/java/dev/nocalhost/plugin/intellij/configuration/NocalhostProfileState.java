package dev.nocalhost.plugin.intellij.configuration;

import com.google.common.collect.Lists;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardEndOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.settings.NocalhostProjectSettings;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class NocalhostProfileState extends CommandLineState {
    private static final Logger LOG = Logger.getInstance(NocalhostProfileState.class);

    private static final String DEFAULT_SHELL = "sh";

    private final AtomicReference<NocalhostDevInfo> devInfoHolder = new AtomicReference<>(null);

    public NocalhostProfileState(ExecutionEnvironment environment) {
        super(environment);
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
        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(devModeService.getRawKubeConfig());

        List<String> commandLine = Lists.newArrayList(
                NhctlUtil.binaryPath(), "exec", devModeService.getApplicationName(),
                "--deployment", nocalhostDevInfo.getDevModeService().getServiceName(),
                "--command", shell, "--command", "-c", "--command", command,
                "--kubeconfig", kubeConfigPath.toString(),
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
            ServiceProjectPath devModeService = getDevModeService();
            if (devModeService == null) {
                throw new ExecutionException("Service is not in dev mode.");
            }

            NhctlDescribeService nhctlDescribeService = getNhctlDescribeService(devModeService);
            if (!nhctlDescribeService.isDeveloping() || !projectPathMatched(nhctlDescribeService)) {
                throw new ExecutionException("Service is not in dev mode.");
            }

            List<ServiceContainer> containers = nhctlDescribeService.getRawConfig().getContainers();
            ServiceContainer serviceContainer = containers.isEmpty() ? null : containers.get(0);
            if (StringUtils.isNotEmpty(devModeService.getContainerName())) {
                for (ServiceContainer c : containers) {
                    if (StringUtils.equals(devModeService.getContainerName(), c.getName())) {
                        serviceContainer = c;
                        break;
                    }
                }
            }
            if (serviceContainer == null) {
                throw new ExecutionException("Service container config not found.");
            }

            NocalhostDevInfo.Command command = new NocalhostDevInfo.Command(resolveRunCommand(serviceContainer), resolveDebugCommand(serviceContainer));
            NocalhostDevInfo.Debug debug = null;
            if (isDebugExecutor()) {
                if (!StringUtils.isNotEmpty(command.getDebug())) {
                    throw new ExecutionException("Debug command not configured");
                }

                String remotePort = resolveDebugPort(serviceContainer);
                if (!StringUtils.isNotEmpty(remotePort)) {
                    throw new ExecutionException("Remote debug port not configured.");
                }
                String localPort = startDebugPortForward(devModeService, remotePort);
                debug = new NocalhostDevInfo.Debug(remotePort, localPort);
            } else {
                if (!StringUtils.isNotEmpty(command.getRun())) {
                    throw new ExecutionException("Run command not configured");
                }
            }

            devInfoHolder.set(new NocalhostDevInfo(
                    command,
                    debug,
                    serviceContainer.getDev().getShell(),
                    devModeService
            ));
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException | ExecutionException e) {
            throw new ExecutionException(e);
        }
    }

    private boolean isDebugExecutor() {
        return StringUtils.equals(DefaultDebugExecutor.EXECUTOR_ID, getEnvironment().getExecutor().getId());
    }

    private String startDebugPortForward(ServiceProjectPath devModeService, String remotePort) throws ExecutionException {
        NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(devModeService.getRawKubeConfig());

        try {
            KubeResource deployment = kubectlCommand.getResource("deployment", devModeService.getServiceName(), kubeConfigPath, devModeService.getNamespace());
            List<KubeResource> pods = kubectlCommand.getResourceList("pods", deployment.getSpec().getSelector().getMatchLabels(), kubeConfigPath, devModeService.getNamespace()).getItems();
            KubeResource selectedPod = null;
            for (KubeResource pod : pods) {
                for (KubeResource.Spec.Container container : pod.getSpec().getContainers()) {
                    if (StringUtils.equals(container.getName(), "nocalhost-dev")) {
                        selectedPod = pod;
                        break;
                    }
                }
            }
            if (selectedPod == null) {
                throw new ExecutionException("Pod not found");
            }

            NhctlPortForwardStartOptions nhctlPortForwardStartOptions = new NhctlPortForwardStartOptions(kubeConfigPath, devModeService.getNamespace());
            nhctlPortForwardStartOptions.setDevPorts(List.of(":" + remotePort));
            nhctlPortForwardStartOptions.setWay(NhctlPortForwardStartOptions.Way.MANUAL);
            nhctlPortForwardStartOptions.setDeployment(devModeService.getServiceName());
            nhctlPortForwardStartOptions.setPod(selectedPod.getMetadata().getName());
            nhctlCommand.startPortForward(devModeService.getApplicationName(), nhctlPortForwardStartOptions);

            NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(kubeConfigPath, devModeService.getNamespace());
            nhctlDescribeOptions.setDeployment(devModeService.getServiceName());
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
                NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
                Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(devModeService.getRawKubeConfig());

                NhctlPortForwardEndOptions nhctlPortForwardEndOptions = new NhctlPortForwardEndOptions(kubeConfigPath, devModeService.getNamespace());
                nhctlPortForwardEndOptions.setPort(debug.getLocalPort() + ":" + debug.getRemotePort());
                nhctlPortForwardEndOptions.setDeployment(devModeService.getServiceName());

                nhctlCommand.endPortForward(devModeService.getApplicationName(), nhctlPortForwardEndOptions);
            } catch (Exception e) {
                LOG.error(e);
            }
        });
    }

    private ServiceProjectPath getDevModeService() {
        final NocalhostProjectSettings nocalhostProjectSettings =
                getEnvironment().getProject().getService(NocalhostProjectSettings.class);
        return nocalhostProjectSettings.getDevModeService();
    }

    private NhctlDescribeService getNhctlDescribeService(ServiceProjectPath serviceProjectPath)
            throws InterruptedException, NocalhostExecuteCmdException, IOException {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(serviceProjectPath.getRawKubeConfig());
        NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, serviceProjectPath.getNamespace());
        opts.setDeployment(serviceProjectPath.getServiceName());
        return nhctlCommand.describe(
                serviceProjectPath.getApplicationName(),
                opts,
                NhctlDescribeService.class);
    }

    private boolean projectPathMatched(NhctlDescribeService nhctlDescribeService) {
        String projectPath = getEnvironment().getProject().getBasePath();
        for (String path : nhctlDescribeService.getLocalAbsoluteSyncDirFromDevStartPlugin()) {
            if (StringUtils.equals(projectPath, path)) {
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
}
