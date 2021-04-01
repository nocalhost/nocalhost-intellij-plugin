package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

import dev.nocalhost.plugin.intellij.api.data.DevSpace;

public interface NocalhostConsoleTerminalNotifier {
    Topic<NocalhostConsoleTerminalNotifier> NOCALHOST_CONSOLE_TERMINAL_NOTIFIER_TOPIC =
            Topic.create("Nocalhost Console Terminal", NocalhostConsoleTerminalNotifier.class);

    void action(DevSpace devSpace, String application, String deploymentName);
}
