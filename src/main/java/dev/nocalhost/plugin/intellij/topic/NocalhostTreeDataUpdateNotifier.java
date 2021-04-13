package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface NocalhostTreeDataUpdateNotifier {
    @Topic.AppLevel
    Topic<NocalhostTreeDataUpdateNotifier> NOCALHOST_TREE_DATA_UPDATE_NOTIFIER_TOPIC =
            new Topic<>(NocalhostTreeDataUpdateNotifier.class);

    void action();
}
