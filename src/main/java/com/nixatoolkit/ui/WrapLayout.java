package com.nixatoolkit.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * Drop-in replacement for {@link FlowLayout} that correctly reports the
 * preferred/minimum size of the container when its components wrap onto
 * multiple rows.
 *
 * <p>{@link FlowLayout} always reports a single-row preferred size, which is
 * fine as long as the container itself is size-authoritative (e.g. directly
 * inside a {@code BorderLayout.CENTER}). But when a {@code FlowLayout} panel
 * is nested inside a {@code BoxLayout(Y_AXIS)} (or any layout that asks for
 * preferred size before the final width is known), the wrapped rows get
 * clipped because the parent never learns the taller height that wrapping
 * actually requires.
 *
 * <p>This class fixes that by computing the preferred/minimum size based on
 * the container's current width, wrapping components exactly as
 * {@code FlowLayout} would at paint time, and reporting the resulting total
 * height. Based on the widely used public-domain implementation by Rob
 * Camick.
 */
public class WrapLayout extends FlowLayout {

    public WrapLayout() {
        super();
    }

    public WrapLayout(int align) {
        super(align);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            Container container = target;
            while (container.getSize().width == 0 && container.getParent() != null) {
                container = container.getParent();
            }
            targetWidth = container.getSize().width;
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int componentCount = target.getComponentCount();
            for (int i = 0; i < componentCount; i++) {
                Component component = target.getComponent(i);
                if (!component.isVisible()) {
                    continue;
                }
                Dimension componentSize = preferred ? component.getPreferredSize() : component.getMinimumSize();

                if (rowWidth + componentSize.width > maxWidth && rowWidth > 0) {
                    dim.width = Math.max(dim.width, rowWidth);
                    dim.height += rowHeight + vgap;
                    rowWidth = 0;
                    rowHeight = 0;
                }

                if (rowWidth != 0) {
                    rowWidth += hgap;
                }
                rowWidth += componentSize.width;
                rowHeight = Math.max(rowHeight, componentSize.height);
            }

            dim.width = Math.max(dim.width, rowWidth);
            dim.height += rowHeight;

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + (vgap * 2);

            Container scrollPane = findScrollPaneAncestor(target);
            if (scrollPane != null && target.isValid()) {
                dim.width -= (hgap + 1);
            }

            return dim;
        }
    }

    private Container findScrollPaneAncestor(Container target) {
        Container c = target;
        while (c != null) {
            if (c instanceof javax.swing.JScrollPane) {
                return c;
            }
            c = c.getParent();
        }
        return null;
    }
}
