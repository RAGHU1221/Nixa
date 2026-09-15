package com.nixatoolkit.ui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * A vertical-BoxLayout content panel that always matches its enclosing
 * JScrollPane viewport's width, so panels only ever scroll vertically
 * (a plain JPanel inside a JScrollPane can otherwise grow wider than the
 * viewport and trigger an unwanted horizontal scrollbar).
 */
public class ScrollableBox extends JPanel implements Scrollable {
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 100;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
