package com.nixatoolkit.ui;

import com.nixatoolkit.Theme;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/** A one-line status/toast strip, auto-clearing after 6s. */
public class StatusBar extends JLabel {
    private Timer timer;

    public StatusBar() {
        super(" ");
        setOpaque(true);
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 12, 8, 12));
        setFont(Theme.uiFont(12));
        clear();
    }

    public enum Kind { INFO, OK, WARN, ERR }

    public void show(String message, Kind kind) {
        java.awt.Color bg, fg;
        switch (kind) {
            case OK: bg = Theme.SUCCESS_BG; fg = Theme.SUCCESS; break;
            case WARN: bg = Theme.WARN_BG; fg = Theme.WARN; break;
            case ERR: bg = Theme.DANGER_BG; fg = Theme.DANGER; break;
            default: bg = Theme.SURFACE_2; fg = Theme.INK;
        }
        setText("  " + message);
        setBackground(bg);
        setForeground(fg);
        if (timer != null) timer.stop();
        timer = new Timer(6000, e -> clear());
        timer.setRepeats(false);
        timer.start();
    }

    private void clear() {
        setText(" ");
        setBackground(Theme.BG);
        setForeground(Theme.INK);
    }
}
