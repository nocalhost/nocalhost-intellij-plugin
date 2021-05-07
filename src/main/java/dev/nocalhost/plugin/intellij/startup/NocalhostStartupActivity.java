package dev.nocalhost.plugin.intellij.startup;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.ServiceProjectPath;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;

import static dev.nocalhost.plugin.intellij.utils.Constants.DEFAULT_APPLICATION_NAME;

public final class NocalhostStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(NocalhostStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        devStart(project);
    }

    private void devStart(Project project) {
        final NocalhostSettings nocalhostSettings = ServiceManager
                .getService(NocalhostSettings.class);
        ServiceProjectPath serviceProjectPath = nocalhostSettings
                .getDevModeServiceByProjectPath(project.getBasePath());
        if (serviceProjectPath != null) {
            try {
                if (StringUtils.equals(serviceProjectPath.getApplicationName(),
                        DEFAULT_APPLICATION_NAME)) {
                    ProgressManager.getInstance().run(new StartingDevModeTask(project,
                            serviceProjectPath));
                } else {
                    ProgressManager.getInstance().run(new StartingDevModeTask(project,
                            serviceProjectPath));
                }
            } catch (Exception e) {
                LOG.error("error occurred while starting develop", e);
                NocalhostNotifier.getInstance(project).notifyError(
                        "Nocalhost starting dev mode error",
                        "Error occurred while starting dev mode",
                        e.getMessage());
            } finally {
                nocalhostSettings.removeDevModeServiceByProjectPath(project.getBasePath());
            }
        }
    }

}
