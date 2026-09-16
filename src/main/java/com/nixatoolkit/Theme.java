package com.nixatoolkit;

import java.awt.Color;
import java.awt.Font;
import java.awt.FlowLayout;
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

    public static final Color BG = new Color(0xF3, 0xF5, 0xF7);
    public static final Color SURFACE = new Color(0xFF, 0xFF, 0xFF);
    public static final Color SURFACE_2 = new Color(0xE8, 0xED, 0xF2);
    public static final Color LINE = new Color(0xD1, 0xD9, 0xE1);
    public static final Color INK = new Color(0x1F, 0x2D, 0x3D);
    public static final Color INK_SOFT = new Color(0x5D, 0x6B, 0x78);
    public static final Color TEAL = new Color(0x00, 0x67, 0xC0);
    public static final Color TEAL_HOVER = new Color(0x00, 0x56, 0xA6);
    public static final Color MARIGOLD = new Color(0xD9, 0x8A, 0x00);
    public static final Color MARIGOLD_HOVER = new Color(0xB7, 0x70, 0x00);
    public static final Color BUTTON_ORANGE = new Color(0x00, 0x67, 0xC0);
    public static final Color BUTTON_ORANGE_LIGHT = new Color(0x00, 0x56, 0xA6);
    public static final Color SUCCESS = new Color(0x1C, 0x7D, 0x4D);
    public static final Color SUCCESS_BG = new Color(0xE0, 0xF2, 0xE6);
    public static final Color WARN = new Color(0xA3, 0x62, 0x0B);
    public static final Color WARN_BG = new Color(0xFF, 0xF4, 0xD6);
    public static final Color DANGER = new Color(0xC4, 0x2B, 0x2B);
    public static final Color DANGER_BG = new Color(0xFD, 0xE8, 0xE8);

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

    /**
     * Groups a label with the control(s) it describes (e.g. "DPI:" + a
     * combo box) into one small, non-opaque panel, so that a wrapping
     * {@link com.nixatoolkit.ui.WrapLayout} row never splits a label from
     * its field onto two different lines.
     */
    public static JPanel pair(java.awt.Component... components) {
        JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        group.setOpaque(false);
        for (java.awt.Component component : components) {
            group.add(component);
        }
        return group;
    }

    /** Applies the compact outlined/primary button language used across the app. */
    public static void styleButtons(Container root) {
        if (root instanceof JComboBox) return;
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
        if (root instanceof JComboBox) return;
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
        boolean primary = text.contains("save") || text.contains("create") || text.contains("compress")
            || text.contains("convert") || text.contains("scan") || text.contains("start")
            || text.contains("merge") || text.contains("split") || text.contains("extract")
            || text.contains("check");
        button.setFont(uiFont(Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setRolloverEnabled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(danger ? DANGER : (primary ? TEAL : LINE)),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));
        button.setBackground(danger ? DANGER_BG : (primary ? TEAL : SURFACE));
        button.setForeground(danger ? DANGER : (primary ? Color.WHITE : INK));
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
            int radius = 6;
            boolean danger = button.getText() != null
                    && (button.getText().toLowerCase().contains("delete")
                    || button.getText().toLowerCase().contains("remove")
                    || button.getText().toLowerCase().contains("clear"));
            boolean cyan = "cyan".equals(button.getClientProperty("buttonTone"));
                boolean primary = button.getText() != null && (button.getText().toLowerCase().contains("save")
                    || button.getText().toLowerCase().contains("create")
                    || button.getText().toLowerCase().contains("compress")
                    || button.getText().toLowerCase().contains("convert")
                    || button.getText().toLowerCase().contains("scan")
                    || button.getText().toLowerCase().contains("merge")
                    || button.getText().toLowerCase().contains("split")
                    || button.getText().toLowerCase().contains("extract")
                    || button.getText().toLowerCase().contains("check"));
                Color base = danger ? DANGER_BG : (cyan || primary ? BUTTON_ORANGE : SURFACE);
                Color highlight = danger ? DANGER_BG : (cyan || primary ? BUTTON_ORANGE_LIGHT : SURFACE_2);
            if (!button.isEnabled()) {
                base = new Color(0xB8, 0xB8, 0xB8);
                highlight = new Color(0xD8, 0xD8, 0xD8);
            } else if (button.getModel().isRollover()) {
                base = highlight;
            }
            g.setColor(base);
            g.fillRoundRect(0, 0, Math.max(0, width - 1), Math.max(0, height - 1), radius, radius);
            if (button.getModel().isRollover()) {
                g.setColor(highlight);
                g.fillRoundRect(1, 1, Math.max(0, width - 3), Math.max(0, height - 3), radius, radius);
            }
            g.dispose();
            super.paint(graphics, component);
        }
    }
}
