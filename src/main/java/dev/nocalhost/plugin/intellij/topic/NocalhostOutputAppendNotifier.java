package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface NocalhostOutputAppendNotifier {
    Topic<NocalhostOutputAppendNotifier> NOCALHOST_OUTPUT_APPEND_NOTIFIER =
            Topic.create("Nocalhost Console Output Append", NocalhostOutputAppendNotifier.class);

    void action(String text);
}
