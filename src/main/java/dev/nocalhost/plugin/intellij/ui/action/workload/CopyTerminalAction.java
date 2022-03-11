package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.google.common.collect.Lists;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetOptions;
import dev.nocalhost.plugin.intellij.commands.data.NhctlGetResource;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.Container;
import dev.nocalhost.plugin.intellij.commands.data.kuberesource.KubeResource;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.nhctl.NhctlGetCommand;
import dev.nocalhost.plugin.intellij.ui.dialog.ListChooseDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;
import dev.nocalhost.plugin.intellij.utils.KubeResourceUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlDescribeServiceUtil;
import dev.nocalhost.plugin.intellij.utils.NhctlUtil;
import dev.nocalhost.plugin.intellij.utils.PathsUtil;

import static dev.nocalhost.plugin.intellij.utils.Constants.WORKLOAD_TYPE_POD;

public class CopyTerminalAction extends DumbAwareAction {
    private final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);

    private final Project project;
    private final ResourceNode node;
    private final Path kubeConfigPath;
    private final String namespace;

    public CopyTerminalAction(Project project, ResourceNode node) {
        super("Copy Terminal Exec Command");
        this.project = project;
        this.node = node;
        this.kubeConfigPath = KubeConfigUtil.toPath(node.getClusterNode().getRawKubeConfig());
        this.namespace = node.getNamespaceNode().getNamespace();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                var desService = NhctlUtil.getDescribeService(
                        project, node.resourceName(), node.controllerType(),
                        namespace, node.applicationName(), kubeConfigPath
                );
                if (NhctlDescribeServiceUtil.developStarted(desService)) {
                    copyDevTerminal();
                    return;
                }

                if (StringUtils.equalsIgnoreCase(node.controllerType(), WORKLOAD_TYPE_POD)) {
                    selectContainer(node.getKubeResource());
                    return;
                }

                NhctlGetOptions nhctlGetOptions = new NhctlGetOptions(kubeConfigPath, namespace);
                List<NhctlGetResource> podList = nhctlCommand.getResources("Pods", nhctlGetOptions,
                        KubeResourceUtil.getMatchLabels(node.getKubeResource()));
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
            } catch (Exception ex) {
                ErrorUtil.dealWith(project, "Failed to load service status",
                        "Error occurred while loading service status.", ex);
            }
        });
    }

    private void copyDevTerminal() {
        GeneralCommandLine commandLine = new GeneralCommandLine(Lists.newArrayList(
                PathsUtil.backslash(NhctlUtil.binaryPath()),
                "dev",
                "terminal", node.applicationName(),
                "--deployment", node.resourceName(),
                "--kubeconfig", PathsUtil.backslash(kubeConfigPath.toString()),
                "--namespace", namespace,
                "--controller-type", node.controllerType(),
                "--container", "nocalhost-dev"
        ));

        StringSelection stringSelection = new StringSelection(commandLine.getCommandLineString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
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
                copyTerminal(pod.getMetadata().getName(), listChooseDialog.getSelectedValue());
            });
            return;
        }
        copyTerminal(pod.getMetadata().getName(), pod.getSpec().getContainers().get(0).getName());
    }

    private void copyTerminal(String podName, String containerName) {
        GeneralCommandLine commandLine = new GeneralCommandLine(Lists.newArrayList(
                PathsUtil.backslash(NhctlUtil.binaryPath()),
                "k",
                "exec",
                podName,
                "--stdin",
                "--tty",
                "--container", containerName,
                "--kubeconfig", PathsUtil.backslash(kubeConfigPath.toString()),
                "--namespace", namespace,
                "--", "sh", "-c", "clear; (zsh || bash || ash || sh)"
        ));

        StringSelection stringSelection = new StringSelection(commandLine.getCommandLineString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);

        NocalhostNotifier.getInstance(project).notifySuccess("Terminal command copied",
                "Please open terminal and paste command to open new shell.");
    }

}
