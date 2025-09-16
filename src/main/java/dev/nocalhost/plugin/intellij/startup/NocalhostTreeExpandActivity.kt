package dev.nocalhost.plugin.intellij.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeExpandNotifier

class NocalhostTreeExpandActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.messageBus.syncPublisher(
            NocalhostTreeExpandNotifier.NOCALHOST_TREE_EXPAND_NOTIFIER_TOPIC
        ).action()
    }
}
