package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

public final class MessageDialogUtil {
    public static boolean okCancel(Project project, String title, String message) {
        return show(project, title, message, "OK", "Cancel") == 0;
    }

    public static boolean yesNo(Project project, String title, String message) {
        return show(project, title, message, "Yes", "No") == 0;
    }

    public static int yesNoCancel(Project project, String title, String message) {
        return show(project, title, message, "Yes", "No", "Cancel");
    }

    public static int show(Project project, String title, String message, String... options) {
        return Messages.showIdeaMessageDialog(project, message, title, options, 0, null, null);
    }
}
