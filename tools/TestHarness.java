package com.nixatoolkit.ui;

import com.nixatoolkit.App;
import com.nixatoolkit.util.HistoryStore;
import com.nixatoolkit.util.ImageUtil;
import com.nixatoolkit.util.MiniJson;
import com.nixatoolkit.util.PdfUtil;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plain-assertion test harness for the Java edition of Nixa Toolkit -
 * mirrors the rigor of the Python edition's test_harness.py, but without any
 * external test framework (no Maven/JUnit available in this build).
 *
 * Run: java -cp out TestHarness
 * Exits with code 0 if every check passes, 1 otherwise.
 */
public class TestHarness {
    static int passCount = 0;
    static int failCount = 0;
    static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("== Nixa Toolkit (Java) test harness ==");

        testMiniJson();
        testImageUtilHumanSize();
        testImageUtilToRgb();
        testImageUtilCropResizeCover();
        testImageUtilEncodeJpeg();
        testImageUtilCompressToTargetKb();
        testImageUtilBwScanEffect();
        testPdfRoundTrip();
        testPdfExtractOnNonPdf();
        testHistoryStore();

        // GUI-driven tests exercising the panel test hooks - must run on the EDT.
        SwingUtilities.invokeAndWait(TestHarness::runGuiTests);

        System.out.println();
        System.out.println(passCount + " passed, " + failCount + " failed");
        if (!failures.isEmpty()) {
            System.out.println("Failures:");
            for (String f : failures) System.out.println("  - " + f);
        }
        System.exit(failCount == 0 ? 0 : 1);
    }

    // ---------- helpers ----------

    static void check(String name, boolean condition) {
        if (condition) {
            passCount++;
            System.out.println("  [ok] " + name);
        } else {
            failCount++;
            failures.add(name);
            System.out.println("  [FAIL] " + name);
        }
    }

    static void checkEquals(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        check(name + " (expected=" + expected + ", actual=" + actual + ")", ok);
    }

    static BufferedImage solidImage(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        // add a bit of noise/detail so JPEG compression has something real to chew on
        g.setColor(new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue()));
        for (int i = 0; i < w; i += 7) {
            g.drawLine(i, 0, i, h);
        }
        g.dispose();
        return img;
    }

    // ---------- MiniJson ----------

    static void testMiniJson() {
        System.out.println("-- MiniJson --");
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("name", "Nixa");
        obj.put("count", 42);
        obj.put("ratio", 3.5);
        obj.put("active", true);
        obj.put("missing", null);
        List<Object> arr = new ArrayList<>();
        arr.add("a");
        arr.add(1);
        arr.add(false);
        obj.put("items", arr);
        obj.put("tamil", "தமிழ் \"quoted\" text\nwith newline");

        String json = MiniJson.write(obj);
        Object parsedObj = MiniJson.parse(json);
        check("parse(write(map)) returns a Map", parsedObj instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> parsed = (Map<String, Object>) parsedObj;
        checkEquals("string field round-trips", "Nixa", parsed.get("name"));
        checkEquals("int field round-trips", 42.0, ((Number) parsed.get("count")).doubleValue());
        checkEquals("double field round-trips", 3.5, ((Number) parsed.get("ratio")).doubleValue());
        checkEquals("boolean field round-trips", true, parsed.get("active"));
        check("null field round-trips", parsed.containsKey("missing") && parsed.get("missing") == null);
        check("tamil + escaped text round-trips exactly",
                "தமிழ் \"quoted\" text\nwith newline".equals(parsed.get("tamil")));
        Object itemsParsed = parsed.get("items");
        check("array field round-trips as a List", itemsParsed instanceof List);
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) itemsParsed;
        checkEquals("array length preserved", 3, items.size());
        checkEquals("array string element", "a", items.get(0));

        String pretty = MiniJson.writePretty(obj);
        check("writePretty produces multi-line output", pretty.contains("\n"));
        Object prettyParsed = MiniJson.parse(pretty);
        check("writePretty output re-parses to an equal map",
                prettyParsed instanceof Map && ((Map<?, ?>) prettyParsed).get("name").equals("Nixa"));

        Object emptyObj = MiniJson.parse("{}");
        check("empty object parses", emptyObj instanceof Map && ((Map<?, ?>) emptyObj).isEmpty());
        Object emptyArr = MiniJson.parse("[]");
        check("empty array parses", emptyArr instanceof List && ((List<?>) emptyArr).isEmpty());
    }

    // ---------- ImageUtil ----------

    static void testImageUtilHumanSize() {
        System.out.println("-- ImageUtil.humanSize --");
        check("bytes under 1KB shown as fractional KB (no B tier)", ImageUtil.humanSize(500).equals("0.5 KB"));
        check("a few KB shown as KB", ImageUtil.humanSize(20_000).contains("KB"));
        check("a few MB shown as MB", ImageUtil.humanSize(5_000_000).contains("MB"));
    }

    static void testImageUtilToRgb() {
        System.out.println("-- ImageUtil.toRgb --");
        BufferedImage argb = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        BufferedImage rgb = ImageUtil.toRgb(argb);
        check("toRgb strips alpha (no longer ARGB type)", rgb.getType() != BufferedImage.TYPE_INT_ARGB);
        checkEquals("toRgb preserves width", 10, rgb.getWidth());
        checkEquals("toRgb preserves height", 10, rgb.getHeight());
    }

    static void testImageUtilCropResizeCover() {
        System.out.println("-- ImageUtil.cropResizeCover --");
        BufferedImage src = solidImage(400, 200, Color.BLUE);
        BufferedImage out = ImageUtil.cropResizeCover(src, 200, 230);
        checkEquals("cropResizeCover width matches target", 200, out.getWidth());
        checkEquals("cropResizeCover height matches target", 230, out.getHeight());

        BufferedImage out2 = ImageUtil.cropResizeCover(src, 140, 60);
        checkEquals("cropResizeCover width matches target (signature preset)", 140, out2.getWidth());
        checkEquals("cropResizeCover height matches target (signature preset)", 60, out2.getHeight());
    }

    static void testImageUtilEncodeJpeg() throws IOException {
        System.out.println("-- ImageUtil.encodeJpeg --");
        BufferedImage src = solidImage(100, 100, Color.RED);
        byte[] high = ImageUtil.encodeJpeg(src, 0.95f);
        byte[] low = ImageUtil.encodeJpeg(src, 0.10f);
        check("encodeJpeg produces a valid JPEG (SOI marker)",
                (high[0] & 0xFF) == 0xFF && (high[1] & 0xFF) == 0xD8);
        check("lower quality produces a smaller (or equal) file", low.length <= high.length);
        BufferedImage decoded = ImageIO.read(new java.io.ByteArrayInputStream(high));
        check("encoded JPEG decodes back to an image", decoded != null);
    }

    static void testImageUtilCompressToTargetKb() {
        System.out.println("-- ImageUtil.compressToTargetKb --");
        BufferedImage src = solidImage(1000, 800, Color.GREEN);
        ImageUtil.CompressResult r = ImageUtil.compressToTargetKb(src, 20.0, 0.35);
        double actualKb = r.data.length / 1024.0;
        check("compressToTargetKb(20KB) result is at or under target (or marked overLimit)",
                actualKb <= 20.0 + 0.5 || r.overLimit);
        check("compressToTargetKb result has positive dimensions", r.width > 0 && r.height > 0);

        // A generously large target should be trivially satisfiable without hitting the floor.
        ImageUtil.CompressResult r2 = ImageUtil.compressToTargetKb(src, 500.0, 0.35);
        check("compressToTargetKb(500KB) is not overLimit for a generous target", !r2.overLimit);
    }

    static void testImageUtilBwScanEffect() {
        System.out.println("-- ImageUtil.applyBwScanEffect --");
        BufferedImage src = solidImage(50, 50, new Color(200, 50, 50));
        BufferedImage bw = ImageUtil.applyBwScanEffect(src);
        checkEquals("bwScanEffect preserves width", 50, bw.getWidth());
        checkEquals("bwScanEffect preserves height", 50, bw.getHeight());
        int rgb = bw.getRGB(5, 5);
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        check("bwScanEffect output is grayscale (R=G=B) at a sample pixel", r == g && g == b);
    }

    // ---------- PdfUtil ----------

    static void testPdfRoundTrip() throws Exception {
        System.out.println("-- PdfUtil round-trip (build then extract) --");
        List<byte[]> jpegs = new ArrayList<>();
        List<int[]> sizes = new ArrayList<>();
        BufferedImage page1 = solidImage(300, 400, Color.ORANGE);
        BufferedImage page2 = solidImage(300, 400, Color.CYAN);
        jpegs.add(ImageUtil.encodeJpeg(page1, 0.85f));
        sizes.add(new int[]{page1.getWidth(), page1.getHeight()});
        jpegs.add(ImageUtil.encodeJpeg(page2, 0.85f));
        sizes.add(new int[]{page2.getWidth(), page2.getHeight()});

        byte[] pdf = PdfUtil.buildFromJpegPages(jpegs, sizes, 200.0);
        check("buildFromJpegPages produces a PDF (starts with %PDF)",
                new String(pdf, 0, Math.min(5, pdf.length), java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF"));

        Path tmp = Files.createTempFile("nixa-test-", ".pdf");
        try {
            Files.write(tmp, pdf);
            List<byte[]> extracted = PdfUtil.extractJpegPages(tmp);
            checkEquals("extractJpegPages finds the same number of pages", 2, extracted.size());
            for (byte[] jpeg : extracted) {
                BufferedImage decoded = PdfUtil.decodeJpeg(jpeg);
                check("each extracted page decodes to a real image", decoded != null && decoded.getWidth() > 0);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    static void testPdfExtractOnNonPdf() throws IOException {
        System.out.println("-- PdfUtil.extractJpegPages on a non-PDF/typed-text-like file --");
        Path tmp = Files.createTempFile("nixa-test-not-pdf-", ".pdf");
        try {
            Files.write(tmp, "%PDF-1.4\nnot a real pdf, no jpeg streams here\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            List<byte[]> extracted = PdfUtil.extractJpegPages(tmp);
            check("extractJpegPages returns an empty list (not an exception) for a PDF with no image pages",
                    extracted.isEmpty());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ---------- HistoryStore ----------

    static void testHistoryStore() {
        System.out.println("-- HistoryStore --");
        HistoryStore.clearHistory();
        check("history starts empty after clear", HistoryStore.loadHistory().isEmpty());

        HistoryStore.logAction("scan", "2 page(s) -> test.pdf");
        HistoryStore.logAction("convert", "3 file(s) -> JPEG in out");
        List<HistoryStore.Entry> history = HistoryStore.loadHistory();
        checkEquals("logAction appends entries", 2, history.size());
        checkEquals("first entry kind preserved", "scan", history.get(0).kind);
        checkEquals("second entry detail preserved", "3 file(s) -> JPEG in out", history.get(1).detail);
        check("entry timestamp looks like yyyy-MM-dd HH:mm", history.get(0).ts.matches("\\d{4}-\\d{2}-\\d{2}.*"));

        HistoryStore.clearHistory();
        check("history empty again after clear", HistoryStore.loadHistory().isEmpty());

        Map<String, Object> settings = new HashMap<>(HistoryStore.loadSettings());
        settings.put("testMarker", 12345.0);
        HistoryStore.saveSettings(settings);
        Map<String, Object> reloaded = HistoryStore.loadSettings();
        checkEquals("saveSettings/loadSettings round-trips a custom field",
                12345.0, ((Number) reloaded.get("testMarker")).doubleValue());
    }

    // ---------- GUI-driven panel tests (must run on EDT) ----------

    static void runGuiTests() {
        System.out.println("-- GUI panel tests (EDT) --");
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        App app = new App();

        testScanPanel(app);
        testConvertPanel(app);
        testCompressPanelImage(app);
        testCompressPanelPdf(app);
        testResizePanel(app);
        testCapturePanel(app);

        app.dispose();
    }

    static File writeTempJpeg(BufferedImage img, String prefix) {
        try {
            File f = File.createTempFile(prefix, ".jpg");
            f.deleteOnExit();
            Files.write(f.toPath(), ImageUtil.encodeJpeg(img, 0.9f));
            return f;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void testScanPanel(App app) {
        System.out.println("  - ScanPanel");
        ScanPanel panel = new ScanPanel(app);
        checkEquals("ScanPanel starts with 0 pages", 0, panel.pagesForTest().size());
        panel.addPageForTest(solidImage(300, 400, Color.PINK));
        panel.addPageForTest(solidImage(300, 400, Color.MAGENTA));
        checkEquals("ScanPanel accumulates added pages", 2, panel.pagesForTest().size());

        try {
            File out = File.createTempFile("nixa-scan-test-", ".pdf");
            out.deleteOnExit();
            panel.generatePdfToFile(out);
            check("ScanPanel.generatePdfToFile writes a non-empty PDF", out.length() > 0);
            List<byte[]> pages = PdfUtil.extractJpegPages(out.toPath());
            checkEquals("generated PDF contains the same number of pages added", 2, pages.size());
        } catch (Exception e) {
            check("ScanPanel PDF generation threw: " + e, false);
        }
    }

    static void testConvertPanel(App app) {
        System.out.println("  - ConvertPanel");
        ConvertPanel panel = new ConvertPanel(app);
        File img1 = writeTempJpeg(solidImage(80, 80, Color.YELLOW), "nixa-convert-1-");
        File img2 = writeTempJpeg(solidImage(80, 80, Color.BLUE), "nixa-convert-2-");
        panel.itemsForTest().add(img1);
        panel.itemsForTest().add(img2);
        checkEquals("ConvertPanel test items registered", 2, panel.itemsForTest().size());

        File outDir = new File(System.getProperty("java.io.tmpdir"), "nixa-convert-out-" + System.nanoTime());
        outDir.mkdirs();
        int[] result = panel.convertAllSync(outDir, "PNG", 85);
        checkEquals("convertAllSync converts both files with no failures", 0, result[1]);
        checkEquals("convertAllSync reports 2 successes", 2, result[0]);
        File[] produced = outDir.listFiles((d, name) -> name.endsWith(".png"));
        check("convertAllSync actually wrote 2 PNG files to disk", produced != null && produced.length == 2);
    }

    static void testCompressPanelImage(App app) {
        System.out.println("  - CompressPanel (image)");
        CompressPanel panel = new CompressPanel(app);
        File img = writeTempJpeg(solidImage(600, 500, Color.DARK_GRAY), "nixa-compress-img-");
        try {
            panel.loadImageForTest(img);
        } catch (IOException e) {
            check("CompressPanel.loadImageForTest threw: " + e, false);
            return;
        }
        check("CompressPanel not in PDF mode after loading an image", !panel.isPdfForTest());
        ImageUtil.CompressResult r = panel.compressImageSync(30.0);
        panel.applyImageCompressResult(r);
        check("CompressPanel image compress produced result data", panel.resultDataForTest() != null
                && panel.resultDataForTest().length > 0);

        try {
            File out = File.createTempFile("nixa-compress-out-", ".jpg");
            out.deleteOnExit();
            panel.saveResultTo(out);
            check("CompressPanel.saveResultTo wrote a non-empty file", out.length() > 0);
        } catch (IOException e) {
            check("CompressPanel.saveResultTo threw: " + e, false);
        }
    }

    static void testCompressPanelPdf(App app) {
        System.out.println("  - CompressPanel (PDF)");
        CompressPanel panel = new CompressPanel(app);
        try {
            List<byte[]> jpegs = new ArrayList<>();
            List<int[]> sizes = new ArrayList<>();
            BufferedImage page = solidImage(300, 400, Color.LIGHT_GRAY);
            jpegs.add(ImageUtil.encodeJpeg(page, 0.9f));
            sizes.add(new int[]{page.getWidth(), page.getHeight()});
            byte[] pdf = PdfUtil.buildFromJpegPages(jpegs, sizes, 200.0);
            File pdfFile = File.createTempFile("nixa-compress-src-", ".pdf");
            pdfFile.deleteOnExit();
            Files.write(pdfFile.toPath(), pdf);

            panel.loadPdfForTest(pdfFile);
            check("CompressPanel in PDF mode after loading a PDF", panel.isPdfForTest());
            Object[] result = panel.compressPdfSync(pdfFile.toPath(), 100.0);
            panel.applyPdfCompressResult(result);
            check("CompressPanel PDF compress produced result data", panel.resultDataForTest() != null
                    && panel.resultDataForTest().length > 0);
            checkEquals("CompressPanel PDF compress kept 1 page", 1, panel.resultPagesForTest());

            File out = File.createTempFile("nixa-compress-pdf-out-", ".pdf");
            out.deleteOnExit();
            panel.saveResultTo(out);
            check("CompressPanel.saveResultTo (PDF) wrote a non-empty file", out.length() > 0);
        } catch (Exception e) {
            check("CompressPanel PDF path threw: " + e, false);
        }
    }

    static void testResizePanel(App app) {
        System.out.println("  - ResizePanel");
        ResizePanel panel = new ResizePanel(app);
        File img = writeTempJpeg(solidImage(1000, 1000, Color.RED), "nixa-resize-src-");
        try {
            panel.loadImageForTest(img);
        } catch (IOException e) {
            check("ResizePanel.loadImageForTest threw: " + e, false);
            return;
        }
        panel.selectPresetForTest("Passport Photo — 200x230px, <=20KB");
        ImageUtil.CompressResult r = panel.resizeCompressSync(200, 230, 20.0);
        panel.applyResizeResult(r, 20.0);
        checkEquals("ResizePanel passport preset produces exact width", 200, panel.resultWForTest());
        checkEquals("ResizePanel passport preset produces exact height", 230, panel.resultHForTest());
        check("ResizePanel result data present", panel.resultDataForTest() != null && panel.resultDataForTest().length > 0);

        try {
            File out = File.createTempFile("nixa-resize-out-", ".jpg");
            out.deleteOnExit();
            panel.saveResultTo(out);
            check("ResizePanel.saveResultTo wrote a non-empty file", out.length() > 0);
        } catch (IOException e) {
            check("ResizePanel.saveResultTo threw: " + e, false);
        }
    }

    static void testCapturePanel(App app) {
        System.out.println("  - CapturePanel");
        CapturePanel panel = new CapturePanel(app);
        File img = writeTempJpeg(solidImage(640, 480, Color.WHITE), "nixa-capture-src-");
        try {
            panel.loadPhoto(img);
        } catch (IOException e) {
            check("CapturePanel.loadPhoto threw: " + e, false);
            return;
        }
        check("CapturePanel loaded photo into state", panel.photoForTest() != null);

        try {
            File out = File.createTempFile("nixa-capture-out-", ".jpg");
            out.deleteOnExit();
            panel.savePhotoTo(out);
            check("CapturePanel.savePhotoTo wrote a non-empty file", out.length() > 0);
        } catch (IOException e) {
            check("CapturePanel.savePhotoTo threw: " + e, false);
        }
    }
}
