package dev.nocalhost.plugin.intellij.startup;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.serviceContainer.AlreadyDisposedException;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import dev.nocalhost.plugin.intellij.NocalhostNotifier;
import dev.nocalhost.plugin.intellij.api.NocalhostApi;
import dev.nocalhost.plugin.intellij.api.data.DevModeService;
import dev.nocalhost.plugin.intellij.api.data.DevSpace;
import dev.nocalhost.plugin.intellij.settings.NocalhostSettings;
import dev.nocalhost.plugin.intellij.task.StartingDevModeTask;
import dev.nocalhost.plugin.intellij.topic.NocalhostOutputActivateNotifier;

public final class NocalhostStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(NocalhostStartupActivity.class);

    private Project project;

    @Override
    public void runActivity(@NotNull Project project) {
        this.project = project;

        project.getMessageBus().connect().subscribe(
                NocalhostOutputActivateNotifier.NOCALHOST_OUTPUT_ACTIVATE_NOTIFIER_TOPIC,
                this::activateOutput
        );

        devStart();
    }

    private void devStart() {
        final NocalhostSettings nocalhostSettings = ServiceManager.getService(NocalhostSettings.class);
        DevModeService devModeService = nocalhostSettings.getDevModeProjectBasePath2Service().get(project.getBasePath());
        if (nocalhostSettings.getUserInfo() != null && devModeService != null) {
            final NocalhostApi nocalhostApi = ServiceManager.getService(NocalhostApi.class);
            try {
                for (DevSpace devSpace : nocalhostApi.listDevSpace()) {
                    if (devSpace.getId() == devModeService.getApplicationId()
                            && devSpace.getDevSpaceId() == devModeService.getDevSpaceId()) {
                        ProgressManager.getInstance().run(new StartingDevModeTask(project, devSpace, devModeService));
                        break;
                    }
                }
            } catch (IOException e) {
                LOG.error("error occurred while starting develop", e);
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost starting dev mode error", "Error occurred while starting dev mode", e.getMessage());
            } finally {
                nocalhostSettings.getDevModeProjectBasePath2Service().remove(project.getBasePath());
            }
        }
    }

    private void activateOutput() {
        ApplicationManager.getApplication().invokeAndWait(() -> {
            try {
                ToolWindowManager.getInstance(project).getToolWindow("Nocalhost Console").activate(() -> {
                    ContentManager contentManager = ToolWindowManager.getInstance(project).getToolWindow("Nocalhost Console").getContentManager();
                    Content content = contentManager.getContent(0);
                    if (content != null) {
                        contentManager.setSelectedContent(content);
                    }
                });
            } catch (AlreadyDisposedException e) {
                // Ignore
            } catch (Exception e) {
                LOG.error("error occurred while activate output window", e);
                NocalhostNotifier.getInstance(project).notifyError("Nocalhost starting dev mode error", "Error occurred while starting dev mode", e.getMessage());
            }
        });
    }
}
