package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import dev.nocalhost.plugin.intellij.exception.NhctlCommandException;
import dev.nocalhost.plugin.intellij.exception.NocalhostExecuteCmdException;
import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;

public final class ErrorUtil {
    private static final Logger LOG = Logger.getInstance(ErrorUtil.class);

    public static void dealWith(Project project, String title, String message, Throwable t) {
        if (project.isDisposed()) {
            return;
        }
        if (t instanceof NhctlCommandException) {
            final NhctlCommandException e = (NhctlCommandException) t;
            ApplicationManager.getApplication().invokeLater(() ->
                    NocalhostNotifier.getInstance(project).notifyError(title, e.getErrorOutput()));
        } else if (t instanceof NocalhostExecuteCmdException) {
            ApplicationManager.getApplication().invokeLater(() ->
                    NocalhostNotifier.getInstance(project).notifyError(title, message, t.getMessage()));
        } else {
            LOG.error(title, t);
        }
    }
}
