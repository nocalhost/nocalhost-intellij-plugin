package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface NocalhostConsoleLogNotifier {
    Topic<NocalhostConsoleLogNotifier> NOCALHOST_CONSOLE_LOG_NOTIFIER_TOPIC =
            Topic.create("Noclahost Console log", NocalhostConsoleLogNotifier.class);

    void action(String log);
}
