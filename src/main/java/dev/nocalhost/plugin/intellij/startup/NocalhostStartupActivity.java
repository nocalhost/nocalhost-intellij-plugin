package dev.nocalhost.plugin.intellij.startup;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Paths;

import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.settings.data.DevModeService;
import dev.nocalhost.plugin.intellij.task.ExecutionTask;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;

public final class NocalhostStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(NocalhostStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        devStart(project);
    }

    private void devStart(Project project) {
        var settings = ApplicationManager.getApplication().getService(NocalhostSettings.class);
        String projectPath = Paths.get(project.getBasePath()).toString();
        DevModeService devModeService = settings.getDevModeServiceByProjectPath(projectPath);
        if (devModeService != null) {
            try {
                ProgressManager.getInstance().run(new StartingDevModeTask(
                        project,
                        devModeService,
                        settings.get(ExecutionTask.asKey(projectPath))
                ));
            } catch (Exception e) {
                LOG.error("error occurred while starting develop", e);
                NocalhostNotifier.getInstance(project).notifyError(
                        "Nocalhost starting dev mode error",
                        "Error occurred while starting dev mode",
                        e.getMessage());
            } finally {
                settings.del(ExecutionTask.asKey(projectPath));
                settings.removeDevModeServiceByProjectPath(projectPath);
            }
        }
    }
}
