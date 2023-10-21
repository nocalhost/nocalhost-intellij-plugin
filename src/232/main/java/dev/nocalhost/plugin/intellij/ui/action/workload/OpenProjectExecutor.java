package dev.nocalhost.plugin.intellij.ui.action.workload;

import com.intellij.ide.RecentProjectsManagerBase;
import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.openapi.application.ApplicationManager;

import java.nio.file.Paths;

public class OpenProjectExecutor {

    public static void open(String projectPath) {
        var task = OpenProjectTask.build();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            RecentProjectsManagerBase.getInstanceEx().openProjectSync(Paths.get(projectPath), task);
        });
    }
}
