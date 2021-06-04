package dev.nocalhost.plugin.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManager;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SyncStatusWidget implements StatusBarWidget {

    private final StatusBar statusBar;
    private final Project project;
    private Thread updateThread = null;
    private boolean forceExit = false;


    public SyncStatusWidget(Project project) {
        this.statusBar = WindowManager.getInstance().getStatusBar(project);
        this.project = project;
    }

    @Override
    public @NonNls
    @NotNull String ID() {
        return "Nocalhost Sync Status";
    }

    @Override
    public @Nullable WidgetPresentation getPresentation() {
        return new SyncStatusPresentation(statusBar, project, this);
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            updateThread = Thread.currentThread();
            while (!forceExit) {
                statusBar.updateWidget("Nocalhost Sync Status");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    @Override
    public void dispose() {
        forceExit = true;
        if (updateThread != null && !updateThread.isInterrupted()) {
            updateThread.interrupt();
        }
    }
}
