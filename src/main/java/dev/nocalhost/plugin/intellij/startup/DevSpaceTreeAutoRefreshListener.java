package dev.nocalhost.plugin.intellij.startup;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.Nullable;

import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUpdateNotifier;

public class DevSpaceTreeAutoRefreshListener implements AppLifecycleListener {
    private static final long NOCALHOST_TREE_UPDATE_INTERVAL_MILLIS = 10 * 1000;  // 10 seconds

    private Thread updateThread = null;
    private boolean forceExit = false;

    @Override
    public void appStarting(@Nullable Project projectFromCommandLine) {
        AppLifecycleListener.super.appStarting(projectFromCommandLine);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            updateThread = Thread.currentThread();
            while (!forceExit) {
                try {
                    Thread.sleep(NOCALHOST_TREE_UPDATE_INTERVAL_MILLIS);
                } catch (InterruptedException ignored) {
                }
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUpdateNotifier.NOCALHOST_TREE_UPDATE_NOTIFIER_TOPIC
                ).action();
            }
        });
    }

    @Override
    public void appClosing() {
        forceExit = true;
        if (updateThread != null && !updateThread.isInterrupted()) {
            updateThread.interrupt();
        }
    }
}
