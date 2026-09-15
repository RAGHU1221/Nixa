package com.nixatoolkit.ui;

import com.nixatoolkit.App;
import com.nixatoolkit.Theme;
import com.nixatoolkit.util.HistoryStore;
import com.nixatoolkit.util.ImageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Photo Import - picks an existing photo (taken with a phone camera, or
 * whatever software already runs the CSC computer's own webcam) rather than
 * a live in-app camera preview. A true live webcam preview needs a native
 * capture library (e.g. sarxos webcam-capture) from Maven Central, which
 * this build cannot fetch - see README.md.
 */
public class CapturePanel extends JPanel implements ToolPanel {
    private final App app;
    private BufferedImage photo;
    private final JLabel previewLabel = new JLabel("ஒரு photo pick பண்ணுங்க...", SwingConstants.CENTER);
    private final JButton saveBtn = new JButton("Save Photo");

    public CapturePanel(App app) {
        this.app = app;
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        ScrollableBox top = new ScrollableBox();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("புகைப்படம் Import · Photo Import");
        header.setFont(Theme.uiFont(Font.BOLD, 20));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(header);

        JLabel note = new JLabel("<html><div style='width:820px'>Live webcam preview இந்த Java build-ல் "
                + "கிடைக்கல் (அதற்கான library internet restriction-ஆல் add பண்ண முடியல்) — பதிலா ஏற்கனவே "
                + "phone/webcam software மூலம் எடுத்த ஒரு photo-ஐ இங்க pick பண்ணி பயன்படுத்தலாம்.</div></html>");
        note.setFont(Theme.uiFont(11));
        note.setForeground(Theme.WARN);
        note.setOpaque(true);
        note.setBackground(Theme.WARN_BG);
        note.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        note.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 0, 10, 0), note.getBorder()));
        top.add(note);

        previewLabel.setPreferredSize(new Dimension(420, 320));
        previewLabel.setMaximumSize(new Dimension(420, 320));
        previewLabel.setOpaque(true);
        previewLabel.setBackground(Theme.SURFACE_2);
        previewLabel.setFont(Theme.uiFont(12));
        previewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(previewLabel);
        top.add(Box.createVerticalStrut(12));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton pickBtn = new JButton("Pick Photo");
        pickBtn.setFont(Theme.uiFont(13));
        pickBtn.addActionListener(e -> onPick());
        saveBtn.setFont(Theme.uiFont(13));
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(e -> onSave());
        btnRow.add(pickBtn);
        btnRow.add(saveBtn);
        top.add(btnRow);

        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    private void onPick() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "bmp"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        try {
            loadPhoto(chooser.getSelectedFile());
            Image thumb = photo.getScaledInstance(-1, 300, Image.SCALE_SMOOTH);
            previewLabel.setIcon(new ImageIcon(thumb));
            previewLabel.setText(null);
            saveBtn.setEnabled(true);
        } catch (Exception e) {
            app.flash("Photo திறக்க முடியல்: " + e.getMessage(), StatusBar.Kind.ERR);
        }
    }

    /** Package-visible - loads a photo file into state; used by onPick above and by tests. */
    void loadPhoto(File f) throws IOException {
        BufferedImage img = ImageIO.read(f);
        if (img == null) throw new IOException("Unsupported image");
        photo = ImageUtil.toRgb(img);
    }

    private void onSave() {
        if (photo == null) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("JPEG", "jpg"));
        chooser.setSelectedFile(new File("photo-"
                + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".jpg"));
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File out = chooser.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".jpg")) {
            out = new File(out.getParentFile(), out.getName() + ".jpg");
        }
        try {
            savePhotoTo(out);
            app.flash("✓ Saved: " + out.getName(), StatusBar.Kind.OK);
        } catch (IOException e) {
            app.flash("Save தோல்வி: " + e.getMessage(), StatusBar.Kind.ERR);
        }
    }

    /** Package-visible - saves the current photo to an explicit path; used by onSave above and by tests. */
    void savePhotoTo(File out) throws IOException {
        byte[] data = ImageUtil.encodeJpeg(photo, 0.92f);
        java.nio.file.Files.write(out.toPath(), data);
        HistoryStore.logAction("capture", out.getName());
    }

    BufferedImage photoForTest() {
        return photo;
    }

    @Override
    public void onShow() {
    }
}
