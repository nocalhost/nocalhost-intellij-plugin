package dev.nocalhost.plugin.intellij.ui.tree.listerner.workload;

import com.google.common.collect.Lists;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.sh.run.ShRunner;

import org.apache.commons.lang3.StringUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeResult;
import dev.nocalhost.plugin.intellij.ui.tree.WorkloadNode;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class Terminal implements ActionListener {
    private final WorkloadNode node;
    private final Project project;

    public Terminal(WorkloadNode node, Project project) {
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
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
                final String podName = pods.getItems().get(0).getMetadata().getName();
                final String containerName = pods.getItems().get(0).getSpec().getContainers().get(0).getName();

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
            ApplicationManager.getApplication().invokeLater(() -> {
                ShRunner shRunner = project.getService(ShRunner.class);
                String title = String.format("%s-%s-%s Terminal", node.getDevSpace().getNamespace(), node.getDevSpace().getContext().getApplicationName(), node.getName());
                shRunner.run(cmd, System.getProperty("user.home"), title, true);
            });
        } catch (IOException | InterruptedException ioException) {
            ioException.printStackTrace();
        }
    }
}
