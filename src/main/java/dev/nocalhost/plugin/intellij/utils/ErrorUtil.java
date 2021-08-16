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
            int pos = e.getErrorOutput().indexOf(System.lineSeparator());
            String firstLine = e.getErrorOutput();
            String restLines = "";
            if (pos > 0) {
                firstLine = e.getErrorOutput().substring(0, pos);
                if (pos + 1 < e.getErrorOutput().length()) {
                    restLines = e.getErrorOutput().substring(pos + 1);
                }
            }
            notifyNhctlError(project, title, firstLine, restLines);
        } else if (t instanceof NocalhostExecuteCmdException) {
            notifyNhctlError(project, title, message, t.getMessage());
        } else {
            LOG.error(title, t);
        }
    }

    private static void notifyNhctlError(Project project, String title, String summary, String message) {
        ApplicationManager.getApplication().invokeLater(() ->
                NocalhostNotifier.getInstance(project).notifyError(title, summary, message));
    }
}
