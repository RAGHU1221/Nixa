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
import java.util.ArrayList;
import java.util.List;

public class PdfOperationDialog extends JDialog {
    public enum Operation { IMAGES_TO_PDF, MERGE_PDFS, EXTRACT_PAGES, SPLIT_PDF, PDF_INFO }

    private final App app;
    private final Operation operation;
    private final DefaultListModel<File> files = new DefaultListModel<>();
    private final JList<File> fileList = new JList<>(files);
    private final JLabel preview = new JLabel("Select a document to preview", SwingConstants.CENTER);

    public PdfOperationDialog(App app, Operation operation) {
        super(app, operationTitle(operation), true);
        this.app = app;
        this.operation = operation;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 560);
        setLocationRelativeTo(app);
        buildUi();
    }

    private static String operationTitle(Operation operation) {
        switch (operation) {
            case IMAGES_TO_PDF: return "Images to PDF";
            case MERGE_PDFS: return "Merge PDFs";
            case EXTRACT_PAGES: return "Extract PDF Pages";
            case SPLIT_PDF: return "Split PDF";
            default: return "PDF Info";
        }
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel heading = new JLabel(operationTitle(operation) + "  |  Selected documents");
        heading.setFont(Theme.uiFont(Font.BOLD, 18));
        heading.setForeground(Theme.INK);
        root.add(heading, BorderLayout.NORTH);

        fileList.setBackground(Theme.SURFACE);
        fileList.setForeground(Theme.INK);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                            boolean selected, boolean focused) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focused);
                File file = (File) value;
                label.setText((index + 1) + ".  " + file.getName() + "  (" + ImageUtil.humanSize(file.length()) + ")");
                label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                label.setFont(Theme.uiFont(12));
                return label;
            }
        });
        fileList.addListSelectionListener(e -> showSelectedPreview());
        JScrollPane listScroll = new JScrollPane(fileList);
        listScroll.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        listScroll.setPreferredSize(new Dimension(420, 300));

        preview.setOpaque(true);
        preview.setBackground(Theme.SURFACE_2);
        preview.setForeground(Theme.INK_SOFT);
        preview.setBorder(BorderFactory.createLineBorder(Theme.LINE));
        preview.setPreferredSize(new Dimension(320, 300));
        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.add(listScroll, BorderLayout.CENTER);
        center.add(preview, BorderLayout.EAST);
        root.add(center, BorderLayout.CENTER);

        JButton add = new JButton("Add Files");
        add.addActionListener(e -> addFiles());
        JButton remove = new JButton("Remove Selected");
        remove.addActionListener(e -> removeSelected());
        JButton clear = new JButton("Clear All");
        clear.addActionListener(e -> files.clear());
        JButton run = new JButton(operationTitle(operation));
        run.addActionListener(e -> runOperation());
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(add);
        actions.add(remove);
        actions.add(clear);
        actions.add(run);
        actions.add(close);
        root.add(actions, BorderLayout.SOUTH);
        Theme.styleButtons(root);
        setContentPane(root);
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(operation == Operation.IMAGES_TO_PDF ? "Images" : "PDF",
                operation == Operation.IMAGES_TO_PDF ? new String[]{"jpg", "jpeg", "png", "bmp", "tif", "tiff"} : new String[]{"pdf"}));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        for (File file : chooser.getSelectedFiles()) {
            if (!contains(file)) files.addElement(file);
        }
        if (files.size() > 0) fileList.setSelectedIndex(files.size() - 1);
    }

    private boolean contains(File candidate) {
        for (int i = 0; i < files.size(); i++) if (files.get(i).equals(candidate)) return true;
        return false;
    }

    private void removeSelected() {
        int[] selected = fileList.getSelectedIndices();
        for (int i = selected.length - 1; i >= 0; i--) files.remove(selected[i]);
        preview.setIcon(null);
        preview.setText("Select a document to preview");
    }

    private void showSelectedPreview() {
        File file = fileList.getSelectedValue();
        if (file == null) return;
        try {
            BufferedImage image;
            if (operation == Operation.IMAGES_TO_PDF) {
                image = ImageIO.read(file);
            } else {
                List<byte[]> pages = PdfUtil.extractJpegPages(file.toPath());
                image = pages.isEmpty() ? null : PdfUtil.decodeJpeg(pages.get(0));
            }
            if (image == null) {
                preview.setIcon(null);
                preview.setText("No image preview available");
                return;
            }
            double scale = Math.min(290.0 / image.getWidth(), 260.0 / image.getHeight());
            Image scaled = image.getScaledInstance(Math.max(1, (int) (image.getWidth() * scale)),
                    Math.max(1, (int) (image.getHeight() * scale)), Image.SCALE_SMOOTH);
            preview.setText("");
            preview.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
            preview.setIcon(null);
            preview.setText("Preview unavailable");
        }
    }

    private void runOperation() {
        if (files.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Add at least one document.", "No documents", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            switch (operation) {
                case IMAGES_TO_PDF: imagesToPdf(); break;
                case MERGE_PDFS: mergePdfs(); break;
                case EXTRACT_PAGES: extractPages(); break;
                case SPLIT_PDF: splitPdf(); break;
                case PDF_INFO: showInfo(); break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "PDF operation failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private File chooseOutput(String name, String extension) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(extension.toUpperCase(), extension.substring(1)));
        chooser.setSelectedFile(new File(name + extension));
        return chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    private void imagesToPdf() throws Exception {
        List<byte[]> pages = new ArrayList<>();
        List<int[]> sizes = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            BufferedImage image = ImageIO.read(files.get(i));
            if (image == null) throw new IOException("Unsupported image: " + files.get(i).getName());
            pages.add(ImageUtil.encodeJpeg(image, 0.92f));
            sizes.add(new int[]{image.getWidth(), image.getHeight()});
        }
        File output = chooseOutput("images", ".pdf");
        if (output == null) return;
        Files.write(output.toPath(), PdfUtil.buildFromJpegPages(pages, sizes, 200.0));
        finish("Created " + output.getName());
    }

    private void mergePdfs() throws Exception {
        List<byte[]> pages = new ArrayList<>();
        List<int[]> sizes = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            for (byte[] jpeg : PdfUtil.extractJpegPages(files.get(i).toPath())) {
                BufferedImage image = PdfUtil.decodeJpeg(jpeg);
                pages.add(jpeg); sizes.add(new int[]{image.getWidth(), image.getHeight()});
            }
        }
        if (pages.isEmpty()) throw new IOException("No scanned image pages found.");
        File output = chooseOutput("merged", ".pdf");
        if (output == null) return;
        Files.write(output.toPath(), PdfUtil.buildFromJpegPages(pages, sizes, 200.0));
        finish("Merged " + files.size() + " PDF files");
    }

    private void extractPages() throws Exception {
        File folder = chooseFolder();
        if (folder == null) return;
        int count = 0;
        for (int i = 0; i < files.size(); i++) {
            for (byte[] jpeg : PdfUtil.extractJpegPages(files.get(i).toPath())) {
                Files.write(new File(folder, String.format("page-%03d.jpg", ++count)).toPath(), jpeg);
            }
        }
        if (count == 0) throw new IOException("No scanned image pages found.");
        finish("Extracted " + count + " JPG pages");
    }

    private void splitPdf() throws Exception {
        File folder = chooseFolder();
        if (folder == null) return;
        int count = 0;
        for (int i = 0; i < files.size(); i++) {
            for (byte[] jpeg : PdfUtil.extractJpegPages(files.get(i).toPath())) {
                BufferedImage image = PdfUtil.decodeJpeg(jpeg);
                byte[] pdf = PdfUtil.buildFromJpegPages(List.of(jpeg), List.of(new int[]{image.getWidth(), image.getHeight()}), 200.0);
                Files.write(new File(folder, String.format("page-%03d.pdf", ++count)).toPath(), pdf);
            }
        }
        if (count == 0) throw new IOException("No scanned image pages found.");
        finish("Created " + count + " PDF files");
    }

    private void showInfo() throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < files.size(); i++) {
            text.append(files.get(i).getName()).append(" - ")
                    .append(PdfUtil.extractJpegPages(files.get(i).toPath()).size()).append(" scanned page(s)\n");
        }
        JOptionPane.showMessageDialog(this, text.toString(), "PDF Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private File chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        return chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    private void finish(String message) {
        HistoryStore.logAction("pdf", message);
        app.flash(message, StatusBar.Kind.OK);
        JOptionPane.showMessageDialog(this, message, "PDF Tools", JOptionPane.INFORMATION_MESSAGE);
    }
}
