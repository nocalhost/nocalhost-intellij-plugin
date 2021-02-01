package dev.nocalhost.plugin.intellij.ui;

import java.awt.*;

public class VerticalFlowLayout implements LayoutManager {

    public static final int TOP = 0;

    public static final int CENTER = 1;

    public static final int BOTTOM = 2;

    private int align;

    private int hgap;

    private int vgap;

    private boolean hfill;

    public VerticalFlowLayout() {
        this(CENTER, 5, 5, false);
    }

    public VerticalFlowLayout(int align) {
        this(align, 5, 5, false);
    }

    public VerticalFlowLayout(int align, int hgap, int vgap) {
        this(align, hgap, vgap, false);
    }

    public VerticalFlowLayout(int align, int hgap, int vgap, boolean hfill) {
        this.align = align;
        this.hgap = hgap;
        this.vgap = vgap;
        this.hfill = hfill;
    }

    public int getAlignment() {
        return align;
    }

    public void setAlignment(int align) {
        this.align = align;
    }

    public int getHgap() {
        return hgap;
    }

    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    public int getVgap() {
        return vgap;
    }

    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    public boolean isHfill() {
        return hfill;
    }

    public void setHfill(boolean hfill) {
        this.hfill = hfill;
    }

    public void addLayoutComponent(String name, Component comp) {
    }

    public void removeLayoutComponent(Component comp) {
    }

    public Dimension preferredLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            int nmembers = target.getComponentCount();
            boolean firstVisibleComponent = true;
            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getPreferredSize();
                    dim.width = Math.max(dim.width, d.width);
                    if (firstVisibleComponent) {
                        firstVisibleComponent = false;
                    } else {
                        dim.height += vgap;
                    }
                    dim.height += d.height;
                }
            }
            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    public Dimension minimumLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
            int nmembers = target.getComponentCount();
            boolean firstVisibleComponent = true;
            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getMinimumSize();
                    dim.width = Math.max(dim.width, d.width);
                    if (firstVisibleComponent) {
                        firstVisibleComponent = false;
                    } else {
                        dim.height += vgap;
                    }
                    dim.height += d.height;
                }
            }
            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxwidth = target.getWidth() - (insets.left + insets.right + hgap * 2);
            int maxheight = target.getSize().height - (insets.top + insets.bottom + vgap * 2);
            int nmembers = target.getComponentCount();
            int x = insets.left + hgap, y = 0;
            int colw = 0, start = 0;
            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = m.getPreferredSize();
                    if (hfill) {
                        d.width = maxwidth;
                    }
                    m.setSize(d.width, d.height);
                    if ((y == 0) || ((y + d.height) <= maxheight)) {
                        if (y > 0) {
                            y += vgap;
                        }
                        y += d.height;
                        colw = Math.max(colw, d.width);
                    } else {
                        colw = moveComponents(target, x, insets.top + vgap, colw, maxheight - y, start, i);
                        y = d.height;
                        x += hgap + colw;
                        colw = d.width;
                        start = i;
                    }
                }
            }
            moveComponents(target, x, insets.top + vgap, colw, maxheight - y, start, nmembers);
        }
    }

    private int moveComponents(Container target, int x, int y, int width, int height, int colStart, int colEnd) {
        switch (align) {
            case TOP:
                y += 0;
                break;
            case CENTER:
                y += height / 2;
                break;
            case BOTTOM:
                y += height;
                break;
        }
        for (int i = colStart; i < colEnd; i++) {
            Component m = target.getComponent(i);
            if (m.isVisible()) {
                int cx = x + (width - m.getWidth()) / 2;
                m.setLocation(cx, y);
                y += m.getHeight() + vgap;
            }
        }
        return width;
    }

    public String toString() {
        String str = "";
        switch (align) {
            case TOP:
                str = ",align=top";
                break;
            case CENTER:
                str = ",align=center";
                break;
            case BOTTOM:
                str = ",align=bottom";
                break;
        }
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + ",hfill=" + hfill + str + "]";
    }

}