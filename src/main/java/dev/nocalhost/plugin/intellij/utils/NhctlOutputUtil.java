package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang.StringUtils;

import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;

public final class NhctlOutputUtil {
    private static final String MESSAGE_TAG_WARNING = "[WARNING]";
    private static final String MESSAGE_TAG_INFO = "[INFO]";
    private static final String MESSAGE_TAG_ERROR = "[ERROR]";

    public static void showMessageByCommandOutput(Project project, String line) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (StringUtils.startsWith(line, MESSAGE_TAG_WARNING)) {
                String message = line.substring(MESSAGE_TAG_WARNING.length()).trim();
                Messages.showMessageDialog(message, "", Messages.getWarningIcon());
            }
            if (StringUtils.startsWith(line, MESSAGE_TAG_INFO)) {
                String message = line.substring(MESSAGE_TAG_INFO.length()).trim();
                NocalhostNotifier.getInstance(project).notifySuccess(message, "");
            }
            if (StringUtils.startsWith(line, MESSAGE_TAG_ERROR)) {
                String message = line.substring(MESSAGE_TAG_ERROR.length()).trim();
                NocalhostNotifier.getInstance(project).notifyError(message, "");
            }
        });
    }
}
