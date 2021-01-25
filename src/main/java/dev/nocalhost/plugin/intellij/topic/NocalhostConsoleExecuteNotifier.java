package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

import dev.nocalhost.plugin.intellij.commands.data.KubeResourceType;
import dev.nocalhost.plugin.intellij.ui.console.Action;
import dev.nocalhost.plugin.intellij.ui.tree.node.ResourceNode;

public interface NocalhostConsoleExecuteNotifier {
    Topic<NocalhostConsoleExecuteNotifier> NOCALHOST_CONSOLE_EXECUTE_NOTIFIER_TOPIC =
            Topic.create("Nocalchost Console Execute", NocalhostConsoleExecuteNotifier.class);

    void action(ResourceNode workloadNode, KubeResourceType type, Action action);
}
