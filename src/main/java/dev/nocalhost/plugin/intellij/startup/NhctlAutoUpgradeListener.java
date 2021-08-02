package dev.nocalhost.plugin.intellij.startup;

import com.intellij.ide.ApplicationInitializedListener;

import dev.nocalhost.plugin.intellij.service.NocalhostBinService;

public class NhctlAutoUpgradeListener implements ApplicationInitializedListener {
    @Override
    public void componentsInitialized() {
        NocalhostBinService nocalhostBinService = new NocalhostBinService();
        nocalhostBinService.checkBin();
        nocalhostBinService.checkVersion();
    }
}
