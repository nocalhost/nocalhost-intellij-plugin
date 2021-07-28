package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import dev.nocalhost.plugin.intellij.exception.NocalhostNotifier;

public final class InstallApplicationUtil {
    private static final String MESSAGE_TAG_WARNING = "[WARNING]";
    private static final String MESSAGE_TAG_INFO = "[INFO]";
    private static final String MESSAGE_TAG_ERROR = "[ERROR]";

    public static void showMessageByCommandOutput(Project project, String output) {
        try (BufferedReader br = new BufferedReader(new StringReader(output))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (StringUtils.startsWith(line, MESSAGE_TAG_WARNING)) {
                    String message = line.substring(MESSAGE_TAG_WARNING.length()).trim();
                    Messages.showMessageDialog(message, "Install Application", Messages.getWarningIcon());
                }
                if (StringUtils.startsWith(line, MESSAGE_TAG_INFO)) {
                    String message = line.substring(MESSAGE_TAG_INFO.length()).trim();
                    NocalhostNotifier.getInstance(project).notifySuccess("Install application info", message);
                }
                if (StringUtils.startsWith(line, MESSAGE_TAG_ERROR)) {
                    String message = line.substring(MESSAGE_TAG_ERROR.length()).trim();
                    NocalhostNotifier.getInstance(project).notifyError("Install application error", message);
                }
            }
        } catch (IOException ignored) {
        }
    }
}
