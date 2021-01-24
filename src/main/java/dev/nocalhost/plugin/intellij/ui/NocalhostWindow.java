package dev.nocalhost.plugin.intellij.ui;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.sh.run.ShRunner;
import com.intellij.ui.components.JBScrollPane;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.List;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevStartOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlPortForwardOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlSyncOptions;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.DevSpaceListUpdatedNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostAccountChangedNotifier;
import dev.nocalhost.plugin.intellij.ui.tree.NocalhostTree;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;

public class NocalhostWindow {
    private static final String NOCALHOST_DEV_CONTAINER_NAME = "nocalhost-dev";
    private static final List<String> SUPPORTED_SHELLS = ImmutableList.of("zsh", "bash", "sh");

    private final Project project;
    private final ToolWindow toolWindow;

    private JPanel panel;
    private NocalhostTree tree;
    private JBScrollPane scrollPane;
    private final JButton loginButton;

    public NocalhostWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

        final Application application = ApplicationManager.getApplication();
        application.getMessageBus().connect().subscribe(
                NocalhostAccountChangedNotifier.NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC,
                NocalhostWindow.this::toggleContent
        );
        application.getMessageBus().connect().subscribe(
                DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC,
                NocalhostWindow.this::updateTree
        );

        devStart();

        panel = new SimpleToolWindowPanel(true, true);
        loginButton = new JButton("Login");
        tree = new NocalhostTree(project);
        scrollPane = new JBScrollPane(tree);
        panel.add(scrollPane);
        panel.add(loginButton, BorderLayout.SOUTH);

        loginButton.addActionListener(e -> {
            new LoginDialog().showAndGet();
        });

        toggleContent();
    }

    private void toggleContent() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);

        String jwt = nocalhostSettings.getJwt();
        if (StringUtils.isNotBlank(jwt)) {
            toolWindow.setTitleActions(Lists.newArrayList(
                    ActionManager.getInstance().getAction("Nocalhost.RefreshAction"),
                    ActionManager.getInstance().getAction("Nocalhost.LogoutAction")
            ));
            tree.clear();
            tree.updateDevSpaces();
            loginButton.setVisible(false);
            scrollPane.setVisible(true);
        } else {
            toolWindow.setTitleActions(Lists.newArrayList());
            scrollPane.setVisible(false);
            loginButton.setVisible(true);
        }
    }

    private void updateTree() {
        tree.updateDevSpaces();
    }

    private void devStart() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        DevModeService devModeService = nocalhostSettings.getDevModeProjectBasePath2Service().get(project.getBasePath());
        if (nocalhostSettings.getUserInfo() != null && devModeService != null) {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Starting DevMode", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {

                    final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
                    try {
                        DevSpace devSpace = null;
                        for (DevSpace s : nocalhostApi.listDevSpace()) {
                            if (s.getId() == devModeService.getApplicationId() && s.getDevSpaceId() == devModeService.getDevSpaceId()) {
                                devSpace = s;
                                break;
                            }
                        }
                        if (devSpace == null) {
                            return;
                        }

                        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
                        final String kubeconfigPath = KubeConfigUtil.kubeConfigPath(devSpace).toString();
                        final String appName = devSpace.getContext().getApplicationName();

                        final NhctlDescribeOptions nhctlDescribeOptions = new NhctlDescribeOptions();
                        nhctlDescribeOptions.setDeployment(devModeService.getName());
                        nhctlDescribeOptions.setKubeconfig(kubeconfigPath);
                        NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                                devSpace.getContext().getApplicationName(),
                                nhctlDescribeOptions,
                                NhctlDescribeService.class);

                        // check if devmode already started
                        if (nhctlDescribeService.isDeveloping()) {
                            return;
                        }

                        // nhctl dev start ...
                        NhctlDevStartOptions nhctlDevStartOptions = new NhctlDevStartOptions();
                        nhctlDevStartOptions.setDeployment(devModeService.getName());
                        nhctlDevStartOptions.setLocalSync(Lists.newArrayList(project.getBasePath()));
                        nhctlDevStartOptions.setKubeconfig(kubeconfigPath);
                        nhctlCommand.devStart(appName, nhctlDevStartOptions);

                        // wait for nocalhost-dev container started
                        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
                        KubeResource deployment;
                        String containerName;
                        do {
                            Thread.sleep(1000);
                            deployment = kubectlCommand.getResource("deployment", devModeService.getName(), devSpace);
                            KubeResourceList pods = kubectlCommand.getResourceList("pods", deployment.getMetadata().getLabels(), devSpace);
                            containerName = pods.getItems().get(0).getSpec().getContainers().get(0).getName();
                        } while (!NhctlUtil.isKubeResourceAvailable(deployment) || !StringUtils.equals(containerName, NOCALHOST_DEV_CONTAINER_NAME));

                        // nhctl sync ...
                        NhctlSyncOptions nhctlSyncOptions = new NhctlSyncOptions();
                        nhctlSyncOptions.setDeployment(devModeService.getName());
                        nhctlSyncOptions.setKubeconfig(kubeconfigPath);
                        nhctlCommand.sync(appName, nhctlSyncOptions);

                        // nhctl port-forward ...
                        NhctlPortForwardOptions nhctlPortForwardOptions = new NhctlPortForwardOptions();
                        nhctlPortForwardOptions.setDeployment(devModeService.getName());
                        nhctlPortForwardOptions.setDevPorts(nhctlDescribeService.getRawConfig().getDevPorts());
                        nhctlPortForwardOptions.setKubeconfig(kubeconfigPath);
                        nhctlCommand.portForward(appName, nhctlPortForwardOptions);

                        // start dev space terminal
                        final KubeResourceList pods = kubectlCommand.getResourceList("pods", deployment.getMetadata().getLabels(), devSpace);
                        final String podName = pods.getItems().get(0).getMetadata().getName();

                        String availableShell = "";
                        for (String shell : SUPPORTED_SHELLS) {
                            try {
                                String shellPath = kubectlCommand.exec(podName, NOCALHOST_DEV_CONTAINER_NAME, "which " + shell, devSpace);
                                if (StringUtils.isNotEmpty(shellPath)) {
                                    availableShell = shell;
                                    break;
                                }
                            } catch (RuntimeException e) {
                                // Ignore
                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (StringUtils.isNotEmpty(availableShell)) {
                            final List<String> args = Lists.newArrayList(
                                    "kubectl",
                                    "exec",
                                    "-it", podName,
                                    "-c", NOCALHOST_DEV_CONTAINER_NAME,
                                    "--kubeconfig", KubeConfigUtil.kubeConfigPath(devSpace).toString(),
                                    "--", availableShell
                            );
                            final String cmd = String.join(" ", args.toArray(new String[]{}));
                            ApplicationManager.getApplication().invokeLater(() -> {
                                ShRunner shRunner = project.getService(ShRunner.class);
                                shRunner.run(cmd, System.getProperty("user.home"), "DevSpace Terminal", true);
                            });
                        }

                        ApplicationManager.getApplication().getMessageBus()
                                .syncPublisher(DevSpaceListUpdatedNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC)
                                .action();

                        nocalhostSettings.getDevModeProjectBasePath2Service().remove(project.getBasePath());

                        Notifications.Bus.notify(new Notification("Nocalhost.Notification", "DevMode started", "", NotificationType.INFORMATION), project);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }

    public JPanel getPanel() {
        return panel;
    }
}
