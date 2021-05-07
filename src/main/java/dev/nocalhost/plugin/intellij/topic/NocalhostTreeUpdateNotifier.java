package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface NocalhostTreeUpdateNotifier {
    @Topic.AppLevel
    Topic<NocalhostTreeUpdateNotifier> NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC =
            new Topic<>(NocalhostTreeUpdateNotifier.class);

    void action();
}
