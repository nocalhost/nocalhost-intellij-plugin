package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

import dev.nocalhost.plugin.intellij.settings.data.DevModeService;

public interface NocalhostTreeExpandNotifier {
    @Topic.ProjectLevel
    Topic<NocalhostTreeExpandNotifier> NOCALHOST_TREE_EXPAND_NOTIFIER_TOPIC =
            new Topic<>(NocalhostTreeExpandNotifier.class);

    void action();
}
