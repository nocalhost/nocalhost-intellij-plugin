package dev.nocalhost.plugin.intellij.startup;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

import org.jetbrains.annotations.NotNull;

import dev.nocalhost.plugin.intellij.service.NocalhostProjectService;

public final class NocalhostProjectServiceActivify implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        project.getService(NocalhostProjectService.class).refreshServiceProjectPath();
    }
}
