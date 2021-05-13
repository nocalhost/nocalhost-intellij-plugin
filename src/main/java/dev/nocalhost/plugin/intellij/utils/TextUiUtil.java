package dev.nocalhost.plugin.intellij.utils;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

import javax.swing.text.JTextComponent;

public final class TextUiUtil {
    private static final ActionPopupMenu CUT_COPY_PASTE_MENU;

    static {
        DefaultActionGroup group = (DefaultActionGroup) ActionManager.getInstance()
                .getAction("NocalhostEditActions");
        CUT_COPY_PASTE_MENU = ActionManager.getInstance()
                .createActionPopupMenu("popup", group);
    }

    public static void setCutCopyPastePopup(JTextComponent... components) {
        if (components == null) {
            return;
        }
        for (JTextComponent tc : components) {
            tc.setComponentPopupMenu(CUT_COPY_PASTE_MENU.getComponent());
        }
    }
}
