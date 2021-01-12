package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface NocalhostAccountChangedNotifier {
    Topic<NocalhostAccountChangedNotifier> NOCALHOST_ACCOUNT_CHANGED_NOTIFIER_TOPIC = Topic
            .create("Nocalhost account changed", NocalhostAccountChangedNotifier.class);

    void action();
}
