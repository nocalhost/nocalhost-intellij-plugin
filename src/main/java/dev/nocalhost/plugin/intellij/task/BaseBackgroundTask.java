package dev.nocalhost.plugin.intellij.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.concurrency.Semaphore;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import dev.nocalhost.plugin.intellij.service.ProgressProcessManager;
import lombok.SneakyThrows;

public abstract class BaseBackgroundTask extends Task.Backgroundable {
    private final ProgressProcessManager progressProcessManager = ApplicationManager
            .getApplication().getService(ProgressProcessManager.class);

    private final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>(null);

    public BaseBackgroundTask(@Nullable Project project,
                              @NlsContexts.ProgressTitle @NotNull String title) {
        super(project, title);
    }


    public BaseBackgroundTask(@Nullable Project project,
                              @NlsContexts.ProgressTitle @NotNull String title,
                              boolean canBeCancelled) {
        super(project, title, canBeCancelled);
    }

    public BaseBackgroundTask(@Nullable Project project,
                              @NlsContexts.ProgressTitle @NotNull String title,
                              boolean canBeCancelled,
                              @Nullable PerformInBackgroundOption backgroundOption) {
        super(project, title, canBeCancelled, backgroundOption);
    }

    @Override
    public void onCancel() {
        super.onCancel();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            List<Process> processList = progressProcessManager.get(BaseBackgroundTask.this);
            if (processList == null) {
                return;
            }
            for (Process process : processList) {
                if (process.isAlive()) {
                    ApplicationManager.getApplication().executeOnPooledThread(process::destroy);
                }
            }
            progressProcessManager.del(BaseBackgroundTask.this);
        });
    }

    @Override
    public void onFinished() {
        super.onFinished();
        ApplicationManager.getApplication().executeOnPooledThread(() ->
                progressProcessManager.del(BaseBackgroundTask.this));
    }

    public abstract void runTask(@NotNull ProgressIndicator indicator);

    @SneakyThrows
    @Override
    public final void run(@NotNull ProgressIndicator indicator) {
        final Semaphore semaphore = new Semaphore();
        semaphore.down();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                runTask(indicator);
            } catch (Throwable e) {
                throwableAtomicReference.set(e);
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
                    //noinspection SynchronizeOnThis
                    synchronized (this) {
                        //noinspection SynchronizeOnThis
                        wait(500L);
                    }
                } catch (InterruptedException ignore) {
                }
            }
        });

        semaphore.waitFor();

        if (throwableAtomicReference.get() != null) {
            throw throwableAtomicReference.get();
        }
    }
}
