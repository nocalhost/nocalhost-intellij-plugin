package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface NocalhostI18nChangedNotifier {
    @Topic.AppLevel
    Topic<NocalhostI18nChangedNotifier> NOCALHOST_I18N_CHANGED_NOTIFIER_TOPIC =
            new Topic<>(NocalhostI18nChangedNotifier.class);

    void action();
}
