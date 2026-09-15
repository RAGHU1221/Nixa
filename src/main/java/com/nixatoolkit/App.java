package com.nixatoolkit;

import com.nixatoolkit.ui.*;
import com.nixatoolkit.util.HistoryStore;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nixa Toolkit (Desktop, Java edition) - CSC front-desk utility application.
 *
 * Connects directly to a scanner attached to the computer (via the Windows
 * WIA driver layer, shelled out to through PowerShell - no separate scanner
 * software needed), combines pages into a PDF, converts image formats,
 * shrinks file sizes, and prepares passport/signature photos to an exact
 * pixel + KB size for government forms.
 *
 * Everything runs locally on this machine. No file or photo is ever
 * uploaded anywhere.
 *
 * This edition is written with zero external dependencies (no Maven
 * libraries) so it can be built and verified entirely with a plain JDK -
 * see README.md for why.
 */
public class App extends JFrame {
    public static final String APP_VERSION = "1.0.4";

    private final Map<String, JComponent> panels = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final StatusBar status = new StatusBar();
    private Map<String, Object> settings;

    private static final String[][] NAV_ITEMS = {
            {"home", "", "முகப்பு / Home"},
            {"scan", "", "Scan → PDF"},
            {"pdf-tools", "", "PDF Tools"},
            {"convert", "", "Image Converter"},
            {"compress", "", "Reduce File Size"},
            {"resize", "", "Form Photo / Signature"},
            {"capture", "", "Photo Import"},
            {"history", "", "Activity History"},
    };

    public App() {
        super("Nixa Toolkit — CSC Desktop (Java)  ·  v" + APP_VERSION);
        settings = HistoryStore.loadSettings();
        applyWindowGeometry();
        setMinimumSize(new Dimension(880, 620));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        buildMenuBar();

        JPanel sidebar = buildSidebar();
        Theme.styleButtons(sidebar);
        styleNavigationButtons();
        add(sidebar, BorderLayout.WEST);

        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 0, 18));
        content.setBackground(Theme.BG);
        add(content, BorderLayout.CENTER);

        JPanel statusWrap = new JPanel(new BorderLayout());
        statusWrap.setBackground(Theme.BG);
        statusWrap.setBorder(BorderFactory.createEmptyBorder(8, 18, 12, 18));
        statusWrap.add(status, BorderLayout.CENTER);
        add(statusWrap, BorderLayout.SOUTH);

        registerPanel("home", new HomePanel(this));
        registerPanel("scan", new ScanPanel(this));
        registerPanel("pdf-tools", new PdfToolsPanel(this));
        registerPanel("convert", new ConvertPanel(this));
        registerPanel("compress", new CompressPanel(this));
        registerPanel("resize", new ResizePanel(this));
        registerPanel("capture", new CapturePanel(this));
        registerPanel("history", new HistoryPanel(this));

        Theme.styleButtons(content);
        showPanel("home");

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveWindowGeometry();
                dispose();
                System.exit(0);
            }
        });
    }

    private void applyWindowGeometry() {
        try {
            int x = ((Number) settings.getOrDefault("x", 80)).intValue();
            int y = ((Number) settings.getOrDefault("y", 60)).intValue();
            int w = ((Number) settings.getOrDefault("width", 1080)).intValue();
            int h = ((Number) settings.getOrDefault("height", 720)).intValue();
            setBounds(x, y, w, h);
        } catch (Exception e) {
            setSize(1080, 720);
            setLocationRelativeTo(null);
        }
    }

    private void saveWindowGeometry() {
        try {
            settings.put("x", getX());
            settings.put("y", getY());
            settings.put("width", getWidth());
            settings.put("height", getHeight());
            HistoryStore.saveSettings(settings);
        } catch (Exception ignored) {
        }
    }

    private void buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem exit = new JMenuItem("Exit / வெளியேறு");
        exit.addActionListener(e -> {
            saveWindowGeometry();
            dispose();
            System.exit(0);
        });
        fileMenu.add(exit);
        bar.add(fileMenu);

        JMenu toolsMenu = new JMenu("Tools");
        for (String[] item : NAV_ITEMS) {
            JMenuItem mi = new JMenuItem(item[2]);
            mi.addActionListener(e -> showPanel(item[0]));
            toolsMenu.add(mi);
        }
        bar.add(toolsMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About Nixa Toolkit");
        about.addActionListener(e -> showAbout());
        helpMenu.add(about);
        bar.add(helpMenu);

        setJMenuBar(bar);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Nixa Toolkit — CSC Desktop (Java edition)\nVersion " + APP_VERSION + "\n\n"
                        + "Scan → PDF  ·  Image Converter  ·  Reduce File Size  ·\n"
                        + "Form Photo / Signature  ·  Photo Import  ·  Activity History\n\n"
                        + "எல்லாமே இந்த கணினியிலேயே process ஆகும் — "
                        + "எந்த file/photo-வும் எங்கும் upload ஆகாது.",
                "About / பற்றி",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel buildSidebar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBackground(Theme.SURFACE);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.LINE));
        bar.setPreferredSize(new Dimension(220, 0));
        bar.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(BorderFactory.createEmptyBorder(22, 18, 18, 18));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel brandIcon = new JLabel("Nixa Toolkit");
        brandIcon.setFont(Theme.uiFont(Font.BOLD, 16));
        brandIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel brandSub = new JLabel("CSC Desktop (Java) · v" + APP_VERSION);
        brandSub.setFont(Theme.uiFont(11));
        brandSub.setForeground(Theme.INK_SOFT);
        brandSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(brandIcon);
        brand.add(Box.createVerticalStrut(2));
        brand.add(brandSub);
        bar.add(brand);

        for (String[] item : NAV_ITEMS) {
            String key = item[0];
            JButton btn = new JButton(item[2]);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setFont(Theme.uiFont(13));
            btn.setFocusPainted(false);
            btn.setBackground(Theme.SURFACE);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> showPanel(key));
            bar.add(Box.createVerticalStrut(2));
            bar.add(btn);
            navButtons.put(key, btn);
        }

        bar.add(Box.createVerticalGlue());

        return bar;
    }

    private void registerPanel(String key, JComponent panel) {
        panels.put(key, panel);
        content.add(panel, key);
    }

    public void showPanel(String key) {
        for (Map.Entry<String, JButton> e : navButtons.entrySet()) {
            boolean active = e.getKey().equals(key);
            e.getValue().setBackground(active ? Theme.TEAL : Theme.SURFACE);
            e.getValue().setForeground(active ? Color.WHITE : Theme.INK);
        }
        cardLayout.show(content, key);
        JComponent panel = panels.get(key);
        if (panel instanceof ToolPanel) {
            ((ToolPanel) panel).onShow();
        }
    }

    private void styleNavigationButtons() {
        for (JButton button : navButtons.values()) {
            button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
            button.setFont(Theme.uiFont(Font.PLAIN, 13));
            button.setForeground(Theme.INK);
            button.setBackground(Theme.SURFACE);
        }
    }

    public void flash(String message, StatusBar.Kind kind) {
        status.show(message, kind);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
                // Fall back to the platform default look and feel.
            }
            System.out.println("[Nixa Toolkit] v" + APP_VERSION + " (Java) - settings/history stored at: "
                    + HistoryStore.dataDir());
            System.out.println("[Nixa Toolkit] Tamil-capable font in use: " + Theme.tamilFontFamily());
            App app = new App();
            app.setVisible(true);
        });
    }
}
