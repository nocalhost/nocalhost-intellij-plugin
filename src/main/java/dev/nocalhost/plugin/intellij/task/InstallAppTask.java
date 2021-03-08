package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;

import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.OutputCapturedNhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeApplication;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlInstallOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainer;
import dev.nocalhost.plugin.intellij.commands.data.ServiceContainerInstall;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import lombok.SneakyThrows;

public class InstallAppTask extends Task.Backgroundable {
    private static final Logger LOG = Logger.getInstance(InstallAppTask.class);

    private final Project project;
    private final DevSpace devSpace;
    private NhctlInstallOptions opts;

    private NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

    public InstallAppTask(@Nullable Project project, DevSpace devSpace, NhctlInstallOptions opts) {
        super(project, "Installing application: " + devSpace.getContext().getApplicationName(), false);
        this.project = project;
        this.devSpace = devSpace;
        this.opts = opts;
    }


    @Override
    public void onSuccess() {
        portForward();
        final Application application = ApplicationManager.getApplication();
        DevSpaceListUpdatedNotifier publisher = application.getMessageBus()
                                                           .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC);
        publisher.action();

        NocalhostNotifier.getInstance(project).notifySuccess("Application " + devSpace.getContext().getApplicationName() + " installed", "");
    }

    private void portForward() {
        String kubeConfigPath = KubeConfigUtil.kubeConfigPath(devSpace).toString();
        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
        final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
        NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions();
        nhctlDescribeOptions.setKubeconfig(kubeConfigPath);
        NhctlDescribeApplication nhctlDescribeApplication = null;
        try {
            nhctlDescribeApplication = nhctlCommand.describe(devSpace.getContext().getApplicationName(), nhctlDescribeOptions, NhctlDescribeApplication.class);
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            NocalhostNotifier.getInstance(project).notifyError("Nocalhost describe devSpaces error", "Error occurred while describe devSpaces", e.getMessage());
            return;
        }

        KubeResourceList deploymentList = null;
        KubeResourceList statefulsetList = null;
        try {
            deploymentList = kubectlCommand.getResourceList("deployments", null, devSpace);
            statefulsetList = kubectlCommand.getResourceList("statefulsets", null, devSpace);
        } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
            NocalhostNotifier.getInstance(project).notifyError("Nocalhost get resources error", "Error occurred while get resources", e.getMessage());
            return;
        }

        final List<NhctlDescribeService> svcProfile = nhctlDescribeApplication.getSvcProfile();
        for (NhctlDescribeService nhctlDescribeService : svcProfile) {
            for (ServiceContainer container : nhctlDescribeService.getRawConfig().getContainers()) {
                final ServiceContainerInstall install = container.getInstall();
                if (install == null) {
                    continue;
                }
                final List<String> portForward = install.getPortForward();
                if (CollectionUtils.isNotEmpty(portForward)) {
                    NhctlPortForwardStartOptions nhctlPortForwardStartOptions = new NhctlPortForwardStartOptions();
                    nhctlPortForwardStartOptions.setDeployment(nhctlDescribeService.getRawConfig().getName());
                    nhctlPortForwardStartOptions.setWay(NhctlPortForwardStartOptions.Way.DEV_PORTS);
                    nhctlPortForwardStartOptions.setKubeconfig(kubeConfigPath);
                    nhctlPortForwardStartOptions.setDevPorts(portForward);

                    if (nhctlDescribeService.getRawConfig().getServiceType().equalsIgnoreCase("deployment")) {
                        final Optional<KubeResource> first = deploymentList.getItems().stream().filter(resource -> nhctlDescribeService.getRawConfig().getName().equals(resource.getMetadata().getName())).findFirst();
                        if (first.isPresent()) {
                            final KubeResource kubeResource = first.get();
                            KubeResourceList pods = null;
                            try {
                                pods = kubectlCommand.getResourceList("pods", kubeResource.getSpec().getSelector().getMatchLabels(), devSpace);
                            } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                                NocalhostNotifier.getInstance(project).notifyError("Nocalhost get resources error", "Error occurred while get resources", e.getMessage());
                                continue;
                            }
                            if (pods != null && CollectionUtils.isNotEmpty(pods.getItems())) {
                                List<String> containers = pods.getItems().stream().map(r -> r.getMetadata().getName()).collect(Collectors.toList());
                                nhctlPortForwardStartOptions.setPod(containers.get(0));
                            }
                        }
                    }

                    if (nhctlDescribeService.getRawConfig().getServiceType().equalsIgnoreCase("statefulset")) {
                        nhctlPortForwardStartOptions.setType("statefulset");
                        final Optional<KubeResource> first = statefulsetList.getItems().stream().filter(resource -> nhctlDescribeService.getRawConfig().getName().equals(resource.getMetadata().getName())).findFirst();
                        if (first.isPresent()) {
                            final KubeResource kubeResource = first.get();
                            KubeResourceList pods = null;
                            try {
                                pods = kubectlCommand.getResourceList("pods", kubeResource.getSpec().getSelector().getMatchLabels(), devSpace);
                            } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                                NocalhostNotifier.getInstance(project).notifyError("Nocalhost get resources error", "Error occurred while get resources", e.getMessage());
                                continue;
                            }
                            if (pods != null && CollectionUtils.isNotEmpty(pods.getItems())) {
                                List<String> containers = pods.getItems().stream().map(r -> r.getMetadata().getName()).collect(Collectors.toList());
                                nhctlPortForwardStartOptions.setPod(containers.get(0));
                            }
                        }
                    }
                    try {
                        outputCapturedNhctlCommand.startPortForward(devSpace.getContext().getApplicationName(), nhctlPortForwardStartOptions);
                    } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                        NocalhostNotifier.getInstance(project).notifyError("Nocalhost port forward error", "Error occurred while port forward", e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void onThrowable(@NotNull Throwable e) {
        LOG.error("error occurred while installing application", e);
        NocalhostNotifier.getInstance(project).notifyError("Nocalhost install devSpace error", "Error occurred while installing application", e.getMessage());
    }

    @SneakyThrows
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        final OutputCapturedNhctlCommand outputCapturedNhctlCommand = project.getService(OutputCapturedNhctlCommand.class);
        outputCapturedNhctlCommand.install(devSpace.getContext().getApplicationName(), opts);

        final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
        nocalhostApi.syncInstallStatus(devSpace, 1);
    }
}
