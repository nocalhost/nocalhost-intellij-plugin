package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

public interface DevSpaceTreeAutoRefreshNotifier {
    Topic<DevSpaceTreeAutoRefreshNotifier> DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC = Topic
            .create("DevSpace Tree Auto Refresh", DevSpaceTreeAutoRefreshNotifier.class);

    void action();
}
