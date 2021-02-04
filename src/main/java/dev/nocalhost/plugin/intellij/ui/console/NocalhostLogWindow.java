package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceType;
import dev.nocalhost.plugin.intellij.ui.ContainerSelectorDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class NocalhostLogWindow extends NocalhostConsoleWindow {
    private static final Logger LOG = Logger.getInstance(NocalhostLogWindow.class);

    private final Project project;
    private final ToolWindow toolWindow;
    private final ResourceNode node;

    private JPanel panel;
    private JBScrollPane scrollPane;
    private JBTextArea textArea;
    private String title;
    private ContainerSelectorDialog containerSelectorDialog;


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

        String logs = null;

        switch (type) {
            case Deployment:
                containerName = workloadName;
                KubeResourceList pods = null;
                try {
                    pods = kubectlCommand.getResourceList("pods", Map.of("app", workloadName), devSpace);
                } catch (IOException | InterruptedException e) {
                    LOG.error("error occurred while getting workload pods", e);
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

        try {
            logs = kubectlCommand.logs(podName, containerName, devSpace);
        } catch (IOException | InterruptedException e) {
            LOG.error("error occurred while getting workload log", e);
            return;
        }
        title = String.format("%s/%s.log", podName, containerName);


        panel = new SimpleToolWindowPanel(true);

        textArea = new JBTextArea(logs);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        scrollPane = new JBScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane);
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
