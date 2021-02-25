package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface NocalhostExceptionPrintNotifier {
    Topic<NocalhostExceptionPrintNotifier> NOCALHOST_CONSOLE_EXECUTE_NOTIFIER_TOPIC =
            Topic.create("Nocalhost Exception print", NocalhostExceptionPrintNotifier.class);

    void action(String title, String content, String eMessage);
}
