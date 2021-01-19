package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.components.JBTextArea;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.utils.Lists;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.*;

import dev.nocalhost.plugin.intellij.commands.KubectlCommand;
import dev.nocalhost.plugin.intellij.commands.data.KubeResource;
import dev.nocalhost.plugin.intellij.commands.data.KubeResourceList;
import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleExecuteNotifier;
import dev.nocalhost.plugin.intellij.ui.console.ConsolePanel;
import dev.nocalhost.plugin.intellij.ui.console.LogPanel;
import dev.nocalhost.plugin.intellij.ui.tree.WorkloadNode;

public class NocalhostConsoleWindow {
    private final Project project;
    private final ToolWindow toolWindow;

    private JPanel panel;
    private JBTabbedPane tabbedPane;
    private List<ConsolePanel> tabs;

    public NocalhostConsoleWindow(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;

        panel = new SimpleToolWindowPanel(true, true);

        tabbedPane = new JBTabbedPane(JTabbedPane.TOP);
        tabs = Lists.newArrayList();
        final Application application = ApplicationManager.getApplication();
        application.getMessageBus().connect().subscribe(
                NocalhostConsoleExecuteNotifier.NOCALHOST_CONSOLE_EXECUTE_NOTIFIER_TOPIC,
                this::updateTab
        );
        panel.add(tabbedPane);
    }

    private void updateTab(WorkloadNode node) {
        final KubectlCommand kubectlCommand = ServiceManager.getService(KubectlCommand.class);
        toolWindow.show();
        KubeResourceList list = null;
        try {
            list = kubectlCommand.getResourceList("pods", Map.of("app", node.getName()), node.getDevSpace());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        if (list != null && CollectionUtils.isNotEmpty(list.getItems())) {
            KubeResource kubeResource = list.getItems().get(0);
            String logs = null;
            try {
                logs = kubectlCommand.logs(kubeResource.getMetadata().getName(), node.getName(), node.getDevSpace());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            String title = String.format("%s/%s.log", kubeResource.getMetadata().getName(), node.getName());

            Optional<ConsolePanel> tabOptional = tabs.stream().filter(tab -> tab.getTitle().equals(title)).findFirst();
            LogPanel logPanel;
            if (tabOptional.isPresent()) {
                logPanel = (LogPanel) tabOptional.get();
                logPanel.getPanel().removeAll();
                JBTextArea area = new JBTextArea(logs);
                area.setEditable(false);
                logPanel.getPanel().add(area);
                logPanel.getPanel().repaint();
            } else {
                logPanel = new LogPanel(title, null, project);
                JBTextArea area = new JBTextArea(logs);
                area.setEditable(false);
                logPanel.getPanel().add(area);
                tabs.add(logPanel);
                tabbedPane.add(logPanel.getTitle(), logPanel.getPanel());

            }
            tabbedPane.repaint();
            int index = tabs.indexOf(logPanel);
            tabbedPane.setSelectedIndex(index);

        }
    }

    public JComponent getPanel() {
        return panel;
    }
}
