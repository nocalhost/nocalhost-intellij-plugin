package dev.nocalhost.plugin.intellij.startup

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import dev.nocalhost.plugin.intellij.api.NocalhostApi
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount
import dev.nocalhost.plugin.intellij.utils.TokenUtil
import java.text.MessageFormat

class NocalhostTokenRefreshListener : ApplicationInitializedListener {
    override suspend fun execute() {
        ApplicationManager.getApplication().executeOnPooledThread(Runnable {
            while (!ApplicationManager.getApplication().isDisposed) {
                checkAndRefreshTokens()
                try {
                    Thread.sleep(NOCALHOST_TOKEN_REFRESH_INTERVAL_MILLIS)
                } catch (ignore: InterruptedException) {
                }
                val application = ApplicationManager.getApplication()
                if (application.isDisposed) {
                    return@Runnable
                }
            }
        })
    }

    fun checkAndRefreshTokens() {
        val nocalhostSettings = ApplicationManager.getApplication()
            .getService(NocalhostSettings::class.java)
        val nocalhostApi =
            ApplicationManager.getApplication().getService(NocalhostApi::class.java)

        val nocalhostAccounts: MutableList<NocalhostAccount> =
            ArrayList(nocalhostSettings.getNocalhostAccounts())

        for (nocalhostAccount in nocalhostAccounts) {
            if (!TokenUtil.needRefresh(nocalhostAccount.getJwt())) {
                continue
            }

            try {
                val tokenResponse = nocalhostApi.refreshToken(
                    nocalhostAccount.getServer(),
                    nocalhostAccount.getJwt(),
                    nocalhostAccount.getRefreshToken()
                )
                val userInfo = nocalhostApi.getUserInfo(
                    nocalhostAccount.getServer(),
                    tokenResponse.getToken()
                )
                nocalhostSettings.updateNocalhostAccount(
                    NocalhostAccount(
                        nocalhostAccount.getServer(),
                        nocalhostAccount.getUsername(),
                        tokenResponse.getToken(),
                        tokenResponse.getRefreshToken(),
                        userInfo
                    )
                )
            } catch (e: Exception) {
                LOG.error(
                    MessageFormat.format(
                        "Error occurs while refresh token {0} on {1}",
                        nocalhostAccount.getUsername(),
                        nocalhostAccount.getServer()
                    ),
                    e
                )
            }
        }
    }

    companion object {
        private val LOG = Logger.getInstance(NocalhostTokenRefreshListener::class.java)

        private const val NOCALHOST_TOKEN_REFRESH_INTERVAL_MILLIS = (10 * 1000 // 10 seconds
                ).toLong()
    }
}
