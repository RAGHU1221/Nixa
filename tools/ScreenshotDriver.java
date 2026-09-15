import com.nixatoolkit.App;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/** Dev-only driver: shows each panel in turn and screenshots it via Robot. Not part of the shipped app. */
public class ScreenshotDriver {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[0].getClassName());
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
        String[] panels = {"home", "scan", "convert", "compress", "resize", "capture", "history"};
        App[] holder = new App[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new App();
            holder[0].setVisible(true);
        });
        App app = holder[0];
        Robot robot = new Robot();
        Thread.sleep(600);
        for (String key : panels) {
            SwingUtilities.invokeAndWait(() -> app.showPanel(key));
            Thread.sleep(500);
            Rectangle bounds = app.getBounds();
            BufferedImage img = robot.createScreenCapture(bounds);
            ImageIO.write(img, "png", new File("/home/claude/shot-java-" + key + ".png"));
            System.out.println("saved " + key);
        }
        System.exit(0);
    }
}
