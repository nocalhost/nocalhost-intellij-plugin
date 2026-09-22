package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;

import org.jetbrains.annotations.NotNull;

/**
 * Helper for long running blocking work (nhctl subprocess invocations, ...) which has to finish
 * before the run/debug flow can continue.
 * <p>
 * Such work must never run directly on the EDT: it blocks the event queue and IntelliJ reports
 * {@code Freeze in EDT}, see the {@code nhctl dev pod} call in
 * {@code NocalhostProfileState#prepare()}. Running it through
 * {@link ProgressManager#runProcessWithProgressSynchronously} keeps a modal progress dialog open
 * while the work happens on a background thread and the event queue keeps pumping.
 */
public final class ProgressUtil {
    private ProgressUtil() {
    }

    /**
     * Runs {@code process} on a background thread while blocking the caller with a modal progress
     * dialog, so the caller (usually the EDT) is not frozen. Returns after the work finished and
     * rethrows its exception, if any.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static <T, E extends Exception> T runWithModalProgress(
            @NotNull Project project,
            @NotNull String title,
            @NotNull ThrowableComputable<T, E> process) throws E {
        return ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(process, title, false, project);
    }
}
