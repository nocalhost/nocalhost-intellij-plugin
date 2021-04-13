package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;
import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplication;

import java.util.List;

public interface NocalhostTreeUiUpdateNotifier {
    @Topic.AppLevel
    Topic<NocalhostTreeUiUpdateNotifier> NOCALHOST_TREE_UI_UPDATE_NOTIFIER_TOPIC =
            new Topic<>(NocalhostTreeUiUpdateNotifier.class);

    void action(List<DevSpace> devSpaces,
                List<Application> applications,
                List<NhctlListApplication> nhctlListApplications);
}
