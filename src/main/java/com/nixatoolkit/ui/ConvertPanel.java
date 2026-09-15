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
import java.util.ArrayList;
import java.util.List;

public class ConvertPanel extends JPanel implements ToolPanel {
    private final App app;
    private final List<File> items = new ArrayList<>();
    private final JPanel listPanel = new JPanel();
    private final JLabel countLabel = new JLabel("Files (0)");
    private final JComboBox<String> formatMenu = new JComboBox<>(new String[]{"JPEG", "PNG"});
    private final JSlider qualitySlider = new JSlider(10, 100, 85);
    private final JButton convertBtn = new JButton("⚙ Convert All → Save to Folder");

    public ConvertPanel(App app) {
        this.app = app;
        setOpaque(false);
        setLayout(new BorderLayout(0, 10));

        ScrollableBox top = new ScrollableBox();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("படிவ மாற்று · Image Converter");
        header.setFont(Theme.uiFont(Font.BOLD, 20));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(header);
        top.add(Box.createVerticalStrut(10));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controls.setBackground(Theme.SURFACE);
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel fmtLbl = new JLabel("Output format:");
        fmtLbl.setFont(Theme.uiFont(12));
        controls.add(fmtLbl);
        formatMenu.setFont(Theme.uiFont(12));
        controls.add(formatMenu);
        JLabel qLbl = new JLabel("Quality:");
        qLbl.setFont(Theme.uiFont(12));
        controls.add(qLbl);
        qualitySlider.setPreferredSize(new Dimension(140, 24));
        qualitySlider.setOpaque(false);
        controls.add(qualitySlider);
        JButton chooseBtn = new JButton("📁 Choose Images");
        chooseBtn.setFont(Theme.uiFont(13));
        chooseBtn.addActionListener(e -> onChooseFiles());
        controls.add(chooseBtn);
        top.add(controls);
        top.add(Box.createVerticalStrut(6));

        JPanel listTop = new JPanel(new BorderLayout());
        listTop.setOpaque(false);
        listTop.setAlignmentX(Component.LEFT_ALIGNMENT);
        listTop.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        countLabel.setFont(Theme.uiFont(Font.BOLD, 13));
        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnBox.setOpaque(false);
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            items.clear();
            render();
        });
        convertBtn.setFont(Theme.uiFont(13));
        convertBtn.addActionListener(e -> onConvertAll());
        btnBox.add(clearBtn);
        btnBox.add(convertBtn);
        listTop.add(countLabel, BorderLayout.WEST);
        listTop.add(btnBox, BorderLayout.EAST);
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

        render();
    }

    private void onChooseFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "bmp", "tif", "tiff"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        for (File f : chooser.getSelectedFiles()) {
            items.add(f);
        }
        render();
    }

    private void render() {
        listPanel.removeAll();
        countLabel.setText("Files (" + items.size() + ")");
        for (int i = 0; i < items.size(); i++) {
            File f = items.get(i);
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(Theme.SURFACE);
            row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            JLabel name = new JLabel(f.getName());
            name.setFont(Theme.uiFont(12));
            row.add(name, BorderLayout.WEST);
            JLabel size = new JLabel(ImageUtil.humanSize(f.length()));
            size.setFont(Theme.uiFont(11));
            size.setForeground(Theme.INK_SOFT);
            row.add(size, BorderLayout.CENTER);
            int idx = i;
            JButton remove = new JButton("✕");
            remove.setForeground(Theme.DANGER);
            remove.addActionListener(e -> {
                items.remove(idx);
                render();
            });
            row.add(remove, BorderLayout.EAST);
            listPanel.add(row);
            listPanel.add(Box.createVerticalStrut(3));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void onConvertAll() {
        if (items.isEmpty()) {
            app.flash("Convert பண்ண files எதுவும் இல்ல.", StatusBar.Kind.WARN);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;
        File outDir = chooser.getSelectedFile();

        String fmt = (String) formatMenu.getSelectedItem();
        int quality = qualitySlider.getValue();
        convertBtn.setEnabled(false);
        convertBtn.setText("⚙ Converting...");

        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() {
                return convertAllSync(outDir, fmt, quality);
            }

            @Override
            protected void done() {
                convertBtn.setEnabled(true);
                convertBtn.setText("⚙ Convert All → Save to Folder");
                try {
                    int[] r = get();
                    reportConvertResult(r, outDir, fmt);
                } catch (Exception ignored) {
                }
            }
        };
        worker.execute();
    }

    /** Package-visible, synchronous conversion pass - used by the SwingWorker above and by tests. */
    int[] convertAllSync(File outDir, String fmt, int quality) {
        int ok = 0, fail = 0;
        String ext = "PNG".equals(fmt) ? ".png" : ".jpg";
        for (File f : items) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) throw new IOException("Unsupported image");
                String base = f.getName();
                int dot = base.lastIndexOf('.');
                if (dot > 0) base = base.substring(0, dot);
                File outFile = new File(outDir, base + ext);
                if ("PNG".equals(fmt)) {
                    ImageIO.write(img, "png", outFile);
                } else {
                    byte[] data = ImageUtil.encodeJpeg(img, quality / 100f);
                    java.nio.file.Files.write(outFile.toPath(), data);
                }
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        return new int[]{ok, fail};
    }

    void reportConvertResult(int[] r, File outDir, String fmt) {
        if (r[1] > 0) {
            app.flash("✓ " + r[0] + " converted, ✕ " + r[1] + " தோல்வி. Folder: " + outDir, StatusBar.Kind.WARN);
        } else {
            app.flash("✓ " + r[0] + " images converted → " + outDir, StatusBar.Kind.OK);
        }
        if (r[0] > 0) {
            HistoryStore.logAction("convert", r[0] + " file(s) -> " + fmt + " in " + outDir.getName());
        }
    }

    List<File> itemsForTest() {
        return items;
    }
}
