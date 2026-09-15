package com.nixatoolkit;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Container;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicButtonUI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Civic teal + marigold palette (kept close to the earlier web/desktop
 * editions) plus Tamil-capable font detection.
 *
 * Note on Tamil text: unlike Tk (the earlier Python/CustomTkinter edition),
 * Java2D's text layout performs automatic font *fallback* for characters
 * missing from the requested font - so even in the worst case (no Tamil
 * font detected below) Tamil glyphs should still render via the platform's
 * own fallback chain instead of showing as boxes. Explicitly picking a
 * known-good Tamil font below is still done so the UI looks consistent,
 * not just "not broken".
 */
public final class Theme {
    private Theme() {
    }

    public static final Color BG = new Color(0x1C, 0x29, 0x38);
    public static final Color SURFACE = new Color(0x25, 0x36, 0x49);
    public static final Color SURFACE_2 = new Color(0x2D, 0x43, 0x59);
    public static final Color LINE = new Color(0x3C, 0x58, 0x70);
    public static final Color INK = new Color(0xE8, 0xF4, 0xFB);
    public static final Color INK_SOFT = new Color(0xA8, 0xBC, 0xCD);
    public static final Color TEAL = new Color(0x18, 0xB9, 0xEF);
    public static final Color TEAL_HOVER = new Color(0x48, 0xD5, 0xFF);
    public static final Color MARIGOLD = new Color(0x18, 0xB9, 0xEF);
    public static final Color MARIGOLD_HOVER = new Color(0x48, 0xD5, 0xFF);
    public static final Color BUTTON_ORANGE = new Color(0x18, 0xB9, 0xEF);
    public static final Color BUTTON_ORANGE_LIGHT = new Color(0x5A, 0xDB, 0xFF);
    public static final Color SUCCESS = new Color(0x1C, 0x7D, 0x4D);
    public static final Color SUCCESS_BG = new Color(0xE0, 0xF2, 0xE6);
    public static final Color WARN = new Color(0xA3, 0x62, 0x0B);
    public static final Color WARN_BG = new Color(0x4A, 0x3D, 0x27);
    public static final Color DANGER = new Color(0xFF, 0x76, 0x78);
    public static final Color DANGER_BG = new Color(0x52, 0x2E, 0x3A);

    private static final List<String> TAMIL_FONT_CANDIDATES = Arrays.asList(
            "Nirmala UI", "Vijaya", "Latha", "Noto Sans Tamil", "Kavivanar", "Arial Unicode MS");

    private static String tamilFontCache;

    /** First installed Tamil-capable font found on this machine (cached). */
    public static synchronized String tamilFontFamily() {
        if (tamilFontCache != null) return tamilFontCache;
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : TAMIL_FONT_CANDIDATES) {
            if (available.contains(candidate)) {
                tamilFontCache = candidate;
                return tamilFontCache;
            }
        }
        // Fall back to the logical "Dialog" font - Java2D will still
        // substitute a Tamil-capable physical font per-glyph at draw time.
        tamilFontCache = Font.DIALOG;
        return tamilFontCache;
    }

    public static Font uiFont(int style, int size) {
        return new Font(tamilFontFamily(), style, size);
    }

    public static Font uiFont(int size) {
        return uiFont(Font.PLAIN, size);
    }

    /** Applies the compact outlined/primary button language used across the app. */
    public static void styleButtons(Container root) {
        for (java.awt.Component component : root.getComponents()) {
            if (component instanceof JButton) {
                styleButton((JButton) component);
            }
            if (component instanceof Container) {
                styleButtons((Container) component);
            }
        }
    }

    public static void styleGreenButtons(Container root) {
        for (java.awt.Component component : root.getComponents()) {
            if (component instanceof JButton) {
                ((JButton) component).putClientProperty("buttonTone", "cyan");
                ((JButton) component).repaint();
            }
            if (component instanceof Container) {
                styleGreenButtons((Container) component);
            }
        }
    }

    public static void styleControls(Container root) {
        for (java.awt.Component component : root.getComponents()) {
            if (component instanceof JTextField) {
                JTextField field = (JTextField) component;
                field.setBackground(SURFACE_2);
                field.setForeground(INK);
                field.setCaretColor(TEAL_HOVER);
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(LINE), BorderFactory.createEmptyBorder(5, 8, 5, 8)));
            } else if (component instanceof JComboBox) {
                JComboBox<?> combo = (JComboBox<?>) component;
                combo.setBackground(SURFACE_2);
                combo.setForeground(INK);
                combo.setBorder(BorderFactory.createLineBorder(LINE));
            } else if (component instanceof JCheckBox) {
                JCheckBox check = (JCheckBox) component;
                check.setOpaque(false);
                check.setForeground(INK);
            } else if (component instanceof JSlider) {
                component.setBackground(SURFACE_2);
            } else if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                if (panel.isOpaque()) panel.setBackground(SURFACE);
            }
            if (component instanceof Container) styleControls((Container) component);
        }
    }

    private static void styleButton(JButton button) {
        String text = button.getText() == null ? "" : button.getText().toLowerCase();
        boolean danger = text.contains("delete") || text.contains("remove") || text.contains("clear");
        button.setFont(uiFont(Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setBackground(danger ? DANGER : BUTTON_ORANGE);
        button.setForeground(danger ? Color.WHITE : new Color(0x20, 0x18, 0x0C));
        button.setUI(new GlassButtonUI());
    }

    private static final class GlassButtonUI extends BasicButtonUI {
        @Override
        public void paint(java.awt.Graphics graphics, JComponent component) {
            AbstractButton button = (AbstractButton) component;
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                    java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int width = button.getWidth();
            int height = button.getHeight();
            int radius = Math.max(16, height - 4);
            boolean danger = button.getText() != null
                    && (button.getText().toLowerCase().contains("delete")
                    || button.getText().toLowerCase().contains("remove")
                    || button.getText().toLowerCase().contains("clear"));
            boolean cyan = "cyan".equals(button.getClientProperty("buttonTone"));
            Color base = cyan ? BUTTON_ORANGE
                    : (danger ? new Color(0xD9, 0x4A, 0x3A) : BUTTON_ORANGE);
            Color highlight = cyan ? BUTTON_ORANGE_LIGHT
                    : (danger ? new Color(0xF1, 0x76, 0x66) : BUTTON_ORANGE_LIGHT);
            if (!button.isEnabled()) {
                base = new Color(0xB8, 0xB8, 0xB8);
                highlight = new Color(0xD8, 0xD8, 0xD8);
            } else if (button.getModel().isRollover()) {
                base = highlight;
            }
            g.setColor(new Color(0, 0, 0, 45));
            g.fillRoundRect(2, 3, Math.max(0, width - 3), Math.max(0, height - 2), radius, radius);
            g.setPaint(new java.awt.GradientPaint(0, 0, highlight, 0, height, base));
            g.fillRoundRect(0, 0, Math.max(0, width - 4), Math.max(0, height - 4), radius, radius);
            g.setColor(new Color(255, 255, 255, 105));
            g.drawRoundRect(1, 1, Math.max(0, width - 6), Math.max(0, height / 2), radius, radius);
            g.dispose();
            super.paint(graphics, component);
        }
    }
}
