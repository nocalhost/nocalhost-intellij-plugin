package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.openapi.components.ServiceManager;
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
import dev.nocalhost.plugin.intellij.ui.ContainerSelectorDialog;
import dev.nocalhost.plugin.intellij.ui.tree.node.DevSpaceNode;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class NocalhostLogWindow extends NocalhostConsoleWindow {
    private final Project project;
    private final ToolWindow toolWindow;
    private final ResourceNode node;

    private JPanel panel;
    private JBScrollPane scrollPane;
    private JBTextArea textArea;
    private String title;
    private ContainerSelectorDialog containerSelectorDialog;


    public NocalhostLogWindow(Project project, ToolWindow toolWindow, ResourceNode node) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.node = node;


        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
        final String workloadName = node.getKubeResource().getMetadata().getName();
        final DevSpace devSpace = ((DevSpaceNode) node.getParent().getParent().getParent()).getDevSpace();

        toolWindow.show();
        KubeResourceList pods = null;
        try {
            pods = kubectlCommand.getResourceList("pods", Map.of("app", workloadName), devSpace);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        if (pods != null && CollectionUtils.isNotEmpty(pods.getItems())) {
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

            String logs = null;
            try {
                logs = kubectlCommand.logs(kubeResource.getMetadata().getName(), workloadName, devSpace);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            title = String.format("%s/%s.log", kubeResource.getMetadata().getName(), workloadName);
            panel = new SimpleToolWindowPanel(true);

            textArea = new JBTextArea(logs, 24, 50);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            scrollPane = new JBScrollPane(textArea);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            panel.add(scrollPane);
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
}
