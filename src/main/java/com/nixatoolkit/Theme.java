package com.nixatoolkit;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
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

    public static final Color BG = new Color(0xEE, 0xF3, 0xF2);
    public static final Color SURFACE = new Color(0xFF, 0xFF, 0xFF);
    public static final Color SURFACE_2 = new Color(0xE3, 0xEC, 0xE9);
    public static final Color LINE = new Color(0xD3, 0xE0, 0xDC);
    public static final Color INK = new Color(0x10, 0x1A, 0x20);
    public static final Color INK_SOFT = new Color(0x5B, 0x6B, 0x74);
    public static final Color TEAL = new Color(0x0D, 0x7D, 0x72);
    public static final Color TEAL_HOVER = new Color(0x0A, 0x62, 0x59);
    public static final Color MARIGOLD = new Color(0xE0, 0x8E, 0x2C);
    public static final Color MARIGOLD_HOVER = new Color(0xC9, 0x74, 0x1C);
    public static final Color SUCCESS = new Color(0x1C, 0x7D, 0x4D);
    public static final Color SUCCESS_BG = new Color(0xE0, 0xF2, 0xE6);
    public static final Color WARN = new Color(0xA3, 0x62, 0x0B);
    public static final Color WARN_BG = new Color(0xFB, 0xEC, 0xD2);
    public static final Color DANGER = new Color(0xB3, 0x31, 0x1F);
    public static final Color DANGER_BG = new Color(0xFB, 0xE2, 0xDE);

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
}
