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

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.ui.ContainerSelectorDialog;
import dev.nocalhost.plugin.intellij.ui.tree.WorkloadNode;

public class NocalhostLogWindow extends NocalhostConsoleWindow {
    private final Project project;
    private final ToolWindow toolWindow;
    private final WorkloadNode node;

    private JPanel panel;
    private JBScrollPane scrollPane;
    private JBTextArea textArea;
    private String title;
    private ContainerSelectorDialog containerSelectorDialog;


    public NocalhostLogWindow(Project project, ToolWindow toolWindow, WorkloadNode node) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.node = node;

        scrollPane = new JBScrollPane();


        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
        toolWindow.show();
        KubeResourceList pods = null;
        try {
            pods = kubectlCommand.getResourceList("pods", Map.of("app", node.getName()), node.getDevSpace());
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
                logs = kubectlCommand.logs(kubeResource.getMetadata().getName(), node.getName(), node.getDevSpace());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            title = String.format("%s/%s.log", kubeResource.getMetadata().getName(), node.getName());
            panel = new SimpleToolWindowPanel(true);

            textArea = new JBTextArea(logs, 24, 50);
            textArea.setEditable(false);
//            scrollPane.add(textArea);
//            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
//            scrollPane.setVisible(true);
//            panel.setPreferredSize(new Dimension(450, 110));
//            panel.add(scrollPane);
            panel.add(textArea);
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
