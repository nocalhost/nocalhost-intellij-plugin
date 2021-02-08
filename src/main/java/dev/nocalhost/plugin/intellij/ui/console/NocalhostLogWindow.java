package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceType;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.ui.ContainerSelectorDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class NocalhostLogWindow extends NocalhostConsoleWindow {
    private static final Logger LOG = Logger.getInstance(NocalhostLogWindow.class);

    private final Project project;
    private final ToolWindow toolWindow;
    private final ResourceNode node;

    private String title;
    private ContainerSelectorDialog containerSelectorDialog;
    private ConsoleView consoleView;
    private LogPanel panel;


    public NocalhostLogWindow(Project project, ToolWindow toolWindow, KubeResourceType type, ResourceNode node) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.node = node;


        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
        final String workloadName = node.getKubeResource().getMetadata().getName();
        final DevSpace devSpace = ((DevSpaceNode) node.getParent().getParent().getParent()).getDevSpace();
        String containerName = "";
        String podName = "";
        toolWindow.show();

        switch (type) {
            case Deployment:
                containerName = workloadName;
                KubeResourceList pods = null;
                try {
                    pods = kubectlCommand.getResourceList("pods", Map.of("app", workloadName), devSpace);
                } catch (IOException | InterruptedException | NocalhostExecuteCmdException e) {
                    LOG.error("error occurred while getting workload pods", e);
                    NocalhostNotifier.getInstance(project).notifyError("Nocalhost log error", String.format("error occurred while getting workload pods workloadName:[%s] devSpace:[%s]", workloadName, devSpace), e.getMessage());
                    return;
                }
                if (pods != null && CollectionUtils.isNotEmpty(pods.getItems())) {
                    KubeResource kubeResource;
                    String[] containers = pods.getItems().stream().map(r -> r.getMetadata().getName()).toArray(String[]::new);
                    if (containers.length > 1) {
                        containerSelectorDialog = new ContainerSelectorDialog(containers);
                        containerSelectorDialog.showAndGet();
                        String currentPod = containerSelectorDialog.getCurrent();

                        Optional<KubeResource> optionalKubeResource = pods.getItems().stream().filter(r -> r.getMetadata().getName().equals(currentPod)).findFirst();
                        if (optionalKubeResource.isPresent()) {
                            kubeResource = optionalKubeResource.get();
                        } else {
                            return;
                        }
                    } else {
                        kubeResource = pods.getItems().get(0);
                    }
                    podName = kubeResource.getMetadata().getName();
                }
                break;
            case Daemonset:
                break;
            case Statefulset:
                break;
            case Job:
                break;
            case CronJobs:
                break;
            case Pod:
                podName = node.getKubeResource().getMetadata().getName();
                containerName = node.getKubeResource().getSpec().getContainers().get(0).getName();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }

        title = String.format("%s/%s.log", podName, containerName);


        consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();

        panel = new LogPanel(false);
        panel.add(consoleView.getComponent());
        AnAction[] consoleActions = consoleView.createConsoleActions();
        AnAction[] consoleViewActions = ArrayUtils.subarray(consoleActions, 2, consoleActions.length);
        DefaultActionGroup actionGroup = new DefaultActionGroup(consoleViewActions);

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar("Nocalhost.Log.Window.Toolbar", actionGroup, false);
        panel.setToolbar(actionToolbar.getComponent());

        ProcessHandler logsProcessHandler;
        try {
            logsProcessHandler = kubectlCommand.getLogsProcessHandler(podName, containerName, devSpace);
            logsProcessHandler.startNotify();
            consoleView.attachToProcess(logsProcessHandler);
            consoleView.print(
                    "",
                    ConsoleViewContentType.LOG_INFO_OUTPUT);
        } catch (ExecutionException e) {
            NocalhostNotifier.getInstance(project).notifyError("Nocalhost log error", String.format("failed to log podName:[%s] containerName:[%s] devSpace:[%s]", podName, containerName, devSpace), e.getMessage());
        }
    }

    public ConsoleView getConsoleView() {
        return consoleView;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public JComponent getPanel() {
        return panel;
    }
}
