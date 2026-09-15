package com.nixatoolkit.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/** Pure java.awt/ImageIO helpers - no external dependencies. */
public final class ImageUtil {
    private ImageUtil() {
    }

    public static String humanSize(long bytes) {
        if (bytes >= 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format("%.1f KB", bytes / 1024.0);
    }

    /** Ensures a plain RGB (no alpha) image, matching the JPEG pipeline. */
    public static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, src.getWidth(), src.getHeight());
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /**
     * Resize+crop to EXACTLY targetW x targetH, cropping overflow from the
     * center - matches how passport-photo tools work.
     */
    public static BufferedImage cropResizeCover(BufferedImage src, int targetW, int targetH) {
        src = toRgb(src);
        int srcW = src.getWidth(), srcH = src.getHeight();
        double srcRatio = (double) srcW / srcH;
        double dstRatio = (double) targetW / targetH;
        BufferedImage cropped;
        if (srcRatio > dstRatio) {
            int newW = Math.max(1, (int) Math.round(srcH * dstRatio));
            int x0 = (srcW - newW) / 2;
            cropped = src.getSubimage(Math.max(0, x0), 0, Math.min(newW, srcW - Math.max(0, x0)), srcH);
        } else {
            int newH = Math.max(1, (int) Math.round(srcW / dstRatio));
            int y0 = (srcH - newH) / 2;
            cropped = src.getSubimage(0, Math.max(0, y0), srcW, Math.min(newH, srcH - Math.max(0, y0)));
        }
        return scale(cropped, targetW, targetH);
    }

    public static byte[] encodeJpeg(BufferedImage img, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IOException("No JPEG writer available");
        ImageWriter writer = writers.next();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(toRgb(img), null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    public static final class CompressResult {
        public final byte[] data;
        public final double quality;
        public final int width, height;
        public final boolean overLimit;

        CompressResult(byte[] data, double quality, int width, int height, boolean overLimit) {
            this.data = data;
            this.quality = quality;
            this.width = width;
            this.height = height;
            this.overLimit = overLimit;
        }
    }

    /**
     * Binary-searches JPEG quality to fit under targetKb; if quality alone
     * can't get there, progressively downscales and retries. Mirrors the
     * Python edition's compress_to_target_kb() algorithm exactly.
     */
    public static CompressResult compressToTargetKb(BufferedImage img, double targetKb, double minScale) {
        img = toRgb(img);
        int origW = img.getWidth(), origH = img.getHeight();
        int w = origW, h = origH;
        for (int attempt = 0; attempt < 6; attempt++) {
            double lo = 0.06, hi = 0.95;
            byte[] best = null;
            double bestQ = 0;
            BufferedImage current = (w == origW && h == origH) ? img : scale(img, w, h);
            for (int i = 0; i < 7; i++) {
                double mid = (lo + hi) / 2;
                try {
                    byte[] data = encodeJpeg(current, (float) mid);
                    double sizeKb = data.length / 1024.0;
                    if (sizeKb <= targetKb) {
                        best = data;
                        bestQ = mid;
                        lo = mid;
                    } else {
                        hi = mid;
                    }
                } catch (IOException e) {
                    break;
                }
            }
            if (best != null) {
                return new CompressResult(best, bestQ, w, h, false);
            }
            int newW = (int) (w * 0.82), newH = (int) (h * 0.82);
            if (newW < origW * minScale) break;
            w = newW;
            h = newH;
        }
        BufferedImage current = scale(img, w, h);
        try {
            byte[] data = encodeJpeg(current, 0.06f);
            return new CompressResult(data, 0.06, w, h, true);
        } catch (IOException e) {
            throw new RuntimeException("JPEG encode failed", e);
        }
    }

    /** Grayscale + a simple auto-contrast stretch - a document-scan look. */
    public static BufferedImage applyBwScanEffect(BufferedImage src) {
        src = toRgb(src);
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        int[] hist = new int[256];
        byte[] pixels = ((java.awt.image.DataBufferByte) gray.getRaster().getDataBuffer()).getData();
        for (byte p : pixels) hist[p & 0xFF]++;

        int total = w * h;
        int cutoff = Math.max(1, total / 100); // ~1% cutoff, like PIL's autocontrast(cutoff=1)
        int lo = 0, hi = 255;
        int seen = 0;
        for (int i = 0; i < 256; i++) {
            seen += hist[i];
            if (seen > cutoff) { lo = i; break; }
        }
        seen = 0;
        for (int i = 255; i >= 0; i--) {
            seen += hist[i];
            if (seen > cutoff) { hi = i; break; }
        }
        if (hi <= lo) { lo = 0; hi = 255; }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        double scale = 255.0 / Math.max(1, (hi - lo));
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = pixels[y * w + x] & 0xFF;
                int stretched = (int) Math.round((v - lo) * scale);
                if (stretched < 0) stretched = 0;
                if (stretched > 255) stretched = 255;
                int rgb = (stretched << 16) | (stretched << 8) | stretched;
                out.setRGB(x, y, rgb);
            }
        }
        return out;
    }
}
