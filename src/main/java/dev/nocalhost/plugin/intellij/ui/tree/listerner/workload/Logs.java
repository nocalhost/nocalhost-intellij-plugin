package dev.nocalhost.plugin.intellij.ui.tree.listerner.workload;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleExecuteNotifier;
import dev.nocalhost.plugin.intellij.ui.console.Action;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public class Logs implements ActionListener {
    private final ResourceNode node;
    private final Project project;

    public Logs(ResourceNode node, Project project) {
        this.node = node;
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Application application = ApplicationManager.getApplication();
        NocalhostConsoleExecuteNotifier publisher = application.getMessageBus()
                .syncPublisher(NocalhostConsoleExecuteNotifier.NOCALHOST_CONSOLE_EXECUTE_NOTIFIER_TOPIC);
        publisher.action(node, Action.LOGS);
    }
}
