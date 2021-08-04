package dev.nocalhost.plugin.intellij.startup;

import com.intellij.ide.ApplicationInitializedListener;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;

import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;

public class DevSpaceTreeAutoRefreshListener implements ApplicationInitializedListener {
    private static final long NOCALHOST_TREE_UPDATE_INTERVAL_MILLIS = 10 * 1000;  // 10 seconds

    @Override
    public void componentsInitialized() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            while (!ApplicationManager.getApplication().isDisposed()) {
                try {
                    Thread.sleep(NOCALHOST_TREE_UPDATE_INTERVAL_MILLIS);
                } catch (InterruptedException ignored) {
                }
                Application application = ApplicationManager.getApplication();
                if (application.isDisposed()) {
                    return;
                }
                application.getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC
                ).action();
            }
        });
    }
}
