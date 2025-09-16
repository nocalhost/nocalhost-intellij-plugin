package dev.nocalhost.plugin.intellij.startup

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.ApplicationManager
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier
import kotlinx.coroutines.CoroutineScope

class DevSpaceTreeAutoRefreshListener : ApplicationInitializedListener {
    override suspend fun execute(asyncScope: CoroutineScope) {
        ApplicationManager.getApplication().executeOnPooledThread(Runnable {
            while (!ApplicationManager.getApplication().isDisposed) {
                try {
                    Thread.sleep(NOCALHOST_TREE_UPDATE_INTERVAL_MILLIS)
                } catch (ignore: InterruptedException) {
                }
                val application = ApplicationManager.getApplication()
                if (application.isDisposed) {
                    return@Runnable
                }
                application.messageBus.syncPublisher(
                    NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC
                ).action()
            }
        })
    }

    companion object {
        private const val NOCALHOST_TREE_UPDATE_INTERVAL_MILLIS = (10 * 1000 // 10 seconds
                ).toLong()
    }
}
