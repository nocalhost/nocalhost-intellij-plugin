package dev.nocalhost.plugin.intellij.topic;

import com.intellij.util.messages.Topic;

import java.util.List;

import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateQueryResult;

public interface NocalhostSyncUpdateNotifier {
    @Topic.ProjectLevel
    Topic<NocalhostSyncUpdateNotifier> NOCALHOST_SYNC_UPDATE_NOTIFIER_TOPIC =
            new Topic<>(NocalhostSyncUpdateNotifier.class);

    void action(List<NhctlDevAssociateQueryResult> result);
}
