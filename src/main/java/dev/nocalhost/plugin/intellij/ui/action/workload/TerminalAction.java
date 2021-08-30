package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.google.common.collect.Lists;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDescribeService;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetResource;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.Container;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.KubeResource;
import dev.nocalhost.plugin.intellij.ui.dialog.ListChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import dev.nocalhost.plugin.intellij.utils.TerminalUtil;

public class TerminalAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public TerminalAction(Project project, ResourceNode node) {
        super("Open Remote Terminal", "", AllIcons.Debugger.Console);
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.kubeConfigPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                NhctlDescribeOptions opts = new NhctlDescribeOptions(kubeConfigPath, namespace);
                opts.setDeployment(node.resourceName());
                opts.setType(node.getKubeResource().getKind());
                NhctlDescribeService nhctlDescribeService = nhctlCommand.describe(
                        node.applicationName(), opts, NhctlDescribeService.class);
                if (NhctlDescribeServiceUtil.developStarted(nhctlDescribeService)) {
                    openDevTerminal();
                    return;
                }

                NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(kubeConfigPath, namespace);
                List<NhctlGetResource> podList = nhctlCommand.getResources("Pods", nhctlGetOptions,
                        node.getKubeResource().getSpec().getSelector().getMatchLabels());
                List<KubeResource> pods = podList.stream()
                        .map(NhctlGetResource::getKubeResource)
                        .filter(KubeResource::canSelector)
                        .collect(Collectors.toList());

                if (pods.size() == 0) {
                    ApplicationManager.getApplication().invokeLater(() ->
                            Messages.showMessageDialog("Pods are not ready. Please try later.",
                                    "Open Terminal", null));
                    return;
                }

                if (pods.size() > 1) {
                    selectPod(pods);
                    return;
                }
                selectContainer(pods.get(0));
            } catch (Exception e) {
                ErrorUtil.dealWith(project, "Loading service status error",
                        "Error occurs while loading service status", e);
            }
        });
    }

    private void openDevTerminal() {
        ApplicationManager.getApplication().invokeLater(() ->
                TerminalUtil.openTerminal(
                        project,
                        String.format(
                                "%s/%s",
                                node.applicationName(),
                                node.resourceName()
                        ),
                        new GeneralCommandLine(Lists.newArrayList(
                                NhctlUtil.binaryPath(),
                                "dev",
                                "terminal", node.applicationName(),
                                "--deployment", node.resourceName(),
                                "--kubeconfig", kubeConfigPath.toString(),
                                "--namespace", namespace,
                                "--controller-type", node.getKubeResource().getKind(),
                                "--container", "nocalhost-dev"
                        ))
                )
        );
    }

    private void selectPod(List<KubeResource> pods) {
        List<String> podNames = pods.stream()
                .map(e -> e.getMetadata().getName())
                .collect(Collectors.toList());
        ApplicationManager.getApplication().invokeLater(() -> {
            ListChooseDialog listChooseDialog = new ListChooseDialog(project, "Select Pod",
                    podNames);
            if (!listChooseDialog.showAndGet()) {
                return;
            }
            Optional<KubeResource> podOptional = pods.stream()
                    .filter(e -> StringUtils.equals(e.getMetadata().getName(), listChooseDialog.getSelectedValue()))
                    .findFirst();
            if (podOptional.isEmpty()) {
                return;
            }
            selectContainer(podOptional.get());
        });
    }

    private void selectContainer(KubeResource pod) {
        List<String> containers = pod
                .getSpec()
                .getContainers()
                .stream()
                .map(Container::getName)
                .collect(Collectors.toList());

        if (containers.size() > 1) {
            ApplicationManager.getApplication().invokeLater(() -> {
                ListChooseDialog listChooseDialog = new ListChooseDialog(project,
                        "Select Container", containers);
                if (!listChooseDialog.showAndGet()
                        || !StringUtils.isNotEmpty(listChooseDialog.getSelectedValue())) {
                    return;
                }
                openTerminal(pod.getMetadata().getName(), listChooseDialog.getSelectedValue());
            });
            return;
        }
        openTerminal(pod.getMetadata().getName(), pod.getSpec().getContainers().get(0).getName());
    }

    private void openTerminal(String podName, String containerName) {
        ApplicationManager.getApplication().invokeLater(() ->
                TerminalUtil.openTerminal(
                        project,
                        String.format(
                                "%s/%s",
                                podName,
                                containerName
                        ),
                        new GeneralCommandLine(Lists.newArrayList(
                                NhctlUtil.binaryPath(), "k", "exec", podName,
                                "--stdin",
                                "--tty",
                                "--container", containerName,
                                "--kubeconfig", kubeConfigPath.toString(),
                                "--namespace", namespace,
                                "--", "sh", "-c", "clear; (zsh || bash || ash || sh)"
                        ))
                ));
    }

}
