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
    public static final String APP_VERSION = "1.0.15";

    private final Map<String, JComponent> panels = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);
    private final StatusBar status = new StatusBar();
    private final JLabel breadcrumb = new JLabel("Home");
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
        styleNavigationButtons();
        add(sidebar, BorderLayout.WEST);

        add(buildWorkspaceHeader(), BorderLayout.NORTH);

        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        content.setBackground(Theme.BG);
        add(content, BorderLayout.CENTER);

        JPanel statusWrap = new JPanel(new BorderLayout());
        statusWrap.setBackground(Theme.BG);
        statusWrap.setBorder(BorderFactory.createEmptyBorder(8, 18, 12, 18));
        status.show("Ready  |  Java " + System.getProperty("java.version") + "  |  "
            + System.getProperty("os.name"), StatusBar.Kind.INFO);
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
        Theme.styleControls(content);
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
        JMenuItem openFile = new JMenuItem("Open File...");
        openFile.setAccelerator(KeyStroke.getKeyStroke("control O"));
        fileMenu.add(openFile);
        JMenuItem openFolder = new JMenuItem("Open Folder...");
        openFolder.setAccelerator(KeyStroke.getKeyStroke("control shift O"));
        fileMenu.add(openFolder);
        fileMenu.addSeparator();
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

        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem history = new JMenuItem("Activity History");
        history.addActionListener(e -> showPanel("history"));
        settingsMenu.add(history);
        JMenuItem output = new JMenuItem("Output Folder...");
        output.addActionListener(e -> chooseOutputFolder());
        settingsMenu.add(output);
        bar.add(settingsMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About Nixa Toolkit");
        about.addActionListener(e -> showAbout());
        helpMenu.add(about);
        bar.add(helpMenu);

        setJMenuBar(bar);
    }

    private JPanel buildWorkspaceHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 6));
        header.setBackground(Theme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tools.setOpaque(false);
        JButton home = new JButton("Home");
        home.addActionListener(e -> showPanel("home"));
        JTextField search = new JTextField(22);
        search.setToolTipText("Search tools");
        search.putClientProperty("JTextField.placeholderText", "Search tools...");
        tools.add(home);
        tools.add(search);
        JButton settingsButton = new JButton("Settings");
        settingsButton.addActionListener(e -> chooseOutputFolder());
        tools.add(settingsButton);
        header.add(tools, BorderLayout.NORTH);
        breadcrumb.setFont(Theme.uiFont(Font.PLAIN, 12));
        breadcrumb.setForeground(Theme.INK_SOFT);
        header.add(breadcrumb, BorderLayout.SOUTH);
        Theme.styleButtons(header);
        Theme.styleControls(header);
        return header;
    }

    private void chooseOutputFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            settings.put("outputFolder", chooser.getSelectedFile().getAbsolutePath());
            HistoryStore.saveSettings(settings);
            status.show("Output folder: " + chooser.getSelectedFile().getAbsolutePath(), StatusBar.Kind.OK);
        }
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
        bar.setBackground(new Color(0x24, 0x35, 0x49));
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
        brandIcon.setForeground(Color.WHITE);
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
            btn.setForeground(Color.WHITE);
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
            e.getValue().setBackground(active ? Theme.TEAL : new Color(0x24, 0x35, 0x49));
            e.getValue().setForeground(Color.WHITE);
        }
        cardLayout.show(content, key);
        String label = "Home";
        for (String[] item : NAV_ITEMS) if (item[0].equals(key)) label = item[2];
        breadcrumb.setText("Home  >  " + label);
        JComponent panel = panels.get(key);
        if (panel instanceof ToolPanel) {
            ((ToolPanel) panel).onShow();
        }
    }

    private void styleNavigationButtons() {
        for (JButton button : navButtons.values()) {
            button.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
            button.setFont(Theme.uiFont(Font.PLAIN, 13));
            button.setForeground(Color.WHITE);
            button.setBackground(new Color(0x24, 0x35, 0x49));
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
