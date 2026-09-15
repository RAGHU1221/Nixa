package com.nixatoolkit.ui;

import com.nixatoolkit.App;
import com.nixatoolkit.Theme;
import com.nixatoolkit.util.HistoryStore;
import com.nixatoolkit.util.ImageUtil;
import com.nixatoolkit.util.PdfUtil;
import com.nixatoolkit.util.ScannerUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ScanPanel extends JPanel implements ToolPanel {
    private final App app;
    private final List<BufferedImage> pages = new ArrayList<>();
    private final JPanel listPanel = new JPanel();
    private final JLabel countLabel = new JLabel();
    private final JButton genBtn = new JButton("📄 Generate PDF");
    private final JLabel statusLine = new JLabel("Scanner status: checking...");
    private final JCheckBox bwCheck = new JCheckBox("B&W scan effect");
    private final JComboBox<String> intentMenu =
            new JComboBox<>(new String[]{"Color", "Grayscale", "Text / B&W document"});

    public ScanPanel(App app) {
        this.app = app;
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        ScrollableBox top = new ScrollableBox();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("ஸ்கேன் → PDF · Scan to PDF");
        header.setFont(Theme.uiFont(Font.BOLD, 20));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(header);

        statusLine.setFont(Theme.uiFont(12));
        statusLine.setForeground(Theme.INK_SOFT);
        statusLine.setBorder(BorderFactory.createEmptyBorder(2, 0, 10, 0));
        statusLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(statusLine);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.setBackground(Theme.SURFACE);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        bwCheck.setOpaque(false);
        bwCheck.setFont(Theme.uiFont(12));
        controls.add(bwCheck);
        intentMenu.setFont(Theme.uiFont(12));
        controls.add(intentMenu);
        JButton scanBtn = new JButton("🖨️ Scan from Scanner");
        scanBtn.setFont(Theme.uiFont(13));
        scanBtn.addActionListener(e -> onScanClicked());
        controls.add(scanBtn);
        JButton addFileBtn = new JButton("📁 Add Image File");
        addFileBtn.setFont(Theme.uiFont(13));
        addFileBtn.addActionListener(e -> onAddFileClicked());
        controls.add(addFileBtn);
        top.add(controls);
        top.add(Box.createVerticalStrut(8));

        JPanel listTop = new JPanel(new BorderLayout());
        listTop.setOpaque(false);
        listTop.setAlignmentX(Component.LEFT_ALIGNMENT);
        listTop.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        countLabel.setFont(Theme.uiFont(Font.BOLD, 13));
        genBtn.setFont(Theme.uiFont(13));
        genBtn.setEnabled(false);
        genBtn.addActionListener(e -> onGeneratePdf());
        listTop.add(countLabel, BorderLayout.WEST);
        listTop.add(genBtn, BorderLayout.EAST);
        top.add(listTop);
        top.add(Box.createVerticalStrut(6));

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

        renderPages();
    }

    @Override
    public void onShow() {
        refreshScannerStatus();
    }

    private void refreshScannerStatus() {
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                return ScannerUtil.listConnectedScanners();
            }

            @Override
            protected void done() {
                try {
                    List<String> names = get();
                    if (!names.isEmpty()) {
                        statusLine.setText("🟢 Scanner connected: " + String.join(", ", names));
                    } else {
                        statusLine.setText("⚪ எந்த Scanner-உம் இப்போ கண்டுபிடிக்கப்படல் (அல்லது இது Windows இல்ல) "
                                + "— 'Scan from Scanner' அழுத்தி மறுபடி முயற்சி செய்யலாம்.");
                    }
                } catch (Exception ignored) {
                }
            }
        };
        worker.execute();
    }

    private String currentIntent() {
        String v = (String) intentMenu.getSelectedItem();
        if (v == null) return "color";
        if (v.startsWith("Gray")) return "gray";
        if (v.startsWith("Text")) return "text";
        return "color";
    }

    private void onScanClicked() {
        app.flash("Scanner-ஐ இணைக்கிறோம்... (Windows scan dialog திறக்கும்)", StatusBar.Kind.INFO);
        String intent = currentIntent();
        boolean bw = bwCheck.isSelected();
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            ScannerUtil.ScannerException scanError;

            @Override
            protected BufferedImage doInBackground() {
                try {
                    return ScannerUtil.scan(intent);
                } catch (ScannerUtil.ScannerException e) {
                    scanError = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (scanError != null) {
                    app.flash(scanError.getMessage(), StatusBar.Kind.ERR);
                    return;
                }
                try {
                    BufferedImage img = get();
                    if (img == null) {
                        app.flash("Scan cancel செய்யப்பட்டது.", StatusBar.Kind.WARN);
                        return;
                    }
                    addPage(bw ? ImageUtil.applyBwScanEffect(img) : img);
                } catch (Exception e) {
                    app.flash("Scan தோல்வி: " + e.getMessage(), StatusBar.Kind.ERR);
                }
            }
        };
        worker.execute();
    }

    private void onAddFileClicked() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "bmp", "tif", "tiff"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        for (File f : chooser.getSelectedFiles()) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) throw new IOException("Unsupported image");
                addPage(bwCheck.isSelected() ? ImageUtil.applyBwScanEffect(img) : ImageUtil.toRgb(img));
            } catch (Exception e) {
                app.flash("'" + f.getName() + "' திறக்க முடியல்: " + e.getMessage(), StatusBar.Kind.ERR);
            }
        }
    }

    private void addPage(BufferedImage img) {
        pages.add(img);
        renderPages();
        app.flash("✓ Page " + pages.size() + " சேர்க்கப்பட்டது.", StatusBar.Kind.OK);
    }

    private void renderPages() {
        listPanel.removeAll();
        if (pages.isEmpty()) {
            JLabel empty = new JLabel("<html><div style='width:600px'>இன்னும் பக்கங்கள் இல்லை — Scanner-ல் "
                    + "இருந்து scan பண்ணவும் அல்லது image file சேர்க்கவும்.</div></html>");
            empty.setFont(Theme.uiFont(12));
            empty.setForeground(Theme.WARN);
            empty.setOpaque(true);
            empty.setBackground(Theme.WARN_BG);
            empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            listPanel.add(empty);
            countLabel.setText("பக்கங்கள் · Pages (0)");
            genBtn.setEnabled(false);
        } else {
            countLabel.setText("பக்கங்கள் · Pages (" + pages.size() + ")");
            genBtn.setEnabled(true);
            for (int i = 0; i < pages.size(); i++) {
                listPanel.add(pageRow(i));
                listPanel.add(Box.createVerticalStrut(4));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel pageRow(int idx) {
        BufferedImage img = pages.get(idx);
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Theme.SURFACE);
        row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));

        Image thumb = img.getScaledInstance(70, -1, Image.SCALE_SMOOTH);
        JLabel thumbLabel = new JLabel(new ImageIcon(thumb));
        row.add(thumbLabel, BorderLayout.WEST);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Page " + (idx + 1));
        title.setFont(Theme.uiFont(Font.BOLD, 12));
        JLabel dims = new JLabel(img.getWidth() + " x " + img.getHeight() + " px");
        dims.setFont(Theme.uiFont(11));
        dims.setForeground(Theme.INK_SOFT);
        textBox.add(title);
        textBox.add(dims);
        row.add(textBox, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnRow.setOpaque(false);
        JButton up = new JButton("↑");
        up.addActionListener(e -> movePage(idx, -1));
        JButton down = new JButton("↓");
        down.addActionListener(e -> movePage(idx, 1));
        JButton del = new JButton("✕");
        del.setForeground(Theme.DANGER);
        del.addActionListener(e -> deletePage(idx));
        btnRow.add(up);
        btnRow.add(down);
        btnRow.add(del);
        row.add(btnRow, BorderLayout.EAST);

        return row;
    }

    private void movePage(int idx, int dir) {
        int newIdx = idx + dir;
        if (newIdx < 0 || newIdx >= pages.size()) return;
        BufferedImage tmp = pages.get(idx);
        pages.set(idx, pages.get(newIdx));
        pages.set(newIdx, tmp);
        renderPages();
    }

    private void deletePage(int idx) {
        pages.remove(idx);
        renderPages();
    }

    private void onGeneratePdf() {
        if (pages.isEmpty()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
        chooser.setSelectedFile(new File("scan-"
                + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".pdf")) {
            out = new File(out.getParentFile(), out.getName() + ".pdf");
        }
        generatePdfToFile(out);
    }

    /** Package-visible so tests can exercise the save logic without a real file dialog. */
    void generatePdfToFile(File out) {
        try {
            List<byte[]> jpegs = new ArrayList<>();
            List<int[]> sizes = new ArrayList<>();
            for (BufferedImage img : pages) {
                jpegs.add(ImageUtil.encodeJpeg(img, 0.85f));
                sizes.add(new int[]{img.getWidth(), img.getHeight()});
            }
            byte[] pdf = PdfUtil.buildFromJpegPages(jpegs, sizes, 200.0);
            Files.write(out.toPath(), pdf);
            HistoryStore.logAction("scan", pages.size() + " page(s) -> " + out.getName());
            app.flash("✓ PDF saved: " + out.getName(), StatusBar.Kind.OK);
        } catch (IOException e) {
            app.flash("PDF save தோல்வி: " + e.getMessage(), StatusBar.Kind.ERR);
        }
    }

    List<BufferedImage> pagesForTest() {
        return pages;
    }

    void addPageForTest(BufferedImage img) {
        addPage(img);
    }
}
