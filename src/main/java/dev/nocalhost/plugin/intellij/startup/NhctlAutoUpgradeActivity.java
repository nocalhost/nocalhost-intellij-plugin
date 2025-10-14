package dev.nocalhost.plugin.intellij.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.nocalhost.plugin.intellij.service.NocalhostBinService;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

public class NhctlAutoUpgradeActivity implements ProjectActivity {
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        componentsInitialized();
        return null;
    }

    public void componentsInitialized() {
        System.out.println("NhctlAutoUpgradeActivity execute");
        NocalhostBinService nocalhostBinService = new NocalhostBinService();
        nocalhostBinService.checkBin();
        nocalhostBinService.checkVersion();
    }
}
