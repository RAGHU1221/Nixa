package com.nixatoolkit.ui;

import com.nixatoolkit.App;
import com.nixatoolkit.Theme;
import com.nixatoolkit.util.HistoryStore;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableModel;

public class HistoryPanel extends JPanel implements ToolPanel {
    private final App app;
    private final JLabel countLabel = new JLabel("");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"#", "File Name", "Operation", "Size", "Date & Time", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable activityTable = new JTable(tableModel);

    private static final Map<String, String[]> KIND_LABELS = new LinkedHashMap<>();
    static {
        KIND_LABELS.put("scan", new String[]{"Scan → PDF", "Scan"});
        KIND_LABELS.put("convert", new String[]{"Image Converter", "Convert"});
        KIND_LABELS.put("compress", new String[]{"Reduce File Size", "Compress"});
        KIND_LABELS.put("resize", new String[]{"Form Photo / Signature", "Resize"});
        KIND_LABELS.put("capture", new String[]{"Photo Import", "Capture"});
    }

    public static String shortLabel(String kind) {
        String[] l = KIND_LABELS.get(kind);
        return l != null ? l[1] : kind;
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

        activityTable.setRowHeight(30);
        activityTable.setBackground(Theme.SURFACE);
        activityTable.setForeground(Theme.INK);
        activityTable.setSelectionBackground(Theme.SURFACE_2);
        activityTable.setSelectionForeground(Theme.INK);
        activityTable.getTableHeader().setBackground(Theme.SURFACE_2);
        activityTable.getTableHeader().setForeground(Theme.INK);
        activityTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        activityTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        activityTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        activityTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        activityTable.getColumnModel().getColumn(4).setPreferredWidth(150);
        activityTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        activityTable.setShowGrid(false);
        activityTable.setIntercellSpacing(new Dimension(0, 1));
        activityTable.setFillsViewportHeight(true);
        activityTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activityTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane tableScroll = new JScrollPane(activityTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(tableScroll);

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
        List<HistoryStore.Entry> history = HistoryStore.loadHistory();
        java.util.Collections.reverse(history);
        tableModel.setRowCount(0);
        countLabel.setText("மொத்தம் · Total (" + history.size() + ")");

        int row = 1;
        for (HistoryStore.Entry entry : history) {
            String detail = entry.detail == null ? "" : entry.detail;
            String fileName = detail.contains(" -> ") ? detail.substring(0, detail.indexOf(" -> ")) : detail;
            String size = detail.contains("(") ? detail.substring(detail.lastIndexOf('(') + 1).replace(")", "") : "-";
            tableModel.addRow(new Object[]{row++, fileName, shortLabel(entry.kind), size, entry.ts, "Completed"});
        }
        activityTable.revalidate();
        activityTable.repaint();
    }

    @Override
    public void onShow() {
        render();
    }
}
