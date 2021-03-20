package dev.nocalhost.plugin.intellij.configuration;

import com.google.common.collect.Lists;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RemoteState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.util.Pair;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForward;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.settings.NocalhostProjectSettings;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class NocalhostProfileState extends CommandLineState implements RemoteState {
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

        String executorId = getEnvironment().getExecutor().getId();
        switch (executorId) {
            case DefaultRunExecutor.EXECUTOR_ID:
                return startRunProcess();
            case DefaultDebugExecutor.EXECUTOR_ID:
                return startDebugProcess();
            default:
                throw new ExecutionException(MessageFormat.format("Executor '{0}' is unable to execute nocalhost configuration.", executorId));
        }
    }

    @Override
    public RemoteConnection getRemoteConnection() {
        NocalhostDevInfo nocalhostDevInfo = devInfoHolder.get();
        return new RemoteConnection(true, "127.0.0.1", nocalhostDevInfo.getDebug().getLocalPort(), false);
    }

    public void prepareDevInfo() throws ExecutionException {
        try {
            DevModeService devModeService = getDevModeService();
            if (devModeService == null) {
                throw new ExecutionException("Service is not in dev mode.");
            }

            Pair<DevSpace, Application> pair = getDevSpaceAndApplication(devModeService);
            if (pair == null) {
                throw new ExecutionException("Service is not in dev mode.");
            }
            DevSpace devSpace = pair.first;
            Application app = pair.second;

            NhctlDescribeService nhctlDescribeService = getNhctlDescribeService(devModeService, devSpace, app);
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
            if (StringUtils.equals(DefaultDebugExecutor.EXECUTOR_ID, getEnvironment().getExecutor().getId())) {
                String remotePort = resolveDebugPort(serviceContainer);
                if (!StringUtils.isNotEmpty(remotePort)) {
                    throw new ExecutionException("Remote debug port not configured.");
                }
                String localPort = startDebugPortForward(devSpace, app, devModeService, remotePort);
                debug = new NocalhostDevInfo.Debug(remotePort, localPort);
            }

            devInfoHolder.set(new NocalhostDevInfo(
                    command,
                    debug,
                    serviceContainer.getDev().getShell(),
                    devSpace,
                    app,
                    devModeService
            ));
        } catch (NocalhostApiException | IOException | InterruptedException | NocalhostExecuteCmdException | ExecutionException e) {
            throw new ExecutionException(e);
        }
    }

    private @NotNull ProcessHandler startRunProcess() throws ExecutionException {
        NocalhostDevInfo nocalhostDevInfo = devInfoHolder.get();
        String shell = StringUtils.isNotEmpty(nocalhostDevInfo.getShell()) ? nocalhostDevInfo.getShell() : DEFAULT_SHELL;
        List<String> command = Lists.newArrayList(
                "nhctl", "exec", nocalhostDevInfo.getApplication().getContext().getApplicationName(),
                "--deployment", nocalhostDevInfo.getDevModeService().getServiceName(),
                "--command", shell, "--command", "-c", "--command", nocalhostDevInfo.getCommand().getRun(),
                "--kubeconfig", KubeConfigUtil.kubeConfigPath(nocalhostDevInfo.getDevSpace()).toString(),
                "--namespace", nocalhostDevInfo.getDevSpace().getNamespace()
        );
        return new NocalhostDevProcessHandler(new GeneralCommandLine(command), getEnvironment());
    }

    private @NotNull ProcessHandler startDebugProcess() throws ExecutionException {
        NocalhostDevInfo nocalhostDevInfo = devInfoHolder.get();
        String shell = StringUtils.isNotEmpty(nocalhostDevInfo.getShell()) ? nocalhostDevInfo.getShell() : DEFAULT_SHELL;
        List<String> command = Lists.newArrayList(
                "nhctl", "exec", nocalhostDevInfo.getApplication().getContext().getApplicationName(),
                "--deployment", nocalhostDevInfo.getDevModeService().getServiceName(),
                "--command", shell, "--command", "-c", "--command", nocalhostDevInfo.getCommand().getDebug(),
                "--kubeconfig", KubeConfigUtil.kubeConfigPath(nocalhostDevInfo.getDevSpace()).toString(),
                "--namespace", nocalhostDevInfo.getDevSpace().getNamespace()
        );
        return new NocalhostDevProcessHandler(new GeneralCommandLine(command), getEnvironment(), nocalhostDevInfo);
    }

    private String startDebugPortForward(DevSpace devSpace, Application app, DevModeService devModeService, String remotePort) throws ExecutionException {
        NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);

        try {
            KubeResource deployment = kubectlCommand.getResource("deployment", devModeService.getServiceName(), devSpace);
            List<KubeResource> pods = kubectlCommand.getResourceList("pods", deployment.getSpec().getSelector().getMatchLabels(), devSpace).getItems();
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

            NhctlPortForwardStartOptions nhctlPortForwardStartOptions = new NhctlPortForwardStartOptions(devSpace);
            nhctlPortForwardStartOptions.setDevPorts(List.of(":" + remotePort));
            nhctlPortForwardStartOptions.setWay(NhctlPortForwardStartOptions.Way.MANUAL);
            nhctlPortForwardStartOptions.setDeployment(devModeService.getServiceName());
            nhctlPortForwardStartOptions.setPod(selectedPod.getMetadata().getName());
            nhctlCommand.startPortForward(app.getContext().getApplicationName(), nhctlPortForwardStartOptions);

            NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions(devSpace);
            nhctlDescribeOptions.setDeployment(devModeService.getServiceName());
            NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(app.getContext().getApplicationName(), nhctlDescribeOptions, NhctlDescribeService.class);

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

    private DevModeService getDevModeService() {
        final NocalhostProjectSettings nocalhostProjectSettings =
                getEnvironment().getProject().getService(NocalhostProjectSettings.class);
        return nocalhostProjectSettings.getDevModeService();
    }

    private static Pair<DevSpace, Application> getDevSpaceAndApplication(
            DevModeService devModeService
    ) throws IOException, NocalhostApiException {
        final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
        for (DevSpace devSpace : nocalhostApi.listDevSpaces()) {
            for (Application app : nocalhostApi.listApplications()) {
                if (app.getId() == devModeService.getApplicationId()
                        && devSpace.getId() == devModeService.getDevSpaceId()) {
                    return Pair.create(devSpace, app);
                }
            }
        }
        return null;
    }

    private static NhctlDescribeService getNhctlDescribeService(
            DevModeService devModeService,
            DevSpace devSpace,
            Application app
    ) throws InterruptedException, NocalhostExecuteCmdException, IOException {
        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        NhctlDescribeOptions opts = new NhctlDescribeOptions(devSpace);
        opts.setDeployment(devModeService.getServiceName());
        return nhctlCommand.describe(
                app.getContext().getApplicationName(),
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
