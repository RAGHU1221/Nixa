package com.nixatoolkit.ui;

import com.nixatoolkit.App;
import com.nixatoolkit.Theme;
import com.nixatoolkit.util.HistoryStore;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistoryPanel extends JPanel implements ToolPanel {
    private final App app;
    private final JLabel countLabel = new JLabel("");
    private final JPanel listPanel = new JPanel();

    private static final Map<String, String[]> KIND_LABELS = new LinkedHashMap<>();
    static {
        KIND_LABELS.put("scan", new String[]{"🖨️", "Scan → PDF", "Scan"});
        KIND_LABELS.put("convert", new String[]{"🖼️", "Image Converter", "Convert"});
        KIND_LABELS.put("compress", new String[]{"📉", "Reduce File Size", "Compress"});
        KIND_LABELS.put("resize", new String[]{"📐", "Form Photo / Signature", "Resize"});
        KIND_LABELS.put("capture", new String[]{"📷", "Photo Import", "Capture"});
    }

    public static String shortLabel(String kind) {
        String[] l = KIND_LABELS.get(kind);
        return l != null ? (l[0] + " " + l[2]) : kind;
    }

    public HistoryPanel(App app) {
        this.app = app;
        setOpaque(false);
        setLayout(new BorderLayout());

        ScrollableBox top = new ScrollableBox();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("செயல்பாடுகள் · Activity History");
        header.setFont(Theme.uiFont(Font.BOLD, 20));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel sub = new JLabel("இந்த கணினியில் மட்டும் சேமிக்கப்படும் — வெளியே அனுப்பப்படாது.");
        sub.setFont(Theme.uiFont(12));
        sub.setForeground(Theme.INK_SOFT);
        sub.setBorder(BorderFactory.createEmptyBorder(2, 0, 12, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        countLabel.setFont(Theme.uiFont(Font.BOLD, 13));
        JButton clearBtn = new JButton("Clear History");
        clearBtn.setFont(Theme.uiFont(12));
        clearBtn.setFocusPainted(false);
        clearBtn.addActionListener(e -> onClear());
        topRow.add(countLabel, BorderLayout.WEST);
        topRow.add(clearBtn, BorderLayout.EAST);

        top.add(header);
        top.add(sub);
        top.add(topRow);
        top.add(Box.createVerticalStrut(10));

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);
        listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(listPanel);

        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    private void onClear() {
        int choice = JOptionPane.showConfirmDialog(this,
                "எல்லா activity history-யும் நீக்கவா? இதை மாற்ற முடியாது.",
                "Clear History / History நீக்க", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;
        HistoryStore.clearHistory();
        render();
        app.flash("✓ History clear ஆச்சு.", StatusBar.Kind.OK);
    }

    private void render() {
        listPanel.removeAll();
        List<HistoryStore.Entry> history = HistoryStore.loadHistory();
        Collections.reverse(history);
        countLabel.setText("மொத்தம் · Total (" + history.size() + ")");

        if (history.isEmpty()) {
            JLabel empty = new JLabel("<html><div style='width:600px'>இன்னும் எந்த செயல்பாடும் இல்லை. "
                    + "Scan / Convert / Compress / Resize / Capture tool-களை பயன்படுத்தினா, "
                    + "அது இங்க பட்டியலா தெரியும்.</div></html>");
            empty.setFont(Theme.uiFont(12));
            empty.setForeground(Theme.INK_SOFT);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(empty);
        } else {
            for (HistoryStore.Entry entry : history) {
                listPanel.add(historyRow(entry));
                listPanel.add(Box.createVerticalStrut(4));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel historyRow(HistoryStore.Entry entry) {
        String[] l = KIND_LABELS.get(entry.kind);
        String icon = l != null ? l[0] : "📄";
        String label = l != null ? l[1] : entry.kind;

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Theme.BG);
        row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        row.add(iconLabel, BorderLayout.WEST);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(Theme.uiFont(Font.BOLD, 12));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel detailLabel = new JLabel(entry.detail);
        detailLabel.setFont(Theme.uiFont(11));
        detailLabel.setForeground(Theme.INK_SOFT);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textBox.add(titleLabel);
        textBox.add(detailLabel);
        row.add(textBox, BorderLayout.CENTER);

        JLabel tsLabel = new JLabel(entry.ts);
        tsLabel.setFont(Theme.uiFont(10));
        tsLabel.setForeground(Theme.INK_SOFT);
        row.add(tsLabel, BorderLayout.EAST);

        return row;
    }

    @Override
    public void onShow() {
        render();
    }
}
