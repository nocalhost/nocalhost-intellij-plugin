package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import org.apache.commons.lang.StringUtils;

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
            final NhctlCommandException ex = (NhctlCommandException) t;
            String details = ex.getErrorOutput();
            if (StringUtils.isEmpty(details)) {
                details = ex.getMessage();
            }
            notifyNhctlError(project, title, message, details);
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
