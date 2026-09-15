package com.nixatoolkit.ui;

import com.nixatoolkit.App;
import com.nixatoolkit.Theme;
import com.nixatoolkit.util.HistoryStore;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HomePanel extends JPanel implements ToolPanel {
    private final App app;
    private final JLabel todayVal = new JLabel("—");
    private final JLabel totalVal = new JLabel("—");
    private final JLabel lastVal = new JLabel("—");

    private static final String[][] QUICK_LINKS = {
            {"UIDAI Aadhaar", "https://uidai.gov.in"},
            {"PAN (NSDL)", "https://www.onlineservices.nsdl.com/paam/endUserRegisterContact.html"},
            {"Passport Seva", "https://www.passportindia.gov.in"},
            {"DigiLocker", "https://www.digilocker.gov.in"},
            {"TNPSC", "https://www.tnpsc.gov.in"},
            {"Income Tax e-Filing", "https://www.incometax.gov.in"},
            {"EPFO", "https://www.epfindia.gov.in"},
            {"CSC Digital Seva", "https://digitalseva.csc.gov.in"},
    };

    public HomePanel(App app) {
        this.app = app;
        setOpaque(false);
        setLayout(new BorderLayout());

        ScrollableBox top = new ScrollableBox();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel header = new JLabel("என்ன வேலை பண்ணணும்?");
        header.setFont(Theme.uiFont(Font.BOLD, 22));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Scanner connect, PDF, image convert, resize & compress — ஒரே இடத்தில்.");
        sub.setFont(Theme.uiFont(13));
        sub.setForeground(Theme.INK_SOFT);
        sub.setBorder(BorderFactory.createEmptyBorder(4, 0, 16, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 12, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        statsRow.add(statCard("இன்று · Today", todayVal));
        statsRow.add(statCard("மொத்தம் · Total", totalVal));
        statsRow.add(statCard("கடைசி செயல் · Last activity", lastVal));

        top.add(header);
        top.add(sub);
        top.add(statsRow);
        top.add(Box.createVerticalStrut(16));

        JPanel cardsGrid = new JPanel(new GridLayout(0, 3, 10, 10));
        cardsGrid.setOpaque(false);
        cardsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardsGrid.add(new ToolCard("🖨️", "ஸ்கேன் → PDF", "Scan to PDF",
                "Scanner-ஐ நேரடியா இணைத்து scan பண்ணி, பல பக்கங்களை ஒரே PDF ஆக்கலாம்.",
                () -> app.showPanel("scan")));
        cardsGrid.add(new ToolCard("🖼️", "படிவ மாற்று", "Image Converter",
                "JPG மற்றும் PNG இடையே மாற்றவும், பல படங்களையும் ஒரே நேரத்தில்.",
                () -> app.showPanel("convert")));
        cardsGrid.add(new ToolCard("📉", "அளவு குறை", "Reduce File Size",
                "Photo அல்லது PDF-ஐ நீங்க கேட்ட KB அளவுக்குள் தானாக compress பண்ணும்.",
                () -> app.showPanel("compress")));
        cardsGrid.add(new ToolCard("📐", "படிவ புகைப்படம்", "Form Photo / Signature",
                "Passport, Aadhaar, PAN, Signature — exact pixel + KB அளவுக்கு தயார்.",
                () -> app.showPanel("resize")));
        cardsGrid.add(new ToolCard("📷", "புகைப்படம் Import", "Photo Import",
                "ஏற்கனவே எடுத்த ஒரு photo-ஐ pick பண்ணி use பண்ணலாம்.",
                () -> app.showPanel("capture")));

        top.add(cardsGrid);
        top.add(Box.createVerticalStrut(16));

        JLabel linksLabel = new JLabel("🔗 அரசு சேவைகள் · Quick Links");
        linksLabel.setFont(Theme.uiFont(Font.BOLD, 12));
        linksLabel.setForeground(Theme.INK_SOFT);
        linksLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        linksLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        top.add(linksLabel);

        int cols = 4;
        int rows = (QUICK_LINKS.length + cols - 1) / cols;
        JPanel linksWrap = new JPanel(new GridLayout(rows, cols, 8, 6));
        linksWrap.setOpaque(false);
        linksWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        linksWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * 40));
        for (String[] link : QUICK_LINKS) {
            JButton b = new JButton(link[0]);
            b.setFont(Theme.uiFont(12));
            b.setFocusPainted(false);
            b.addActionListener(e -> openUrl(link[1]));
            linksWrap.add(b);
        }
        top.add(linksWrap);

        JScrollPane scroll = new JScrollPane(top);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel statCard(String label, JLabel valueLabel) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE, 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel l = new JLabel(label);
        l.setFont(Theme.uiFont(11));
        l.setForeground(Theme.INK_SOFT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setFont(Theme.uiFont(Font.BOLD, 18));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(l);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        return card;
    }

    private void openUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            app.flash("Link திறக்க முடியல்: " + e.getMessage(), StatusBar.Kind.ERR);
        }
    }

    @Override
    public void onShow() {
        List<HistoryStore.Entry> history = HistoryStore.loadHistory();
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        long todayCount = history.stream().filter(e -> e.ts.startsWith(today)).count();
        todayVal.setText(String.valueOf(todayCount));
        totalVal.setText(String.valueOf(history.size()));
        if (!history.isEmpty()) {
            HistoryStore.Entry last = history.get(history.size() - 1);
            lastVal.setText(HistoryPanel.shortLabel(last.kind));
        } else {
            lastVal.setText("—");
        }
    }
}
