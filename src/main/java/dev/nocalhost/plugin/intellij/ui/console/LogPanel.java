package dev.nocalhost.plugin.intellij.ui.console;

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.ui.SimpleToolWindowPanel;

import org.jetbrains.annotations.Nullable;

import java.awt.*;

import javax.swing.*;

public class LogPanel extends SimpleToolWindowPanel {
    public LogPanel(boolean vertical) {
        super(vertical);
    }

    @Override
    public void setToolbar(@Nullable JComponent c) {
        Component myToolbar = super.getToolbar();
        if (c == null) {
            remove(myToolbar);
        }
        myToolbar = c;
        super.setToolbar(c);
        if (myToolbar instanceof ActionToolbar) {
            ((ActionToolbar) myToolbar).setOrientation(myVertical ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL);
        }

        if (c != null) {
            if (myVertical) {
                add(c, BorderLayout.NORTH);
            } else {
                add(c, BorderLayout.EAST);
            }
        }

        revalidate();
        repaint();
    }
}
