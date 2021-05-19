package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.topic.NocalhostConsoleExecuteNotifier;
import dev.nocalhost.plugin.intellij.ui.console.Action;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;
import icons.TerminalIcons;

public class TerminalAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(TerminalAction.class);

    private final Project project;
    private final ResourceNode node;

    public TerminalAction(Project project, ResourceNode node) {
        super("Terminal", "", TerminalIcons.OpenTerminal_13x13);
        this.project = project;
        this.node = node;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        project.getMessageBus()
               .syncPublisher(NocalhostConsoleExecuteNotifier.NOCALHOST_CONSOLE_EXECUTE_NOTIFIER_TOPIC)
               .action(node, Action.TERMINAL);
    }
}
