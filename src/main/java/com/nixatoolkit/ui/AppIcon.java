package com.nixatoolkit.ui;

import com.nixatoolkit.Theme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Small hand-drawn vector glyphs, used in place of pictograph emoji
 * (printer, picture-frame, camera, etc.) for the app's tool icons.
 *
 * Windows renders those pictographs through the color "Segoe UI Emoji"
 * font, and Java2D cannot rasterize its color glyph tables - so on this
 * platform they always come out as blank/tofu boxes in a Swing app, no
 * matter which font family is requested. Drawing the icon ourselves with
 * plain Graphics2D shapes sidesteps the problem entirely.
 */
public class AppIcon implements Icon {
    public static final String SCAN = "scan";
    public static final String CONVERT = "convert";
    public static final String COMPRESS = "compress";
    public static final String RESIZE = "resize";
    public static final String CAPTURE = "capture";
    public static final String LINK = "link";
    public static final String DOC = "doc";

    private final String kind;
    private final int size;
    private final Color color;

    public AppIcon(String kind, int size) {
        this(kind, size, Theme.TEAL_HOVER);
    }

    public AppIcon(String kind, int size, Color color) {
        this.kind = kind;
        this.size = size;
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(Math.max(1.4f, size / 13f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int s = size;
        switch (kind) {
            case SCAN: drawScan(g2, s); break;
            case CONVERT: drawConvert(g2, s); break;
            case COMPRESS: drawCompress(g2, s); break;
            case RESIZE: drawResize(g2, s); break;
            case CAPTURE: drawCapture(g2, s); break;
            case LINK: drawLink(g2, s); break;
            default: drawDoc(g2, s); break;
        }
        g2.dispose();
    }

    private void drawScan(Graphics2D g2, int s) {
        g2.drawRoundRect(s / 8, s * 3 / 8, s * 3 / 4, s * 3 / 8, s / 8, s / 8);
        g2.drawRect(s / 4, s / 16, s / 2, s / 4);
        g2.drawRect(s * 5 / 16, s * 5 / 8, s * 3 / 8, s * 5 / 16);
    }

    private void drawConvert(Graphics2D g2, int s) {
        g2.drawRoundRect(s / 8, s / 6, s * 3 / 4, s * 2 / 3, s / 10, s / 10);
        int[] xs = {s * 3 / 16, s * 7 / 16, s * 13 / 16};
        int[] ys = {s * 11 / 16, s * 3 / 8, s * 11 / 16};
        g2.drawPolyline(xs, ys, 3);
        g2.fillOval(s * 9 / 16, s * 5 / 16, s / 6, s / 6);
    }

    private void drawCompress(Graphics2D g2, int s) {
        g2.drawLine(s / 6, s / 4, s * 7 / 16, s / 2);
        g2.drawLine(s * 7 / 16, s / 2, s / 6, s * 3 / 4);
        g2.drawLine(s * 5 / 6, s / 4, s * 9 / 16, s / 2);
        g2.drawLine(s * 9 / 16, s / 2, s * 5 / 6, s * 3 / 4);
    }

    private void drawResize(Graphics2D g2, int s) {
        g2.drawRect(s / 8, s / 8, s * 3 / 4, s * 3 / 4);
        g2.drawLine(s / 4, s / 2, s * 3 / 4, s / 2);
        g2.drawLine(s / 2, s / 4, s / 2, s * 3 / 4);
    }

    private void drawCapture(Graphics2D g2, int s) {
        g2.drawRoundRect(s / 8, s * 5 / 16, s * 3 / 4, s * 9 / 16, s / 12, s / 12);
        g2.drawRect(s * 3 / 8, s / 8, s / 4, s / 6);
        g2.drawOval(s * 3 / 8, s * 7 / 16, s / 4, s / 4);
    }

    private void drawLink(Graphics2D g2, int s) {
        g2.drawOval(s / 8, s * 3 / 8, s * 7 / 16, s * 7 / 16);
        g2.drawOval(s * 7 / 16, s / 8, s * 7 / 16, s * 7 / 16);
    }

    private void drawDoc(Graphics2D g2, int s) {
        g2.drawRect(s / 4, s / 8, s / 2, s * 3 / 4);
        g2.drawLine(s * 3 / 8, s * 3 / 8, s * 5 / 8, s * 3 / 8);
        g2.drawLine(s * 3 / 8, s / 2, s * 5 / 8, s / 2);
        g2.drawLine(s * 3 / 8, s * 5 / 8, s * 5 / 8, s * 5 / 8);
    }
}
