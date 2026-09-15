package com.nixatoolkit.ui;

import com.nixatoolkit.App;
import com.nixatoolkit.Theme;
import com.nixatoolkit.util.HistoryStore;
import com.nixatoolkit.util.ImageUtil;
import com.nixatoolkit.util.ScannerUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResizePanel extends JPanel implements ToolPanel {
    private final App app;
    private BufferedImage srcImg;
    private File srcFile;
    private byte[] resultData;
    private int resultW, resultH;

    private static final Map<String, int[]> PRESETS = new LinkedHashMap<>();
    static {
        PRESETS.put("Passport Photo — 200x230px, <=20KB", new int[]{200, 230, 20});
        PRESETS.put("Aadhaar / e-KYC Photo — 200x230px, <=50KB", new int[]{200, 230, 50});
        PRESETS.put("PAN Card Photo — 213x213px, <=20KB", new int[]{213, 213, 20});
        PRESETS.put("Signature — 140x60px, <=20KB", new int[]{140, 60, 20});
        PRESETS.put("Exam Application Photo — 100x120px, <=30KB", new int[]{100, 120, 30});
        PRESETS.put("Custom", new int[]{200, 230, 20});
    }

    private final JComboBox<String> presetMenu = new JComboBox<>(PRESETS.keySet().toArray(new String[0]));
    private final JTextField wEntry = new JTextField(5);
    private final JTextField hEntry = new JTextField(5);
    private final JTextField kbEntry = new JTextField(5);
    private final JCheckBox borderCheck = new JCheckBox("Add border");
    private final JComboBox<String> borderColorMenu = new JComboBox<>(new String[]{"Black", "White"});
    private final JTextField borderWidthEntry = new JTextField("2", 3);
    private final JSlider verticalPositionSlider = new JSlider(0, 100, 50);
    private final JButton runBtn = new JButton("Resize + Compress");
    private final JButton cropBtn = new JButton("Crop Image");
    private final JButton scanBtn = new JButton("Scan Photo");
    private final JButton clearBtn = new JButton("Clear Image");
    private final JLabel beforeLabel = new JLabel(" ");
    private final JLabel afterLabel = new JLabel(" ");
    private final JLabel beforeImgLabel = new JLabel();
    private final JLabel afterImgLabel = new JLabel();
    private final JButton saveBtn = new JButton("Save Result");

    public ResizePanel(App app) {
        this.app = app;
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        ScrollableBox top = new ScrollableBox();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("படிவ புகைப்படம் · Form Photo & Signature");
        header.setFont(Theme.uiFont(Font.BOLD, 20));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(header);
        top.add(Box.createVerticalStrut(10));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        controls.setBackground(Theme.SURFACE);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pLbl = new JLabel("Preset:");
        pLbl.setFont(Theme.uiFont(12));
        controls.add(pLbl);
        presetMenu.setFont(Theme.uiFont(12));
        presetMenu.addActionListener(e -> applyPreset());
        controls.add(presetMenu);
        controls.add(labeled("Width(px):"));
        controls.add(wEntry);
        controls.add(labeled("Height(px):"));
        controls.add(hEntry);
        controls.add(labeled("Max KB:"));
        controls.add(kbEntry);
        borderCheck.setOpaque(false);
        borderCheck.setFont(Theme.uiFont(12));
        controls.add(borderCheck);
        borderColorMenu.setFont(Theme.uiFont(12));
        borderColorMenu.setPreferredSize(new Dimension(90, 28));
        controls.add(borderColorMenu);
        controls.add(labeled("Border px:"));
        borderWidthEntry.setPreferredSize(new Dimension(42, 26));
        controls.add(borderWidthEntry);
        controls.add(labeled("Crop position:"));
        verticalPositionSlider.setPreferredSize(new Dimension(120, 28));
        verticalPositionSlider.setToolTipText("Move crop from top to bottom to keep the head visible");
        controls.add(verticalPositionSlider);
        top.add(controls);
        applyPreset();

        WrapLabel note = new WrapLabel("இவை பொதுவாக பயன்படும் அளவுகள் — "
                + "ஒவ்வொரு போர்டல் notification-லும் exact spec மாறலாம். Upload செய்யும் site-ல் சொல்லிருக்கிற "
                + "அளவை பாத்து Custom-ல் மாற்றிக்கோங்க.", 820);
        note.setFont(Theme.uiFont(11));
        note.setForeground(Theme.WARN);
        note.setOpaque(true);
        note.setBackground(Theme.WARN_BG);
        note.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(note);
        top.add(Box.createVerticalStrut(8));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton chooseBtn = new JButton("Choose Photo");
        chooseBtn.setFont(Theme.uiFont(13));
        chooseBtn.setPreferredSize(new Dimension(140, 36));
        chooseBtn.addActionListener(e -> onChoose());
        runBtn.setFont(Theme.uiFont(13));
        runBtn.setPreferredSize(new Dimension(175, 36));
        runBtn.setEnabled(false);
        runBtn.addActionListener(e -> onRun());
        cropBtn.setFont(Theme.uiFont(13));
        cropBtn.setPreferredSize(new Dimension(125, 36));
        cropBtn.setEnabled(false);
        cropBtn.addActionListener(e -> onCrop());
        scanBtn.setFont(Theme.uiFont(13));
        scanBtn.setPreferredSize(new Dimension(120, 36));
        scanBtn.addActionListener(e -> onScan());
        clearBtn.setFont(Theme.uiFont(13));
        clearBtn.setPreferredSize(new Dimension(125, 36));
        clearBtn.setEnabled(false);
        clearBtn.addActionListener(e -> clearImage());
        btnRow.add(chooseBtn);
        btnRow.add(scanBtn);
        btnRow.add(cropBtn);
        btnRow.add(runBtn);
        btnRow.add(clearBtn);
        top.add(btnRow);
        top.add(Box.createVerticalStrut(8));

        JPanel resultPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        resultPanel.setBackground(Theme.SURFACE);
        resultPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        resultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        JPanel beforeBox = new JPanel();
        beforeBox.setOpaque(false);
        beforeBox.setLayout(new BoxLayout(beforeBox, BoxLayout.Y_AXIS));
        beforeLabel.setFont(Theme.uiFont(12));
        beforeBox.add(beforeLabel);
        beforeBox.add(beforeImgLabel);
        resultPanel.add(beforeBox);
        JPanel afterBox = new JPanel();
        afterBox.setOpaque(false);
        afterBox.setLayout(new BoxLayout(afterBox, BoxLayout.Y_AXIS));
        afterLabel.setFont(Theme.uiFont(12));
        afterBox.add(afterLabel);
        afterBox.add(afterImgLabel);
        resultPanel.add(afterBox);
        top.add(resultPanel);
        top.add(Box.createVerticalStrut(6));

        saveBtn.setFont(Theme.uiFont(13));
        saveBtn.setEnabled(false);
        saveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveBtn.addActionListener(e -> onSave());
        top.add(saveBtn);

        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    private JLabel labeled(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.uiFont(12));
        return l;
    }

    private void applyPreset() {
        int[] v = PRESETS.get(presetMenu.getSelectedItem());
        wEntry.setText(String.valueOf(v[0]));
        hEntry.setText(String.valueOf(v[1]));
        kbEntry.setText(String.valueOf(v[2]));
    }

    private void onChoose() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "bmp", "tif", "tiff"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        srcFile = chooser.getSelectedFile();
        try {
            BufferedImage img = ImageIO.read(srcFile);
            if (img == null) throw new IOException("Unsupported image");
            srcImg = ImageUtil.toRgb(img);
        } catch (Exception e) {
            app.flash("படத்தை திறக்க முடியல்: " + e.getMessage(), StatusBar.Kind.ERR);
            return;
        }
        showSourceImage(ImageUtil.humanSize(srcFile.length()));
    }

    private void showSourceImage(String sizeText) {
        runBtn.setEnabled(true);
        cropBtn.setEnabled(true);
        clearBtn.setEnabled(true);
        Image thumb = srcImg.getScaledInstance(320, -1, Image.SCALE_SMOOTH);
        beforeImgLabel.setIcon(new ImageIcon(thumb));
        beforeLabel.setText("Original — " + sizeText);
        afterImgLabel.setIcon(null);
        afterLabel.setText(" ");
        saveBtn.setEnabled(false);
        resultData = null;
    }

    private void onScan() {
        scanBtn.setEnabled(false);
        app.flash("Scanner-ல் இருந்து photo எடுக்கிறோம்...", StatusBar.Kind.INFO);
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<>() {
            ScannerUtil.ScannerException error;

            @Override
            protected BufferedImage doInBackground() {
                try {
                    return com.nixatoolkit.util.ScannerUtil.scan("color", 300, "");
                } catch (com.nixatoolkit.util.ScannerUtil.ScannerException e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                scanBtn.setEnabled(true);
                if (error != null) {
                    app.flash(error.getMessage(), StatusBar.Kind.ERR);
                    return;
                }
                try {
                    BufferedImage image = get();
                    if (image == null) {
                        app.flash("Scan cancel செய்யப்பட்டது.", StatusBar.Kind.WARN);
                        return;
                    }
                    srcFile = null;
                    srcImg = ImageUtil.toRgb(image);
                    showSourceImage(ImageUtil.humanSize(image.getWidth() * (long) image.getHeight() * 3));
                    app.flash("✓ Scan photo ready.", StatusBar.Kind.OK);
                } catch (Exception e) {
                    app.flash("Scan தோல்வி: " + e.getMessage(), StatusBar.Kind.ERR);
                }
            }
        };
        worker.execute();
    }

    private void clearImage() {
        srcFile = null;
        srcImg = null;
        resultData = null;
        beforeImgLabel.setIcon(null);
        afterImgLabel.setIcon(null);
        beforeLabel.setText(" ");
        afterLabel.setText(" ");
        runBtn.setEnabled(false);
        cropBtn.setEnabled(false);
        clearBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        app.flash("Image clear செய்யப்பட்டது.", StatusBar.Kind.OK);
    }

    private void onCrop() {
        if (srcImg == null) return;
        DragCropPanel cropPanel = new DragCropPanel(srcImg);
        int result = JOptionPane.showConfirmDialog(this, cropPanel, "Drag on the image to crop",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        Rectangle selection = cropPanel.imageSelection();
        if (selection == null || selection.width < 2 || selection.height < 2) {
            app.flash("Image மீது drag செய்து crop area select செய்யவும்.", StatusBar.Kind.WARN);
            return;
        }
        BufferedImage cropped = new BufferedImage(selection.width, selection.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = cropped.createGraphics();
        graphics.drawImage(srcImg, 0, 0, selection.width, selection.height,
                selection.x, selection.y, selection.x + selection.width, selection.y + selection.height, null);
        graphics.dispose();
        srcImg = cropped;
        showSourceImage(ImageUtil.humanSize(selection.width * (long) selection.height * 3));
        app.flash("✓ Photo crop செய்யப்பட்டது.", StatusBar.Kind.OK);
    }

    private static final class DragCropPanel extends JPanel {
        private final BufferedImage image;
        private final double scale;
        private final int drawWidth;
        private final int drawHeight;
        private Point dragStart;
        private Rectangle selection;

        DragCropPanel(BufferedImage image) {
            this.image = image;
            scale = Math.min(1.0, Math.min(720.0 / image.getWidth(), 520.0 / image.getHeight()));
            drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
            drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
            setPreferredSize(new Dimension(drawWidth, drawHeight));
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent event) {
                    dragStart = clampPoint(event.getPoint());
                    selection = new Rectangle(dragStart);
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent event) {
                    updateSelection(event.getPoint());
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent event) {
                    updateSelection(event.getPoint());
                }
            });
        }

        private Point clampPoint(Point point) {
            return new Point(Math.max(0, Math.min(drawWidth, point.x)),
                    Math.max(0, Math.min(drawHeight, point.y)));
        }

        private void updateSelection(Point point) {
            if (dragStart == null) return;
            Point end = clampPoint(point);
            selection = new Rectangle(Math.min(dragStart.x, end.x), Math.min(dragStart.y, end.y),
                    Math.abs(dragStart.x - end.x), Math.abs(dragStart.y - end.y));
            repaint();
        }

        Rectangle imageSelection() {
            if (selection == null || selection.width < 2 || selection.height < 2) return null;
            return new Rectangle(
                    (int) Math.round(selection.x / scale),
                    (int) Math.round(selection.y / scale),
                    Math.min(image.getWidth() - (int) Math.round(selection.x / scale),
                            Math.max(1, (int) Math.round(selection.width / scale))),
                    Math.min(image.getHeight() - (int) Math.round(selection.y / scale),
                            Math.max(1, (int) Math.round(selection.height / scale))));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.drawImage(image, 0, 0, drawWidth, drawHeight, null);
            if (selection != null && selection.width > 1 && selection.height > 1) {
                g.setColor(new Color(13, 125, 114, 55));
                g.fill(selection);
                g.setColor(Theme.TEAL);
                g.setStroke(new BasicStroke(2));
                g.draw(selection);
            }
            g.dispose();
        }
    }

    private void onRun() {
        if (srcImg == null) return;
        int w, h;
        double targetKb;
        try {
            w = Integer.parseInt(wEntry.getText().trim());
            h = Integer.parseInt(hEntry.getText().trim());
            targetKb = Double.parseDouble(kbEntry.getText().trim());
        } catch (NumberFormatException e) {
            app.flash("Width/Height/KB-க்கு சரியான எண்களை போடவும்.", StatusBar.Kind.ERR);
            return;
        }
        ImageUtil.CompressResult r = resizeCompressSync(w, h, targetKb);
        applyResizeResult(r, targetKb);
    }

    /** Package-visible, synchronous crop+resize+compress pass - used by onRun above and by tests. */
    ImageUtil.CompressResult resizeCompressSync(int w, int h, double targetKb) {
        BufferedImage cropped = ImageUtil.cropResizeCover(srcImg, w, h, verticalPositionSlider.getValue());
        if (borderCheck.isSelected()) {
            cropped = addBorder(cropped);
        }
        return ImageUtil.compressToTargetKb(cropped, targetKb, 0.6);
    }

    private BufferedImage addBorder(BufferedImage source) {
        int borderWidth;
        try {
            borderWidth = Integer.parseInt(borderWidthEntry.getText().trim());
        } catch (NumberFormatException e) {
            borderWidth = 2;
        }
        borderWidth = Math.max(1, Math.min(borderWidth, Math.min(source.getWidth(), source.getHeight()) / 2));
        BufferedImage bordered = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bordered.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.setColor("White".equals(borderColorMenu.getSelectedItem()) ? Color.WHITE : Color.BLACK);
        graphics.setStroke(new BasicStroke(borderWidth));
        int inset = borderWidth / 2;
        graphics.drawRect(inset, inset, source.getWidth() - borderWidth, source.getHeight() - borderWidth);
        graphics.dispose();
        return bordered;
    }

    /** Package-visible - applies a resizeCompressSync result to UI state; used by onRun and tests. */
    void applyResizeResult(ImageUtil.CompressResult r, double targetKb) {
        resultData = r.data;
        resultW = r.width;
        resultH = r.height;
        try {
            BufferedImage preview = ImageIO.read(new java.io.ByteArrayInputStream(r.data));
            afterImgLabel.setIcon(new ImageIcon(preview));
        } catch (IOException ignored) {
        }
        boolean within = r.data.length / 1024.0 <= targetKb + 1;
        String mark = within ? "✓" : "⚠";
        afterLabel.setText(mark + " " + r.width + "x" + r.height + "px — " + ImageUtil.humanSize(r.data.length));
        saveBtn.setEnabled(true);
        app.flash(within ? "✓ Ready." : "⚠ Closest possible size-க்கு தயார் ஆச்சு.",
                within ? StatusBar.Kind.OK : StatusBar.Kind.WARN);
    }

    private void onSave() {
        if (resultData == null) return;
        String base = srcFile == null ? "scanned-photo" : srcFile.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        String preset = ((String) presetMenu.getSelectedItem()).split(" ")[0].toLowerCase();

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("JPEG", "jpg"));
        chooser.setSelectedFile(new File(base + "-" + preset + ".jpg"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".jpg")) {
            out = new File(out.getParentFile(), out.getName() + ".jpg");
        }
        try {
            saveResultTo(out);
            app.flash("✓ Saved: " + out.getName(), StatusBar.Kind.OK);
        } catch (IOException e) {
            app.flash("Save தோல்வி: " + e.getMessage(), StatusBar.Kind.ERR);
        }
    }

    /** Package-visible - writes the current resize result to an explicit path; used by onSave and tests. */
    void saveResultTo(File out) throws IOException {
        Files.write(out.toPath(), resultData);
        HistoryStore.logAction("resize",
                presetMenu.getSelectedItem() + " -> " + out.getName() + " (" + resultW + "x" + resultH + "px)");
    }

    /** Package-visible - loads a photo file into state as if chosen via dialog; used by tests. */
    void loadImageForTest(File f) throws IOException {
        srcFile = f;
        BufferedImage img = ImageIO.read(f);
        if (img == null) throw new IOException("Unsupported image");
        srcImg = ImageUtil.toRgb(img);
        resultData = null;
    }

    byte[] resultDataForTest() {
        return resultData;
    }

    int resultWForTest() {
        return resultW;
    }

    int resultHForTest() {
        return resultH;
    }

    void selectPresetForTest(String key) {
        presetMenu.setSelectedItem(key);
    }

    @Override
    public void onShow() {
    }
}
