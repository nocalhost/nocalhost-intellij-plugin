package dev.nocalhost.plugin.intellij.startup;

import com.intellij.ide.ApplicationInitializedListener;
import com.intellij.openapi.application.ApplicationManager;

import dev.nocalhost.plugin.intellij.config.NocalhostConfig;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.NocalhostAccount;
import dev.nocalhost.plugin.intellij.settings.data.StandaloneCluster;

public class ConfigMigrationListener implements ApplicationInitializedListener {
    @Override
    public void componentsInitialized() {
        final NocalhostSettings nocalhostSettings = ApplicationManager.getApplication().getService(NocalhostSettings.class);
        final NocalhostConfig nocalhostConfig = ApplicationManager.getApplication().getService(NocalhostConfig.class);

        for (StandaloneCluster sc : nocalhostSettings.getStandaloneClusters()) {
            nocalhostConfig.updateStandaloneCluster(sc);
        }
        nocalhostSettings.cleanStandaloneClusters();

        for (NocalhostAccount na : nocalhostSettings.getNocalhostAccounts()) {
            nocalhostConfig.updateNocalhostAccount(na);
        }
        nocalhostSettings.cleanNocalhostAccounts();
    }
}
