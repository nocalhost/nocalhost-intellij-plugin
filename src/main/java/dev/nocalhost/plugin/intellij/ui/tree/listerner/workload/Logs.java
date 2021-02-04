package dev.nocalhost.plugin.intellij.ui.tree.listerner.workload;

import com.intellij.openapi.project.Project;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import dev.nocalhost.plugin.intellij.commands.data.KubeResourceType;
import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleExecuteNotifier;
import dev.nocalhost.plugin.intellij.ui.console.Action;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class Logs implements ActionListener {
    private final ResourceNode node;
    private final KubeResourceType type;
    private final Project project;

    public Logs(ResourceNode node, KubeResourceType type, Project project) {
        this.node = node;
        this.type = type;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        project.getMessageBus()
                .syncPublisher(NocalhostConsoleExecuteNotifier.NOCALHOST_CONSOLE_EXECUTE_NOTIFIER_TOPIC)
                .action(node, type, Action.LOGS);
    }
}
