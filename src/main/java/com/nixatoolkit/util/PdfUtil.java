package com.nixatoolkit.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A small, dependency-free PDF reader/writer, scoped to exactly what Nixa
 * Toolkit needs: one JPEG image per page (this is what a document scanner
 * produces, and what our own Scan-to-PDF tool writes). This build cannot
 * fetch a real PDF library (Apache PDFBox) from Maven Central, so instead of
 * shipping an untested dependency, this hand-rolled reader/writer only
 * claims what it can actually do - and is fully covered by this project's
 * own tests.
 *
 * Writing: builds a minimal but valid PDF (header, one Image XObject + one
 * content stream + one Page per page, a Pages tree, xref table, trailer).
 *
 * Reading (best-effort): scans the raw PDF bytes for embedded JPEG
 * (/DCTDecode) image streams and extracts them page by page. This covers
 * scanned/photographed documents (exactly the CSC use case) but NOT
 * born-digital/typed-text PDFs, which have no embedded page images to find -
 * callers should treat "zero images found" as "not a scanned PDF".
 */
public final class PdfUtil {
    private PdfUtil() {
    }

    public static class PdfToolException extends Exception {
        public PdfToolException(String message) {
            super(message);
        }
    }

    // --------------------------------------------------------------- write

    /**
     * Builds a PDF from a list of JPEG-encoded page images (already
     * compressed by the caller). dpi controls the physical page size
     * (pageWidthPt = pixelWidth * 72 / dpi), purely cosmetic.
     */
    public static byte[] buildFromJpegPages(List<byte[]> jpegPages, List<int[]> pixelSizes, double dpi)
            throws IOException {
        if (jpegPages.isEmpty()) throw new IOException("No pages to write");
        int n = jpegPages.size();

        List<byte[]> objects = new ArrayList<>(); // index 0 == object number 1
        // Reserve object 1 = Catalog, 2 = Pages; pages start at 3.
        objects.add(null); // placeholder for Catalog (filled at the end - needs no forward refs actually)
        objects.add(null); // placeholder for Pages tree

        int[] pageObjNums = new int[n];
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int imgW = pixelSizes.get(i)[0];
            int imgH = pixelSizes.get(i)[1];
            double ptW = imgW * 72.0 / dpi;
            double ptH = imgH * 72.0 / dpi;

            int contentObjNum = objects.size() + 1;
            String content = String.format(java.util.Locale.ROOT,
                    "q\n%.3f 0 0 %.3f 0 0 cm\n/Im0 Do\nQ\n", ptW, ptH);
            byte[] contentBytes = content.getBytes(StandardCharsets.US_ASCII);
            byte[] contentObj = concat(
                    (contentObjNum + " 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n")
                            .getBytes(StandardCharsets.US_ASCII),
                    contentBytes,
                    "\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            objects.add(contentObj);

            int imgObjNum = objects.size() + 1;
            byte[] jpeg = jpegPages.get(i);
            byte[] imgHeader = ("" + imgObjNum + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + imgW
                    + " /Height " + imgH + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
                    + jpeg.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII);
            byte[] imgObj = concat(imgHeader, jpeg, "\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            objects.add(imgObj);

            int pageObjNum = objects.size() + 1;
            String pageDict = pageObjNum + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                    + fmt(ptW) + " " + fmt(ptH) + "] /Resources << /ProcSet [/PDF /ImageC] /XObject << /Im0 "
                    + imgObjNum + " 0 R >> >> /Contents " + contentObjNum + " 0 R >>\nendobj\n";
            objects.add(pageDict.getBytes(StandardCharsets.US_ASCII));

            pageObjNums[i] = pageObjNum;
            kids.append(pageObjNum).append(" 0 R ");
        }

        objects.set(0, ("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n").getBytes(StandardCharsets.US_ASCII));
        objects.set(1, ("2 0 obj\n<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + n
                + " >>\nendobj\n").getBytes(StandardCharsets.US_ASCII));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write("%PDF-1.4\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1));
        int[] offsets = new int[objects.size() + 1]; // 1-indexed
        for (int i = 0; i < objects.size(); i++) {
            offsets[i + 1] = out.size();
            out.write(objects.get(i));
        }
        int xrefStart = out.size();
        out.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        out.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i <= objects.size(); i++) {
            out.write(String.format("%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.US_ASCII));
        }
        out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefStart
                + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }

    private static String fmt(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format(java.util.Locale.ROOT, "%.3f", v);
    }

    private static byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) total += p.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    // ---------------------------------------------------------------- read

    /**
     * Best-effort extraction of embedded JPEG page images from an existing
     * PDF, in file order. Returns an empty list if none are found (e.g. a
     * born-digital/typed-text PDF with no scanned page images).
     */
    public static List<byte[]> extractJpegPages(Path pdfPath) throws IOException {
        byte[] raw = Files.readAllBytes(pdfPath);
        List<byte[]> jpegs = new ArrayList<>();
        byte[] filterMarker = "/DCTDecode".getBytes(StandardCharsets.US_ASCII);
        byte[] streamMarker = "stream".getBytes(StandardCharsets.US_ASCII);
        byte[] endstreamMarker = "endstream".getBytes(StandardCharsets.US_ASCII);

        int searchFrom = 0;
        while (true) {
            int filterIdx = indexOf(raw, filterMarker, searchFrom);
            if (filterIdx < 0) break;
            // The "stream" keyword for this object should follow reasonably
            // soon after its /DCTDecode filter declaration.
            int streamIdx = indexOf(raw, streamMarker, filterIdx);
            if (streamIdx < 0 || streamIdx - filterIdx > 2000) {
                searchFrom = filterIdx + filterMarker.length;
                continue;
            }
            int dataStart = streamIdx + streamMarker.length;
            // "stream" is followed by CRLF or LF before the binary data starts.
            if (dataStart < raw.length && raw[dataStart] == '\r') dataStart++;
            if (dataStart < raw.length && raw[dataStart] == '\n') dataStart++;

            int endIdx = indexOf(raw, endstreamMarker, dataStart);
            if (endIdx < 0) break;
            int dataEnd = endIdx;
            // Trim a single trailing EOL that precedes "endstream".
            if (dataEnd > dataStart && raw[dataEnd - 1] == '\n') dataEnd--;
            if (dataEnd > dataStart && raw[dataEnd - 1] == '\r') dataEnd--;

            byte[] jpeg = new byte[dataEnd - dataStart];
            System.arraycopy(raw, dataStart, jpeg, 0, jpeg.length);
            if (looksLikeJpeg(jpeg)) {
                jpegs.add(jpeg);
            }
            searchFrom = endIdx + endstreamMarker.length;
        }
        return jpegs;
    }

    private static boolean looksLikeJpeg(byte[] data) {
        return data.length > 4
                && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8
                && (data[data.length - 2] & 0xFF) == 0xFF && (data[data.length - 1] & 0xFF) == 0xD9;
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        outer:
        for (int i = Math.max(0, from); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    public static BufferedImage decodeJpeg(byte[] jpeg) throws IOException {
        BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(jpeg));
        if (img == null) throw new IOException("Could not decode embedded JPEG page");
        return img;
    }
}
