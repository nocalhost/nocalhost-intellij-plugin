package dev.nocalhost.plugin.intellij.startup;

import com.intellij.ide.ApplicationInitializedListener;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.TokenResponse;
import dev.nocalhost.plugin.intellij.api.data.UserInfo;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.utils.TokenUtil;

public class NocalhostTokenRefreshListener implements ApplicationInitializedListener {
    private static final Logger LOG = Logger.getInstance(NocalhostTokenRefreshListener.class);

    private static final long NOCALHOST_TOKEN_REFRESH_INTERVAL_MILLIS = 10 * 1000; // 10 seconds

    @Override
    public void componentsInitialized() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            while (!ApplicationManager.getApplication().isDisposed()) {
                checkAndRefreshTokens();
                try {
                    Thread.sleep(NOCALHOST_TOKEN_REFRESH_INTERVAL_MILLIS);
                } catch (InterruptedException ignore) {
                }
                Application application = ApplicationManager.getApplication();
                if (application.isDisposed()) {
                    return;
                }
            }
        });
    }

    public void checkAndRefreshTokens() {
        NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(NocalhostSettings.class);
        NocalhostApi nocalhostApi = ApplicationManager.getApplication().getService(NocalhostApi.class);

        List<NocalhostAccount> nocalhostAccounts = new ArrayList<>(nocalhostSettings.getNocalhostAccounts());

        for (NocalhostAccount nocalhostAccount : nocalhostAccounts) {
            if (!TokenUtil.needRefresh(nocalhostAccount.getJwt())) {
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
