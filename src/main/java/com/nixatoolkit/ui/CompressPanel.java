package com.nixatoolkit.ui;

import com.nixatoolkit.App;
import com.nixatoolkit.Theme;
import com.nixatoolkit.util.HistoryStore;
import com.nixatoolkit.util.ImageUtil;
import com.nixatoolkit.util.PdfUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CompressPanel extends JPanel implements ToolPanel {
    private final App app;
    private BufferedImage srcImg;
    private Path srcPath;
    private boolean isPdf;

    private byte[] resultData;
    private int resultW, resultH;
    private int resultPages;
    private boolean resultOverLimit;

    private final JTextField targetEntry = new JTextField("100", 6);
        private final JComboBox<String> qualityMenu = new JComboBox<>(
            new String[]{"Best quality", "Balanced", "Smallest file"});
    private final JButton compressBtn = new JButton("Compress");
    private final JLabel pdfNote = new JLabel();
    private final JLabel beforeLabel = new JLabel(" ");
    private final JLabel afterLabel = new JLabel(" ");
    private final JLabel beforeImgLabel = new JLabel();
    private final JLabel afterImgLabel = new JLabel();
    private final JButton saveBtn = new JButton("Save Result");

    public CompressPanel(App app) {
        this.app = app;
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        ScrollableBox top = new ScrollableBox();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("அளவு குறை · Reduce File Size");
        header.setFont(Theme.uiFont(Font.BOLD, 20));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(header);
        top.add(Box.createVerticalStrut(10));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.setBackground(Theme.SURFACE);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tLbl = new JLabel("Target size (KB):");
        tLbl.setFont(Theme.uiFont(12));
        controls.add(tLbl);
        controls.add(targetEntry);
        controls.add(new JLabel("Quality:"));
        qualityMenu.setFont(Theme.uiFont(12));
        qualityMenu.setPreferredSize(new Dimension(135, 30));
        controls.add(qualityMenu);
        JButton chooseBtn = new JButton("Choose Photo or PDF");
        chooseBtn.setFont(Theme.uiFont(13));
        chooseBtn.addActionListener(e -> onChoose());
        controls.add(chooseBtn);
        compressBtn.setFont(Theme.uiFont(13));
        compressBtn.setEnabled(false);
        compressBtn.addActionListener(e -> onCompress());
        controls.add(compressBtn);
        saveBtn.setFont(Theme.uiFont(13));
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(e -> onSave());
        controls.add(saveBtn);
        JButton clearBtn = new JButton("Clear");
        clearBtn.setFont(Theme.uiFont(13));
        clearBtn.addActionListener(e -> clearSelection());
        controls.add(clearBtn);
        Theme.styleGreenButtons(controls);
        top.add(controls);

        pdfNote.setText("<html><div style='width:820px'>PDF-ஐ compress பண்ணும்போது, நம்ம tool இந்த PDF-ல் "
                + "இருக்கிற scan செய்த பட பக்கங்களை (JPEG images) கண்டுபிடித்து மறுபடி compress செய்யும் — "
                + "scan செய்த/photo PDF-க்கு இது நல்லா வேலை செய்யும். Typed-text (born-digital) PDF-ல் "
                + "பட பக்கங்கள் இல்லாததால் இது வேலை செய்யாது.</div></html>");
        pdfNote.setFont(Theme.uiFont(11));
        pdfNote.setForeground(Theme.WARN);
        pdfNote.setOpaque(true);
        pdfNote.setBackground(Theme.WARN_BG);
        pdfNote.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pdfNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        pdfNote.setVisible(false);
        top.add(pdfNote);
        top.add(Box.createVerticalStrut(8));

        JPanel resultPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        resultPanel.setBackground(Theme.SURFACE);
        resultPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        resultPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));

        JPanel beforeBox = new JPanel();
        beforeBox.setOpaque(false);
        beforeBox.setLayout(new BoxLayout(beforeBox, BoxLayout.Y_AXIS));
        beforeLabel.setFont(Theme.uiFont(12));
        beforeImgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
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

        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    private void clearSelection() {
        srcImg = null;
        srcPath = null;
        isPdf = false;
        resultData = null;
        resultW = 0;
        resultH = 0;
        resultPages = 0;
        resultOverLimit = false;
        pdfNote.setVisible(false);
        beforeLabel.setText(" ");
        afterLabel.setText(" ");
        beforeImgLabel.setIcon(null);
        afterImgLabel.setIcon(null);
        compressBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        app.flash("Selected file clear செய்யப்பட்டது.", StatusBar.Kind.OK);
    }

    private void onChoose() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Photos and PDFs", "jpg", "jpeg", "png", "bmp", "tif", "tiff", "pdf"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        srcPath = f.toPath();
        isPdf = f.getName().toLowerCase().endsWith(".pdf");
        resultData = null;
        saveBtn.setEnabled(false);
        afterImgLabel.setIcon(null);
        afterLabel.setText(" ");

        if (isPdf) {
            pdfNote.setVisible(true);
            srcImg = null;
            try {
                List<byte[]> jpegs = PdfUtil.extractJpegPages(srcPath);
                if (jpegs.isEmpty()) {
                    app.flash("இந்த PDF-ல் scan செய்த பட பக்கங்கள் எதுவும் கிடைக்கல் — typed-text PDF-ஆ இருக்கலாம்.",
                            StatusBar.Kind.ERR);
                    compressBtn.setEnabled(false);
                    beforeImgLabel.setIcon(null);
                } else {
                    BufferedImage thumb = PdfUtil.decodeJpeg(jpegs.get(0));
                    beforeImgLabel.setIcon(new ImageIcon(scaled(thumb, 280)));
                    compressBtn.setEnabled(true);
                }
            } catch (IOException e) {
                app.flash("PDF preview காட்ட முடியல்: " + e.getMessage(), StatusBar.Kind.ERR);
                compressBtn.setEnabled(false);
            }
            beforeLabel.setText("Original PDF — " + ImageUtil.humanSize(f.length()));
            return;
        }

        pdfNote.setVisible(false);
        try {
            BufferedImage img = ImageIO.read(f);
            if (img == null) throw new IOException("Unsupported image");
            srcImg = ImageUtil.toRgb(img);
            compressBtn.setEnabled(true);
            beforeImgLabel.setIcon(new ImageIcon(scaled(srcImg, 280)));
            beforeLabel.setText("Original — " + ImageUtil.humanSize(f.length()));
        } catch (Exception e) {
            app.flash("படத்தை திறக்க முடியல்: " + e.getMessage(), StatusBar.Kind.ERR);
        }
    }

    private static Image scaled(BufferedImage img, int maxDim) {
        int w = img.getWidth(), h = img.getHeight();
        double scale = Math.min(1.0, maxDim / (double) Math.max(w, h));
        int nw = Math.max(1, (int) (w * scale));
        int nh = Math.max(1, (int) (h * scale));
        return img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
    }

    private void onCompress() {
        double targetKb;
        try {
            targetKb = Double.parseDouble(targetEntry.getText().trim());
        } catch (NumberFormatException e) {
            app.flash("Target KB-க்கு சரியான எண்ணை போடவும்.", StatusBar.Kind.ERR);
            return;
        }

        if (isPdf) {
            if (srcPath == null) return;
            compressBtn.setEnabled(false);
            compressBtn.setText("⚙ Compressing...");
            app.flash("PDF compress ஆகிறது... பக்கங்கள் அதிகமா இருந்தா கொஞ்சம் நேரம் ஆகும்.", StatusBar.Kind.INFO);
            runPdfCompress(targetKb);
            return;
        }

        if (srcImg == null) return;
        ImageUtil.CompressResult r = compressImageSync(targetKb);
        applyImageCompressResult(r);
    }

    /** Package-visible, synchronous image compression - used by onCompress above and by tests. */
    ImageUtil.CompressResult compressImageSync(double targetKb) {
        return ImageUtil.compressToTargetKb(srcImg, targetKb, selectedMinScale());
    }

    private double selectedMinScale() {
        String mode = (String) qualityMenu.getSelectedItem();
        if ("Smallest file".equals(mode)) return 0.35;
        if ("Balanced".equals(mode)) return 0.55;
        return 0.85;
    }

    /** Package-visible - applies an image compress result to UI state; used by onCompress and tests. */
    void applyImageCompressResult(ImageUtil.CompressResult r) {
        resultData = r.data;
        resultW = r.width;
        resultH = r.height;
        resultOverLimit = r.overLimit;
        try {
            BufferedImage preview = ImageIO.read(new java.io.ByteArrayInputStream(r.data));
            afterImgLabel.setIcon(new ImageIcon(scaled(preview, 280)));
        } catch (IOException ignored) {
        }
        afterLabel.setText("Compressed — " + ImageUtil.humanSize(r.data.length) + " (" + r.width + "x" + r.height + "px)");
        saveBtn.setEnabled(true);
        if (r.overLimit) {
            app.flash("⚠ Target KB மிகவும் குறைவு — கிடைக்கிற அளவுக்கு குறைச்சிருக்கோம்.", StatusBar.Kind.WARN);
        } else {
            app.flash("✓ Target size-க்குள் compress ஆச்சு.", StatusBar.Kind.OK);
        }
    }

    private void runPdfCompress(double targetKb) {
        SwingWorker<Object[], Void> worker = new SwingWorker<>() {
            @Override
            protected Object[] doInBackground() throws Exception {
                return compressPdfSync(srcPath, targetKb);
            }

            @Override
            protected void done() {
                compressBtn.setEnabled(true);
                compressBtn.setText("⚙ Compress");
                try {
                    Object[] r = get();
                    applyPdfCompressResult(r);
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    app.flash("PDF compress தோல்வி: " + cause.getMessage(), StatusBar.Kind.ERR);
                }
            }
        };
        worker.execute();
    }

    /** Package-visible, synchronous PDF compression pass - used by the SwingWorker above and by tests. */
    Object[] compressPdfSync(Path path, double targetKb) throws Exception {
        List<byte[]> jpegs = PdfUtil.extractJpegPages(path);
        if (jpegs.isEmpty()) {
            throw new PdfUtil.PdfToolException("இந்த PDF-ல் scan செய்த பட பக்கங்கள் இல்ல.");
        }
        double perPageKb = Math.max(8.0, targetKb / jpegs.size());
        List<byte[]> outJpegs = new ArrayList<>();
        List<int[]> sizes = new ArrayList<>();
        BufferedImage firstPreview = null;
        for (byte[] jpeg : jpegs) {
            BufferedImage page = PdfUtil.decodeJpeg(jpeg);
            ImageUtil.CompressResult r = ImageUtil.compressToTargetKb(page, perPageKb, selectedMinScale());
            outJpegs.add(r.data);
            sizes.add(new int[]{r.width, r.height});
            if (firstPreview == null) {
                firstPreview = ImageIO.read(new java.io.ByteArrayInputStream(r.data));
            }
        }
        byte[] pdf = PdfUtil.buildFromJpegPages(outJpegs, sizes, 150.0);
        boolean overLimit = (pdf.length / 1024.0) > targetKb;
        return new Object[]{pdf, jpegs.size(), overLimit, firstPreview};
    }

    /** Package-visible - applies a compressPdfSync result to UI state; used by done() above and tests. */
    void applyPdfCompressResult(Object[] r) {
        resultData = (byte[]) r[0];
        resultPages = (int) r[1];
        resultOverLimit = (boolean) r[2];
        BufferedImage preview = (BufferedImage) r[3];
        if (preview != null) {
            afterImgLabel.setIcon(new ImageIcon(scaled(preview, 280)));
        }
        String pageWord = resultPages == 1 ? "page" : "pages";
        afterLabel.setText("Compressed — " + ImageUtil.humanSize(resultData.length)
                + " (" + resultPages + " " + pageWord + ")");
        saveBtn.setEnabled(true);
        if (resultOverLimit) {
            app.flash("⚠ Target KB மிகவும் குறைவு — கிடைக்கிற அளவுக்கு குறைச்சிருக்கோம்.", StatusBar.Kind.WARN);
        } else {
            app.flash("✓ Target size-க்குள் PDF compress ஆச்சு.", StatusBar.Kind.OK);
        }
    }

    private void onSave() {
        if (resultData == null) return;
        String base = srcPath.getFileName().toString();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);

        JFileChooser chooser = new JFileChooser();
        if (isPdf) {
            chooser.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
            chooser.setSelectedFile(new File(base + "-compressed.pdf"));
        } else {
            chooser.setFileFilter(new FileNameExtensionFilter("JPEG", "jpg"));
            chooser.setSelectedFile(new File(base + "-compressed.jpg"));
        }
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        String wantExt = isPdf ? ".pdf" : ".jpg";
        if (!out.getName().toLowerCase().endsWith(wantExt)) {
            out = new File(out.getParentFile(), out.getName() + wantExt);
        }
        try {
            saveResultTo(out);
            app.flash("✓ Saved: " + out.getName(), StatusBar.Kind.OK);
        } catch (IOException e) {
            app.flash("Save தோல்வி: " + e.getMessage(), StatusBar.Kind.ERR);
        }
    }

    /** Package-visible - writes the current compress result to an explicit path; used by onSave and tests. */
    void saveResultTo(File out) throws IOException {
        Files.write(out.toPath(), resultData);
        String detail = isPdf
                ? ("PDF, " + resultPages + " page(s), " + ImageUtil.humanSize(resultData.length))
                : ("photo, " + resultW + "x" + resultH + "px, " + ImageUtil.humanSize(resultData.length));
        HistoryStore.logAction("compress", out.getName() + " (" + detail + ")");
    }

    /** Package-visible - loads a photo file into state as if chosen via dialog; used by tests. */
    void loadImageForTest(File f) throws IOException {
        srcPath = f.toPath();
        isPdf = false;
        BufferedImage img = ImageIO.read(f);
        if (img == null) throw new IOException("Unsupported image");
        srcImg = ImageUtil.toRgb(img);
        resultData = null;
    }

    /** Package-visible - loads a PDF file path into state as if chosen via dialog; used by tests. */
    void loadPdfForTest(File f) {
        srcPath = f.toPath();
        isPdf = true;
        srcImg = null;
        resultData = null;
    }

    byte[] resultDataForTest() {
        return resultData;
    }

    boolean isPdfForTest() {
        return isPdf;
    }

    int resultPagesForTest() {
        return resultPages;
    }

    @Override
    public void onShow() {
    }
}
