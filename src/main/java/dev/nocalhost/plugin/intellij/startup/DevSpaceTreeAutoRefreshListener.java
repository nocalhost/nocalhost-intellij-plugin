package dev.nocalhost.plugin.intellij.startup;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;

import org.apache.commons.collections.CollectionUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.topic.DevSpaceTreeAutoRefreshNotifier;

public class DevSpaceTreeAutoRefreshListener implements AppLifecycleListener {

    private Thread updateThread = null;
    private boolean forceExit = false;

    private final AtomicBoolean updatingDecSpaces = new AtomicBoolean(false);

    @Override
    public void appStarted() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            updateThread = Thread.currentThread();
            while (!forceExit) {

                List<DevSpace> devSpaces = updateDevSpaces();
                if (CollectionUtils.isNotEmpty(devSpaces)) {
                    ApplicationManager.getApplication().getMessageBus()
                                      .syncPublisher(DevSpaceTreeAutoRefreshNotifier.DEV_SPACE_LIST_UPDATED_NOTIFIER_TOPIC).action(devSpaces);
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    private List<DevSpace> updateDevSpaces() {
        if (!updatingDecSpaces.compareAndSet(false, true)) {
            return null;
        }
        final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
        List<DevSpace> devSpaces = null;
        try {
            devSpaces = nocalhostApi.listDevSpace();
        } catch (IOException | NocalhostApiException e) {
            return null;
        } finally {
            updatingDecSpaces.set(false);
        }
        return devSpaces;
    }

    @Override
    public void appClosing() {
        forceExit = true;
        if (updateThread != null && !updateThread.isInterrupted()) {
            updateThread.interrupt();
        }
    }
}
