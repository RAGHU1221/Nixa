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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final JButton saveImageBtn = new JButton("Save Image");
    private final JButton savePdfBtn = new JButton("Save PDF");
    private final JLabel statusLine = new JLabel("Scanner status: checking...");
    private final JCheckBox bwCheck = new JCheckBox("B&W scan effect");
    private final JComboBox<String> intentMenu =
            new JComboBox<>(new String[]{"Color", "Grayscale", "Text / B&W document"});
    private final JComboBox<String> scannerMenu = new JComboBox<>();
    private final JComboBox<Integer> dpiMenu = new JComboBox<>(new Integer[]{75, 150, 200, 300, 600});

    public ScanPanel(App app) {
        this.app = app;
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        ScrollableBox top = new ScrollableBox();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("Scan to PDF");
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
        controls.add(new JLabel("DPI:"));
        dpiMenu.setSelectedItem(300);
        dpiMenu.setFont(Theme.uiFont(12));
        controls.add(dpiMenu);
        controls.add(new JLabel("Scanner:"));
        scannerMenu.addItem("Auto / Default scanner");
        scannerMenu.setFont(Theme.uiFont(12));
        scannerMenu.setPreferredSize(new Dimension(190, 28));
        controls.add(scannerMenu);
        JButton scanBtn = new JButton("Scan from Scanner");
        scanBtn.setFont(Theme.uiFont(13));
        scanBtn.addActionListener(e -> onScanClicked());
        controls.add(scanBtn);
        JButton addFileBtn = new JButton("Add Image File");
        addFileBtn.setFont(Theme.uiFont(13));
        addFileBtn.addActionListener(e -> onAddFileClicked());
        controls.add(addFileBtn);
        JButton cropBtn = new JButton("Crop Image");
        cropBtn.setFont(Theme.uiFont(13));
        cropBtn.addActionListener(e -> onCropImage());
        controls.add(cropBtn);
        top.add(controls);
        top.add(Box.createVerticalStrut(8));

        JPanel listTop = new JPanel(new BorderLayout());
        listTop.setOpaque(false);
        listTop.setAlignmentX(Component.LEFT_ALIGNMENT);
        listTop.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        countLabel.setFont(Theme.uiFont(Font.BOLD, 13));
        saveImageBtn.setFont(Theme.uiFont(13));
        saveImageBtn.setEnabled(false);
        saveImageBtn.addActionListener(e -> onSaveImage());
        savePdfBtn.setFont(Theme.uiFont(13));
        savePdfBtn.setEnabled(false);
        savePdfBtn.addActionListener(e -> onGeneratePdf());
        listTop.add(countLabel, BorderLayout.WEST);
        JPanel saveButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        saveButtons.setOpaque(false);
        saveButtons.add(saveImageBtn);
        saveButtons.add(savePdfBtn);
        listTop.add(saveButtons, BorderLayout.EAST);
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
                        statusLine.setText("Scanner connected: " + String.join(", ", names));
                    } else {
                        statusLine.setText("No scanner found (or this is not Windows) "
                                + "— 'Scan from Scanner' அழுத்தி மறுபடி முயற்சி செய்யலாம்.");
                    }
                    String selected = (String) scannerMenu.getSelectedItem();
                    scannerMenu.removeAllItems();
                    scannerMenu.addItem("Auto / Default scanner");
                    for (String name : names) scannerMenu.addItem(name);
                    if (selected != null) scannerMenu.setSelectedItem(selected);
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

    private int selectedDpi() {
        Integer dpi = (Integer) dpiMenu.getSelectedItem();
        return dpi == null ? 300 : dpi;
    }

    private String selectedScanner() {
        String scanner = (String) scannerMenu.getSelectedItem();
        return scanner == null || scanner.startsWith("Auto") ? "" : scanner;
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
                    return ScannerUtil.scan(intent, selectedDpi(), selectedScanner());
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
            saveImageBtn.setEnabled(false);
            savePdfBtn.setEnabled(false);
        } else {
            countLabel.setText("பக்கங்கள் · Pages (" + pages.size() + ")");
            saveImageBtn.setEnabled(true);
            savePdfBtn.setEnabled(true);
            for (int i = 0; i < pages.size(); i++) {
                listPanel.add(pageRow(i));
                listPanel.add(Box.createVerticalStrut(4));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
        Theme.styleButtons(listPanel);
    }

    private JPanel pageRow(int idx) {
        BufferedImage img = pages.get(idx);
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Theme.SURFACE);
        row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));

        int previewWidth = 360;
        int previewHeight = 300;
        double scale = Math.min((double) previewWidth / img.getWidth(),
            (double) previewHeight / img.getHeight());
        int scaledWidth = Math.max(1, (int) Math.round(img.getWidth() * scale));
        int scaledHeight = Math.max(1, (int) Math.round(img.getHeight() * scale));
        Image thumb = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        JLabel thumbLabel = new JLabel(new ImageIcon(thumb));
        thumbLabel.setPreferredSize(new Dimension(previewWidth, previewHeight));
        thumbLabel.setHorizontalAlignment(SwingConstants.CENTER);
        thumbLabel.setVerticalAlignment(SwingConstants.CENTER);
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
        JButton up = new JButton("Prev");
        up.setPreferredSize(new Dimension(68, 34));
        up.setFont(Theme.uiFont(12));
        up.setEnabled(idx > 0);
        up.addActionListener(e -> movePage(idx, -1));
        JButton down = new JButton("Next");
        down.setPreferredSize(new Dimension(68, 34));
        down.setFont(Theme.uiFont(12));
        down.setEnabled(idx < pages.size() - 1);
        down.addActionListener(e -> movePage(idx, 1));
        JButton preview = new JButton("Preview");
        preview.setPreferredSize(new Dimension(82, 34));
        preview.setFont(Theme.uiFont(12));
        preview.addActionListener(e -> showPreview(img, idx));
        JButton del = new JButton("Delete");
        del.setPreferredSize(new Dimension(76, 34));
        del.setFont(Theme.uiFont(12));
        del.setForeground(Theme.DANGER);
        del.addActionListener(e -> deletePage(idx));
        btnRow.add(up);
        btnRow.add(down);
        btnRow.add(preview);
        btnRow.add(del);
        row.add(btnRow, BorderLayout.EAST);

        return row;
    }

    private void showPreview(BufferedImage image, int index) {
        JLabel imageLabel = new JLabel(new ImageIcon(image));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        JScrollPane previewScroll = new JScrollPane(imageLabel);
        previewScroll.setPreferredSize(new Dimension(700, 560));
        JOptionPane.showMessageDialog(this, previewScroll, "Preview - Page " + (index + 1),
                JOptionPane.PLAIN_MESSAGE);
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

    private void onCropImage() {
        if (pages.isEmpty()) {
            app.flash("முதலில் ஒரு image அல்லது scan page சேர்க்கவும்.", StatusBar.Kind.WARN);
            return;
        }
        DragRotateCropPanel cropPanel = new DragRotateCropPanel(pages.get(pages.size() - 1));
        int result = JOptionPane.showConfirmDialog(this, cropPanel, "Drag to crop and straighten image",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        BufferedImage cropped = cropPanel.cropSelection();
        if (cropped == null) {
            app.flash("Image மீது drag செய்து crop area select செய்யவும்.", StatusBar.Kind.WARN);
            return;
        }
        pages.set(pages.size() - 1, cropped);
        renderPages();
        app.flash("Image crop and straighten completed.", StatusBar.Kind.OK);
    }

    private static final class DragRotateCropPanel extends JPanel {
        private final CropCanvas canvas;
        private final JSlider rotateSlider = new JSlider(-180, 180, 0);
        private BufferedImage transformed;

        DragRotateCropPanel(BufferedImage source) {
            setLayout(new BorderLayout(0, 8));
            transformed = ImageUtil.toRgb(source);
            canvas = new CropCanvas(transformed);
            rotateSlider.setMajorTickSpacing(90);
            rotateSlider.setMinorTickSpacing(15);
            rotateSlider.setPaintTicks(true);
            rotateSlider.setPaintLabels(true);
            rotateSlider.addChangeListener(e -> {
                transformed = rotate(source, rotateSlider.getValue());
                canvas.setImage(transformed);
            });
            add(canvas, BorderLayout.CENTER);
            add(rotateSlider, BorderLayout.SOUTH);
        }

        BufferedImage cropSelection() {
            Rectangle selection = canvas.imageSelection();
            if (selection == null) return null;
            BufferedImage result = new BufferedImage(selection.width, selection.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = result.createGraphics();
            graphics.drawImage(transformed, 0, 0, selection.width, selection.height,
                    selection.x, selection.y, selection.x + selection.width, selection.y + selection.height, null);
            graphics.dispose();
            return result;
        }

        private static BufferedImage rotate(BufferedImage source, int degrees) {
            double radians = Math.toRadians(degrees);
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));
            int width = (int) Math.floor(source.getWidth() * cos + source.getHeight() * sin);
            int height = (int) Math.floor(source.getWidth() * sin + source.getHeight() * cos);
            BufferedImage output = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, output.getWidth(), output.getHeight());
            graphics.translate((output.getWidth() - source.getWidth()) / 2.0,
                    (output.getHeight() - source.getHeight()) / 2.0);
            graphics.rotate(radians, source.getWidth() / 2.0, source.getHeight() / 2.0);
            graphics.drawImage(source, 0, 0, null);
            graphics.dispose();
            return output;
        }
    }

    private static final class CropCanvas extends JPanel {
        private BufferedImage image;
        private double scale;
        private int drawWidth;
        private int drawHeight;
        private Point dragStart;
        private Rectangle selection;

        CropCanvas(BufferedImage image) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            setPreferredSize(new Dimension(720, 480));
            setImage(image);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    dragStart = clamp(e.getPoint());
                    selection = new Rectangle(dragStart);
                    repaint();
                }
                @Override public void mouseReleased(MouseEvent e) { update(e.getPoint()); }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseDragged(MouseEvent e) { update(e.getPoint()); }
            });
        }

        void setImage(BufferedImage image) {
            this.image = image;
            scale = Math.min(1.0, Math.min(700.0 / image.getWidth(), 420.0 / image.getHeight()));
            drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
            selection = null;
            revalidate();
            repaint();
        }

        private Point clamp(Point p) {
            return new Point(Math.max(0, Math.min(drawWidth, p.x)), Math.max(0, Math.min(drawHeight, p.y)));
        }

        private void update(Point p) {
            if (dragStart == null) return;
            Point end = clamp(p);
            selection = new Rectangle(Math.min(dragStart.x, end.x), Math.min(dragStart.y, end.y),
                    Math.abs(dragStart.x - end.x), Math.abs(dragStart.y - end.y));
            repaint();
        }

        Rectangle imageSelection() {
            if (selection == null || selection.width < 2 || selection.height < 2) return null;
            int x = Math.min(image.getWidth() - 1, Math.max(0, (int) (selection.x / scale)));
            int y = Math.min(image.getHeight() - 1, Math.max(0, (int) (selection.y / scale)));
            int width = Math.min(image.getWidth() - x, Math.max(1, (int) (selection.width / scale)));
            int height = Math.min(image.getHeight() - y, Math.max(1, (int) (selection.height / scale)));
            return new Rectangle(x, y, width, height);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.drawImage(image, 0, 0, drawWidth, drawHeight, null);
            if (selection != null && selection.width > 1 && selection.height > 1) {
                g.setColor(new Color(0, 103, 192, 55));
                g.fill(selection);
                g.setColor(Theme.TEAL);
                g.setStroke(new BasicStroke(2));
                g.draw(selection);
            }
            g.dispose();
        }
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

    private void onSaveImage() {
        if (pages.isEmpty()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("JPEG image", "jpg", "jpeg"));
        chooser.setSelectedFile(new File("scan-"
                + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".jpg"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".jpg") && !out.getName().toLowerCase().endsWith(".jpeg")) {
            out = new File(out.getParentFile(), out.getName() + ".jpg");
        }
        try {
            ImageIO.write(ImageUtil.toRgb(pages.get(0)), "jpg", out);
            HistoryStore.logAction("scan", "page 1 -> " + out.getName());
            app.flash("✓ Image saved: " + out.getName(), StatusBar.Kind.OK);
        } catch (IOException e) {
            app.flash("Image save தோல்வி: " + e.getMessage(), StatusBar.Kind.ERR);
        }
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
