package dev.nocalhost.plugin.intellij.ui;

import java.awt.*;

import javax.swing.border.LineBorder;

public class TopLineBorder extends LineBorder {
    public TopLineBorder(Color color, int thickness) {
        super(color, thickness);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.set(thickness, 0, 0, 0);
        return insets;
    }
}
