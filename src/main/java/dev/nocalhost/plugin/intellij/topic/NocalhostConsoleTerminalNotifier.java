package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

import java.nio.file.Path;

public interface NocalhostConsoleTerminalNotifier {
    Topic<NocalhostConsoleTerminalNotifier> NOCALHOST_CONSOLE_TERMINAL_NOTIFIER_TOPIC =
            Topic.create("Nocalhost Console Terminal", NocalhostConsoleTerminalNotifier.class);

    void action(Path kubeConfigPath, String namespace, String applicationName, String deploymentName);
}
