package dev.nocalhost.plugin.intellij.startup;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import dev.nocalhost.plugin.intellij.commands.NhctlCommand;
import dev.nocalhost.plugin.intellij.commands.data.NhctlDevAssociateOptions;
import dev.nocalhost.plugin.intellij.settings.NocalhostProjectSettings;
import dev.nocalhost.plugin.intellij.settings.data.DevModeService;
import dev.nocalhost.plugin.intellij.utils.ErrorUtil;
import dev.nocalhost.plugin.intellij.utils.KubeConfigUtil;

public class MigrateNocalhostProjectSettingsActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        try {
            final NocalhostProjectSettings nocalhostProjectSettings = project.getService(NocalhostProjectSettings.class);
            final DevModeService devModeService = nocalhostProjectSettings.getDevModeService();
            if (devModeService == null) {
                return;
            }
            final Path kubeConfigPath = KubeConfigUtil.kubeConfigPath(devModeService.getRawKubeConfig());
            final NhctlCommand nhctlCommand = ApplicationManager.getApplication().getService(NhctlCommand.class);
            NhctlDevAssociateOptions opts = new NhctlDevAssociateOptions(kubeConfigPath, devModeService.getNamespace());
            opts.setDeployment(devModeService.getServiceName());
            opts.setControllerType(devModeService.getServiceType());
            opts.setContainer(devModeService.getContainerName());
            opts.setLocalSync(devModeService.getProjectPath());
            opts.setMigrate(true);
            nhctlCommand.devAssociate(devModeService.getApplicationName(), opts);
            nocalhostProjectSettings.setDevModeService(null);
        } catch (Exception e) {
            ErrorUtil.dealWith(project, "Migrate Nocalhost project settings error",
                    "Error while migrating Nocalhost project settings", e);
        }
    }
}
