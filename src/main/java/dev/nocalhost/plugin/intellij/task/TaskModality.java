package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.Semaphore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.service.ProgressProcessManager;
import lombok.SneakyThrows;

public abstract class TaskModality extends Task.Modal {
    private final ProgressProcessManager ppm = ApplicationManager.getApplication().getService(ProgressProcessManager.class);
    private final AtomicReference<Throwable> throwable = new AtomicReference<>(null);

    public TaskModality(@Nullable Project project, @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
    }

    public abstract void exec(@NotNull ProgressIndicator indicator);

    @Override
    public void onCancel() {
        super.onCancel();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var queue = ppm.get(TaskModality.this);
            if (queue == null) {
                return;
            }
            for (Process process : queue) {
                if (process.isAlive()) {
                    ApplicationManager.getApplication().executeOnPooledThread(process::destroy);
                }
            }
            ppm.del(TaskModality.this);
        });
    }

    @Override
    public void onFinished() {
        super.onFinished();
        ApplicationManager
                .getApplication()
                .executeOnPooledThread(() -> ppm.del(TaskModality.this));
    }

    @SneakyThrows
    @Override
    public final void run(@NotNull ProgressIndicator indicator) {
        final Semaphore semaphore = new Semaphore();
        semaphore.down();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                exec(indicator);
            } catch (Throwable e) {
                throwable.set(e);
            } finally {
                semaphore.up();
            }
        });

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            while (true) {
                if (indicator.isCanceled() || !indicator.isRunning()) {
                    semaphore.up();
                    break;
                }
                try {
                    synchronized (this) {
                        wait(500L);
                    }
                } catch (Exception ignore) {}
            }
        });

        semaphore.waitFor();

        if (throwable.get() != null) {
            throw throwable.get();
        }
    }
}
