package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;

public final class MessageUtil {
    public static void showMessageDialog(String message) {
        if (SystemInfo.isMac) {
            Messages.showMessageDialog("", message, null);
        } else {
            Messages.showMessageDialog(message, "", null);
        }
    }
}
