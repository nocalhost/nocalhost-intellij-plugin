package dev.nocalhost.plugin.intellij.startup;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.Application;
import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.exception.NocalhostApiException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;

public final class NocalhostStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(NocalhostStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        devStart(project);
    }

    private void devStart(Project project) {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        DevModeService devModeService = nocalhostSettings.getDevModeProjectBasePath2Service().get(project.getBasePath());
        if (nocalhostSettings.getUserInfo() != null && devModeService != null) {
            final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
            try {
                for (DevSpace devSpace : nocalhostApi.listDevSpaces()) {
                    for (Application app : nocalhostApi.listApplications()) {
                        if (app.getId() == devModeService.getApplicationId()
                                && devSpace.getId() == devModeService.getDevSpaceId()) {
                            ProgressManager.getInstance().run(new StartingDevModeTask(project, devSpace, app, devModeService));
                            break;
                        }
                    }
                }
            } catch (IOException | NocalhostApiException e) {
                LOG.error("error occurred while starting develop", e);
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost starting dev mode error", "Error occurred while starting dev mode", e.getMessage());
            } finally {
                nocalhostSettings.getDevModeProjectBasePath2Service().remove(project.getBasePath());
            }
        }
    }

}
