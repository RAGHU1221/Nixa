package com.nixatoolkit.ui;

import com.nixatoolkit.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** A clickable card on the Home dashboard. */
public class ToolCard extends JPanel {
    public ToolCard(String iconKind, String titleTa, String titleEn, String desc, Runnable onClick) {
        setLayout(new BorderLayout());
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(new AppIcon(iconKind, 28));
        iconLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new javax.swing.BoxLayout(textBox, javax.swing.BoxLayout.Y_AXIS));
        textBox.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JLabel sub = new JLabel(titleEn.toUpperCase());
        sub.setFont(Theme.uiFont(Font.BOLD, 11));
        sub.setForeground(Theme.TEAL_HOVER);
        sub.setAlignmentX(0f);

        JLabel title = new JLabel(titleTa);
        title.setFont(Theme.uiFont(Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(4, 0, 6, 0));
        title.setAlignmentX(0f);

        WrapLabel body = new WrapLabel(desc, 220);
        body.setFont(Theme.uiFont(12));
        body.setForeground(Theme.INK_SOFT);
        body.setAlignmentX(0f);

        textBox.add(sub);
        textBox.add(title);
        textBox.add(body);

        add(iconLabel, BorderLayout.NORTH);
        add(textBox, BorderLayout.CENTER);

        MouseAdapter clickHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        };
        addMouseListener(clickHandler);
        iconLabel.addMouseListener(clickHandler);
        textBox.addMouseListener(clickHandler);
        sub.addMouseListener(clickHandler);
        title.addMouseListener(clickHandler);
        body.addMouseListener(clickHandler);
    }
}
