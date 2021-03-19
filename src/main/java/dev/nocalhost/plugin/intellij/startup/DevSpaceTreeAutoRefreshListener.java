package dev.nocalhost.plugin.intellij.startup;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;

import dev.nocalhost.plugin.intellij.topic.DevSpaceTreeAutoRefreshNotifier;

public class DevSpaceTreeAutoRefreshListener implements AppLifecycleListener {

    private Thread updateThread = null;
    private boolean forceExit = false;

    @Override
    public void appStarted() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            updateThread = Thread.currentThread();
            while (!forceExit) {
                ApplicationManager.getApplication().getMessageBus()
                                  .syncPublisher(DevSpaceTreeAutoRefreshNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC).action();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
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
