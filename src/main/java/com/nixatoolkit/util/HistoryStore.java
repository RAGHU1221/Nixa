package com.nixatoolkit.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings + usage history, stored as two small local JSON files - never
 * uploaded anywhere. Mirrors the Python edition's history.json/settings.json
 * design: a window-geometry settings file, and an append-only (capped)
 * activity log used by the Activity History tab and Home dashboard.
 */
public final class HistoryStore {
    private static final int HISTORY_LIMIT = 500;
    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static Path dataDir;

    private HistoryStore() {
    }

    public static synchronized Path dataDir() {
        if (dataDir != null) return dataDir;
        String appData = System.getenv("APPDATA");
        Path base;
        if (appData != null && !appData.isBlank()) {
            base = Paths.get(appData, "NixaToolkit");
        } else {
            base = Paths.get(System.getProperty("user.home"), ".nixa-toolkit");
        }
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            base = Paths.get(System.getProperty("java.io.tmpdir"));
        }
        dataDir = base;
        return dataDir;
    }

    private static Path settingsPath() {
        return dataDir().resolve("settings.json");
    }

    private static Path historyPath() {
        return dataDir().resolve("history.json");
    }

    // ------------------------------------------------------------ settings

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadSettings() {
        try {
            String text = Files.readString(settingsPath(), StandardCharsets.UTF_8);
            Object parsed = MiniJson.parse(text);
            if (parsed instanceof Map) return (Map<String, Object>) parsed;
        } catch (Exception ignored) {
            // Missing/corrupt settings file - fall back to defaults, never crash.
        }
        return new LinkedHashMap<>();
    }

    public static void saveSettings(Map<String, Object> settings) {
        try {
            Files.writeString(settingsPath(), MiniJson.writePretty(settings), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // Best-effort only - settings are a convenience, never load-bearing.
        }
    }

    // ------------------------------------------------------------- history

    public static final class Entry {
        public final String ts;
        public final String kind;
        public final String detail;

        public Entry(String ts, String kind, String detail) {
            this.ts = ts;
            this.kind = kind;
            this.detail = detail;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Entry> loadHistory() {
        List<Entry> out = new ArrayList<>();
        try {
            String text = Files.readString(historyPath(), StandardCharsets.UTF_8);
            Object parsed = MiniJson.parse(text);
            if (parsed instanceof List) {
                for (Object item : (List<Object>) parsed) {
                    if (item instanceof Map) {
                        Map<String, Object> m = (Map<String, Object>) item;
                        out.add(new Entry(
                                String.valueOf(m.getOrDefault("ts", "")),
                                String.valueOf(m.getOrDefault("kind", "")),
                                String.valueOf(m.getOrDefault("detail", ""))));
                    }
                }
            }
        } catch (Exception ignored) {
            // Missing/corrupt history file - treat as empty, never crash.
        }
        return out;
    }

    public static synchronized void logAction(String kind, String detail) {
        try {
            List<Entry> history = loadHistory();
            String ts = LocalDateTime.now().format(TS_FORMAT);
            history.add(new Entry(ts, kind, detail));
            int from = Math.max(0, history.size() - HISTORY_LIMIT);
            List<Entry> trimmed = history.subList(from, history.size());

            List<Object> arr = new ArrayList<>();
            for (Entry e : trimmed) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ts", e.ts);
                m.put("kind", e.kind);
                m.put("detail", e.detail);
                arr.add(m);
            }
            Files.writeString(historyPath(), MiniJson.writePretty(arr), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // Best-effort only, mirrors the Python edition's log_action().
        }
    }

    public static void clearHistory() {
        try {
            Files.writeString(historyPath(), "[]", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }
}
