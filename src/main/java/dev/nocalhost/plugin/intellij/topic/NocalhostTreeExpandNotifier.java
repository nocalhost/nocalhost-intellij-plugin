package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface NocalhostTreeExpandNotifier {
    @Topic.ProjectLevel
    Topic<NocalhostTreeExpandNotifier> NOCALHOST_TREE_EXPAND_NOTIFIER_TOPIC =
            new Topic<>(NocalhostTreeExpandNotifier.class);

    void action();
}
