package com.nixatoolkit.ui;

import javax.swing.JLabel;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * A JLabel that word-wraps to its own actual on-screen width instead of a
 * hard-coded CSS pixel width. A plain JLabel with
 * "&lt;html&gt;&lt;div style='width:820px'&gt;..." always reserves that
 * much space even when the window (or the card it sits in) is narrower,
 * which is what was pushing note/description text past its card or the
 * window edge on every page. This re-renders the html wrapper whenever the
 * label is resized, clamped to [minWidth, maxWidth], so it always fits.
 */
public class WrapLabel extends JLabel {
    private final int minWidth;
    private final int maxWidth;
    private String raw = "";

    public WrapLabel(String text) {
        this(text, Integer.MAX_VALUE);
    }

    /** maxWidth caps the wrap width on wide windows so lines stay readable. */
    public WrapLabel(String text, int maxWidth) {
        this(text, 80, maxWidth);
    }

    public WrapLabel(String text, int minWidth, int maxWidth) {
        this.minWidth = minWidth;
        this.maxWidth = maxWidth;
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                reflow();
            }
        });
        setText(text);
    }

    @Override
    public void setText(String text) {
        this.raw = text == null ? "" : text;
        reflow();
    }

    private void reflow() {
        int w = getWidth();
        if (w <= 0) w = Math.min(maxWidth, 320);
        w = Math.max(minWidth, Math.min(maxWidth, w));
        super.setText("<html><div style='width:" + w + "px'>" + raw + "</div></html>");
    }
}
