package dev.nocalhost.plugin.intellij.startup;

import com.google.common.collect.Lists;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplication;
import dev.nocalhost.plugin.intellij.commands.data.NhctlListApplicationOptions;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeDataUpdateNotifier;
import dev.nocalhost.plugin.intellij.topic.NocalhostTreeUiUpdateNotifier;
import dev.nocalhost.plugin.intellij.utils.TokenUtil;

public class DevSpaceTreeAutoRefreshListener implements AppLifecycleListener {
    private static final Logger LOG = Logger.getInstance(DevSpaceTreeAutoRefreshListener.class);

    private static final long NOCALHOST_TREE_REFRESH_INTERVAL_MILLIS = 5000;

    private final AtomicBoolean nocalhostTreeDataUpdateMutex = new AtomicBoolean(false);

    private Thread updateThread = null;
    private boolean forceExit = false;

    @Override
    public void appStarting(@Nullable Project projectFromCommandLine) {
        AppLifecycleListener.super.appStarting(projectFromCommandLine);
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(
                NocalhostTreeDataUpdateNotifier.NOCALHOST_TREE_DATA_UPDATE_NOTIFIER_TOPIC,
                this::handleNocalhostTreeDataUpdate);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            updateThread = Thread.currentThread();
            while (!forceExit) {
                try {
                    Thread.sleep(NOCALHOST_TREE_REFRESH_INTERVAL_MILLIS);
                } catch (InterruptedException ignored) {
                }
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeDataUpdateNotifier.NOCALHOST_TREE_DATA_UPDATE_NOTIFIER_TOPIC
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

    private void handleNocalhostTreeDataUpdate() {
        @NotNull Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length <= 0) {
            return;
        }
        if (!nocalhostTreeDataUpdateMutex.compareAndSet(false, true)) {
            return;
        }
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        try {
            final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
            final NhctlCommand nhctlCommand = ServiceManager.getService(NhctlCommand.class);

            if (TokenUtil.isTokenValid()) {
                List<DevSpace> devSpaces = nocalhostApi.listDevSpaces();
                List<Application> applications = nocalhostApi.listApplications();
                List<NhctlListApplication> nhctlListApplications = Lists.newArrayList();
                for (DevSpace devSpace : devSpaces) {
                    NhctlListApplicationOptions opts = new NhctlListApplicationOptions(devSpace);
                    nhctlListApplications.addAll(nhctlCommand.listApplication(opts));
                }
                ApplicationManager.getApplication().getMessageBus().syncPublisher(
                        NocalhostTreeUiUpdateNotifier.NOCALHOST_TREE_UI_UPDATE_NOTIFIER_TOPIC
                ).action(devSpaces, applications, nhctlListApplications);
            }
        } catch (NocalhostApiException e) {
            if (nocalhostSettings.getApiErrorCount() > 5) {
                nocalhostSettings.clearAuth();
                nocalhostSettings.setApiErrorCount(0);
            } else {
                nocalhostSettings.setApiErrorCount(nocalhostSettings.getApiErrorCount() + 1);
            }
        } catch (Exception e) {
            LOG.error(e);
        } finally {
            nocalhostTreeDataUpdateMutex.set(false);
        }
    }
}
