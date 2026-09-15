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

public class PdfToolsPanel extends JPanel implements ToolPanel {
    private final App app;
    private final JLabel status = new JLabel("Choose a PDF tool to begin.");

    public PdfToolsPanel(App app) {
        this.app = app;
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));

        ScrollableBox top = new ScrollableBox();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("PDF Tools");
        header.setFont(Theme.uiFont(Font.BOLD, 22));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(header);

        JLabel sub = new JLabel("Merge, split, extract and prepare scanned PDF documents locally.");
        sub.setFont(Theme.uiFont(13));
        sub.setForeground(Theme.INK_SOFT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(sub);
        top.add(Box.createVerticalStrut(12));

        JPanel tools = new JPanel(new GridLayout(0, 3, 12, 12));
        tools.setOpaque(false);
        tools.setAlignmentX(Component.LEFT_ALIGNMENT);
        tools.add(toolCard("Images to PDF", "Combine photos into one PDF document.", "Create PDF",
            () -> openOperation(PdfOperationDialog.Operation.IMAGES_TO_PDF)));
        tools.add(toolCard("Merge PDFs", "Combine scanned PDF files in the selected order.", "Merge Files",
            () -> openOperation(PdfOperationDialog.Operation.MERGE_PDFS)));
        tools.add(toolCard("Extract PDF Pages", "Export scanned pages as JPG images.", "Extract Pages",
            () -> openOperation(PdfOperationDialog.Operation.EXTRACT_PAGES)));
        tools.add(toolCard("Split PDF", "Create one PDF file for every scanned page.", "Split PDF",
            () -> openOperation(PdfOperationDialog.Operation.SPLIT_PDF)));
        tools.add(toolCard("PDF Info", "Check page count and image-page support.", "Check PDF",
            () -> openOperation(PdfOperationDialog.Operation.PDF_INFO)));
        tools.add(toolCard("Scan to PDF", "Capture pages directly from your scanner.", "Open Scanner", () -> app.showPanel("scan")));
        tools.add(toolCard("Compress PDF", "Reduce scanned PDF size with quality control.", "Open Compressor", () -> app.showPanel("compress")));
        top.add(tools);
        top.add(Box.createVerticalStrut(14));

        status.setFont(Theme.uiFont(12));
        status.setForeground(Theme.INK_SOFT);
        status.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        status.setOpaque(true);
        status.setBackground(Theme.SURFACE_2);
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(status);

        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    private void openOperation(PdfOperationDialog.Operation operation) {
        PdfOperationDialog dialog = new PdfOperationDialog(app, operation);
        dialog.setVisible(true);
    }

    private JPanel toolCard(String title, String description, String action, Runnable handler) {
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.uiFont(Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));

        WrapLabel descriptionLabel = new WrapLabel(description, 190);
        descriptionLabel.setFont(Theme.uiFont(12));
        descriptionLabel.setForeground(Theme.INK_SOFT);
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(descriptionLabel);
        card.add(Box.createVerticalGlue());

        JButton button = new JButton(action);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setFont(Theme.uiFont(12));
        button.addActionListener(e -> handler.run());
        card.add(button);
        return card;
    }

    private void imagesToPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "bmp", "tif", "tiff"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File[] files = chooser.getSelectedFiles();
        List<byte[]> jpegs = new ArrayList<>();
        List<int[]> sizes = new ArrayList<>();
        try {
            for (File file : files) {
                BufferedImage image = ImageIO.read(file);
                if (image == null) throw new IOException("Unsupported image: " + file.getName());
                jpegs.add(ImageUtil.encodeJpeg(image, 0.92f));
                sizes.add(new int[]{image.getWidth(), image.getHeight()});
            }
            JFileChooser save = new JFileChooser();
            save.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
            save.setSelectedFile(new File("merged-images.pdf"));
            if (save.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File output = ensureExtension(save.getSelectedFile(), ".pdf");
            Files.write(output.toPath(), PdfUtil.buildFromJpegPages(jpegs, sizes, 200.0));
            HistoryStore.logAction("pdf", files.length + " image(s) -> " + output.getName());
            done("Created " + output.getName() + " with " + files.length + " page(s).");
        } catch (Exception e) {
            fail("Images to PDF failed: " + e.getMessage());
        }
    }

    private void extractPages() {
        File pdf = choosePdf();
        if (pdf == null) return;
        try {
            List<byte[]> pages = PdfUtil.extractJpegPages(pdf.toPath());
            if (pages.isEmpty()) throw new IOException("No scanned image pages found.");
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File folder = chooser.getSelectedFile();
            for (int i = 0; i < pages.size(); i++) {
                Files.write(new File(folder, String.format("page-%03d.jpg", i + 1)).toPath(), pages.get(i));
            }
            HistoryStore.logAction("pdf", pdf.getName() + " -> " + pages.size() + " JPG page(s)");
            done("Extracted " + pages.size() + " page(s) to " + folder.getName() + ".");
        } catch (Exception e) {
            fail("Extract pages failed: " + e.getMessage());
        }
    }

    private void mergePdfs() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File[] files = chooser.getSelectedFiles();
        List<byte[]> pages = new ArrayList<>();
        List<int[]> sizes = new ArrayList<>();
        try {
            for (File file : files) {
                for (byte[] jpeg : PdfUtil.extractJpegPages(file.toPath())) {
                    BufferedImage image = PdfUtil.decodeJpeg(jpeg);
                    pages.add(jpeg);
                    sizes.add(new int[]{image.getWidth(), image.getHeight()});
                }
            }
            if (pages.isEmpty()) throw new IOException("No scanned image pages found.");
            JFileChooser save = new JFileChooser();
            save.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
            save.setSelectedFile(new File("merged.pdf"));
            if (save.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File output = ensureExtension(save.getSelectedFile(), ".pdf");
            Files.write(output.toPath(), PdfUtil.buildFromJpegPages(pages, sizes, 200.0));
            HistoryStore.logAction("pdf", files.length + " PDF(s) -> " + output.getName());
            done("Merged " + files.length + " PDF file(s) into " + output.getName() + ".");
        } catch (Exception e) {
            fail("Merge PDFs failed: " + e.getMessage());
        }
    }

    private void splitPdf() {
        File pdf = choosePdf();
        if (pdf == null) return;
        try {
            List<byte[]> pages = PdfUtil.extractJpegPages(pdf.toPath());
            if (pages.isEmpty()) throw new IOException("No scanned image pages found.");
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File folder = chooser.getSelectedFile();
            for (int i = 0; i < pages.size(); i++) {
                BufferedImage image = PdfUtil.decodeJpeg(pages.get(i));
                byte[] output = PdfUtil.buildFromJpegPages(
                        List.of(pages.get(i)), List.of(new int[]{image.getWidth(), image.getHeight()}), 200.0);
                Files.write(new File(folder, String.format("page-%03d.pdf", i + 1)).toPath(), output);
            }
            HistoryStore.logAction("pdf", pdf.getName() + " -> split into " + pages.size() + " PDF(s)");
            done("Split into " + pages.size() + " PDF file(s).");
        } catch (Exception e) {
            fail("Split PDF failed: " + e.getMessage());
        }
    }

    private void showPdfInfo() {
        File pdf = choosePdf();
        if (pdf == null) return;
        try {
            List<byte[]> pages = PdfUtil.extractJpegPages(pdf.toPath());
            JOptionPane.showMessageDialog(this,
                    "File: " + pdf.getName() + "\nSize: " + ImageUtil.humanSize(pdf.length())
                            + "\nScanned image pages: " + pages.size(),
                    "PDF Info", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            fail("PDF info failed: " + e.getMessage());
        }
    }

    private File choosePdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    private static File ensureExtension(File file, String extension) {
        return file.getName().toLowerCase().endsWith(extension) ? file
                : new File(file.getParentFile(), file.getName() + extension);
    }

    private void done(String message) {
        status.setText(message);
        app.flash(message, StatusBar.Kind.OK);
    }

    private void fail(String message) {
        status.setText(message);
        app.flash(message, StatusBar.Kind.ERR);
    }

    @Override
    public void onShow() {
    }
}
