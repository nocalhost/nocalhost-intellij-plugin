package dev.nocalhost.plugin.intellij.ui.console;

import com.google.common.collect.Lists;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ToolWindow;
import com.jediterm.pty.PtyProcessTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.plugins.terminal.JBTerminalSystemSettingsProvider;
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeResult;
import dev.nocalhost.plugin.intellij.ui.ContainerSelectorDialog;
import dev.nocalhost.plugin.intellij.ui.tree.WorkloadNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class NocalhostTerminalWindow extends NocalhostConsoleWindow {
    private final Project project;
    private final ToolWindow toolWindow;
    private final WorkloadNode node;

    private JComponent panel;

    private String title;

    private ContainerSelectorDialog containerSelectorDialog;

    public NocalhostTerminalWindow(Project project, ToolWindow toolWindow, WorkloadNode node) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.node = node;

        final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);
        final NhctlDescribeOptions opts = new NhctlDescribeOptions();
        opts.setDeployment(node.getName());
        try {
            final NhctlDescribeResult describeResult = nhctlCommand.describe(node.getDevSpace().getContext().getApplicationName(), opts);
            List<String> args;
            if (describeResult.isDeveloping()) {
                args = Lists.newArrayList(
                        "nhctl",
                        "dev",
                        "terminal", node.getDevSpace().getContext().getApplicationName(),
                        "--deployment", node.getName(),
                        "--kubeconfig", KubeConfigUtil.kubeConfigPath(node.getDevSpace()).toString()
                );
            } else {
                final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
                final KubeResource deployment = kubectlCommand.getResource("deployment", node.getName(), node.getDevSpace());
                final KubeResourceList pods = kubectlCommand.getResourceList("pods", deployment.getMetadata().getLabels(), node.getDevSpace());

                KubeResource kubeResource;
                String[] containers = pods.getItems().stream().map(r -> r.getMetadata().getName()).toArray(String[]::new);
                if (containers.length > 1) {
                    containerSelectorDialog = new ContainerSelectorDialog(containers);
                    containerSelectorDialog.showAndGet();
                    String podName = containerSelectorDialog.getCurrent();

                    Optional<KubeResource> optionalKubeResource = pods.getItems().stream().filter(r -> r.getMetadata().getName().equals(podName)).findFirst();
                    if (optionalKubeResource.isPresent()) {
                        kubeResource = optionalKubeResource.get();
                    } else {
                        return;
                    }
                } else {
                    kubeResource = pods.getItems().get(0);
                }
                final String podName = kubeResource.getMetadata().getName();
                final String containerName = node.getName();

                final List<String> availableShells = Lists.newArrayList("zsh", "bash", "sh").stream().filter((shell) -> {
                    try {
                        String shellPath = kubectlCommand.exec(podName, containerName, "which " + shell, node.getDevSpace());
                        if (StringUtils.isNotEmpty(shellPath)) {
                            return true;
                        }
                    } catch (RuntimeException runtimeException) {
                        return false;
                    } catch (InterruptedException | IOException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                    return false;
                }).collect(Collectors.toList());

                args = Lists.newArrayList(
                        "kubectl",
                        "exec",
                        "-it", podName,
                        "-c", containerName,
                        "--kubeconfig", KubeConfigUtil.kubeConfigPath(node.getDevSpace()).toString(),
                        "--", availableShells.get(0)
                );

            }
            final String cmd = String.join(" ", args.toArray(new String[]{}));
            title = String.format("%s-%s-%s Terminal", node.getDevSpace().getNamespace(), node.getDevSpace().getContext().getApplicationName(), node.getName());

            LocalTerminalDirectRunner localTerminalDirectRunner = LocalTerminalDirectRunner.createTerminalRunner(project);
            PtyProcess ptyProcess = localTerminalDirectRunner.createProcess(project.getBasePath());
            TtyConnector connector = new PtyProcessTtyConnector(ptyProcess, StandardCharsets.UTF_8);
            JBTerminalSystemSettingsProvider settingsProvider = new JBTerminalSystemSettingsProvider();
            ShellTerminalWidget terminal = new ShellTerminalWidget(project, settingsProvider, new TermDisposable());
            Disposer.register(terminal, settingsProvider);
            terminal.start(connector);
            terminal.executeCommand(cmd);
            panel = terminal;
        } catch (ExecutionException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public JComponent getPanel() {
        return panel;
    }

    protected static final class TermDisposable implements Disposable {
        private volatile boolean myDisposed;

        public TermDisposable() { }

        @Override
        public void dispose() {
            myDisposed = true;
        }

        public boolean isDisposed() {
            return myDisposed;
        }
    }
}
