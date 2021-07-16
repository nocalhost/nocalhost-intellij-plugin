package dev.nocalhost.plugin.intellij.startup;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.TokenResponse;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.utils.TokenUtil;

import static dev.nocalhost.plugin.intellij.exception.NocalhostNotifier.NOCALHOST_ERROR_NOTIFICATION_ID;

public class NocalhostTokenRefreshListener implements AppLifecycleListener {
    private static final Logger LOG = Logger.getInstance(NocalhostTokenRefreshListener.class);

    private static final long NOCALHOST_TOKEN_REFRESH_INTERVAL_MILLIS = 10 * 1000; // 10 seconds

    private volatile boolean stopped = false;

    @Override
    public void appStarting(@Nullable Project projectFromCommandLine) {
        AppLifecycleListener.super.appStarting(projectFromCommandLine);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            while (!stopped) {
                checkAndRefreshTokens();
                try {
                    Thread.sleep(NOCALHOST_TOKEN_REFRESH_INTERVAL_MILLIS);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    @Override
    public void appClosing() {
        stopped = true;
    }

    public void checkAndRefreshTokens() {
        NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);

        List<NocalhostAccount> nocalhostAccounts = new ArrayList<>(nocalhostSettings.getNocalhostAccounts());

        for (NocalhostAccount nocalhostAccount : nocalhostAccounts) {
            if (!TokenUtil.needRefresh(nocalhostAccount.getJwt())) {
                continue;
            }

            if (!TokenUtil.isValid(nocalhostAccount.getRefreshToken())) {
                String message = "Token of " + nocalhostAccount.getUsername() + " @ " + nocalhostAccount.getServer() + " expired.";
                NotificationGroupManager.getInstance().getNotificationGroup(NOCALHOST_ERROR_NOTIFICATION_ID)
                        .createNotification(message, NotificationType.ERROR).notify();
                continue;
            }

            try {
                TokenResponse tokenResponse = nocalhostApi.refreshToken(
                        nocalhostAccount.getServer(),
                        nocalhostAccount.getJwt(),
                        nocalhostAccount.getRefreshToken());
                UserInfo userInfo = nocalhostApi.getUserInfo(nocalhostAccount.getServer(),
                        tokenResponse.getToken());
                nocalhostSettings.updateNocalhostAccount(new NocalhostAccount(
                        nocalhostAccount.getServer(),
                        nocalhostAccount.getUsername(),
                        tokenResponse.getToken(),
                        tokenResponse.getRefreshToken(),
                        userInfo));

            } catch (Exception e) {
                LOG.error(MessageFormat.format(
                        "Error occurs while refresh token {0} on {1}",
                        nocalhostAccount.getUsername(),
                        nocalhostAccount.getServer()),
                        e);
            }
        }
    }
}
