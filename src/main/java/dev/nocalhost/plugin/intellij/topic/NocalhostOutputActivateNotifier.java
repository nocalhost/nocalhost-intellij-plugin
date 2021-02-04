package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface NocalhostOutputActivateNotifier {
    Topic<NocalhostOutputActivateNotifier> NOCALHOST_OUTPUT_ACTIVATE_NOTIFIER_TOPIC =
            Topic.create("Nocalhost Console Output Activate", NocalhostOutputActivateNotifier.class);

    void action();
}
