package com.nixatoolkit.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Connects directly to a Windows scanner via WIA (Windows Image
 * Acquisition) - the same technology the earlier pywin32 edition used - but
 * through a small embedded PowerShell script instead of an in-process COM
 * binding, since this build has no access to a Java-COM library (e.g. JACOB)
 * from Maven Central. PowerShell ships with every Windows install, so this
 * needs nothing extra on the CSC computer, same as before.
 */
public final class ScannerUtil {
    private ScannerUtil() {
    }

    public static class ScannerException extends Exception {
        private final boolean cameraPermissionIssue;

        public ScannerException(String message) {
            this(message, false);
        }

        public ScannerException(String message, boolean cameraPermissionIssue) {
            super(message);
            this.cameraPermissionIssue = cameraPermissionIssue;
        }

        /** True when Windows likely hid the camera because desktop-app camera access is off in Settings. */
        public boolean isCameraPermissionIssue() {
            return cameraPermissionIssue;
        }
    }

    private static final String WIA_SCRIPT =
            "param(\n" +
            "  [string]$Mode = \"list\",\n" +
            "  [string]$OutPath = \"\",\n" +
            "  [string]$Intent = \"color\",\n" +
            "  [int]$Dpi = 300,\n" +
            "  [string]$DeviceName = \"\"\n" +
            ")\n" +
            "$ErrorActionPreference = 'Stop'\n" +
            "if ($Mode -eq \"list\") {\n" +
            "  try {\n" +
            "    $mgr = New-Object -ComObject WIA.DeviceManager\n" +
            "    foreach ($info in $mgr.DeviceInfos) {\n" +
            "      if ($info.Type -eq 1) {\n" +
            "        Write-Output $info.Properties(\"Name\").Value\n" +
            "      }\n" +
            "    }\n" +
            "    exit 0\n" +
            "  } catch {\n" +
            "    exit 3\n" +
            "  }\n" +
            "}\n" +
            "try {\n" +
            "  $mgr = New-Object -ComObject WIA.DeviceManager\n" +
            "  $info = $null\n" +
            "  foreach ($candidate in $mgr.DeviceInfos) {\n" +
            "    if ($candidate.Type -eq 1 -and ($DeviceName -eq \"\" -or $candidate.Properties(\"Name\").Value -eq $DeviceName)) { $info = $candidate; break }\n" +
            "  }\n" +
            "  if ($null -eq $info) { exit 4 }\n" +
            "  $device = $info.Connect()\n" +
            "  $item = $device.Items.Item(1)\n" +
            "  foreach ($property in $item.Properties) {\n" +
            "    try {\n" +
            "      if ($property.PropertyID -eq 6146) { $property.Value = 24 }\n" +
            "      if ($property.PropertyID -eq 6147 -or $property.PropertyID -eq 6148) { $property.Value = $Dpi }\n" +
            "      if ($property.PropertyID -eq 6149 -or $property.PropertyID -eq 6150) { $property.Value = 0 }\n" +
            "    } catch { }\n" +
            "  }\n" +
            "  try { $item.Properties.Item(6147).Value = $Dpi } catch { }\n" +
            "  try { $item.Properties.Item(6148).Value = $Dpi } catch { }\n" +
            "  try { $item.Properties.Item(6149).Value = 0 } catch { }\n" +
            "  try { $item.Properties.Item(6150).Value = 0 } catch { }\n" +
            "  try { $item.Properties.Item(6151).Value = $item.Properties.Item(6151).SubTypeMax } catch { }\n" +
            "  try { $item.Properties.Item(6152).Value = $item.Properties.Item(6152).SubTypeMax } catch { }\n" +
            "} catch {\n" +
            "  Write-Error \"Scanner setup failed: $_\"\n" +
            "  exit 3\n" +
            "}\n" +
            "$intentMap = @{ \"color\" = 1; \"gray\" = 2; \"text\" = 4 }\n" +
            "$intentVal = $intentMap[$Intent]\n" +
            "if (-not $intentVal) { $intentVal = 1 }\n" +
            "try {\n" +
            "  $dialog = New-Object -ComObject WIA.CommonDialog\n" +
            "} catch {\n" +
            "  exit 3\n" +
            "}\n" +
            "try {\n" +
            "  $image = $dialog.ShowTransfer($item, " +
            "\"{B96B3CAE-0728-11D3-9D7B-0000F81EF32E}\", $false)\n" +
            "} catch {\n" +
            "  Write-Error \"$_\"\n" +
            "  exit 1\n" +
            "}\n" +
            "if ($null -eq $image) {\n" +
            "  exit 2\n" +
            "}\n" +
            "$image.SaveFile($OutPath)\n" +
            "exit 0\n";

    private static final String CAMERA_SCRIPT =
            "param([string]$OutPath)\n" +
            "$ErrorActionPreference = 'Stop'\n" +
            "try {\n" +
            "  $dialog = New-Object -ComObject WIA.CommonDialog\n" +
            "  $image = $dialog.ShowAcquireImage(2, 1, 65536, " +
            "\"{B96B3CAE-0728-11D3-9D7B-0000F81EF32E}\", $false, $true)\n" +
            "  if ($null -eq $image) { exit 2 }\n" +
            "  $image.SaveFile($OutPath)\n" +
            "  exit 0\n" +
            "} catch { Write-Error \"$_\"; exit 1 }\n";

    private static Path writeScript() throws IOException {
        Path script = Files.createTempFile("nixa_wia_", ".ps1");
        Files.writeString(script, WIA_SCRIPT, StandardCharsets.UTF_8);
        script.toFile().deleteOnExit();
        return script;
    }

    /** Never throws - returns [] on any failure (no scanner, not Windows, PowerShell missing). */
    public static List<String> listConnectedScanners() {
        List<String> names = new ArrayList<>();
        try {
            Path script = writeScript();
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", script.toString(), "-Mode", "list")
                    .redirectErrorStream(false)
                    .start();
            try (var reader = p.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) names.add(line.trim());
                }
            }
            p.waitFor(15, TimeUnit.SECONDS);
            Files.deleteIfExists(script);
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
        return names;
    }

    /** Returns null if the user cancelled the scan dialog. */
    public static BufferedImage scan(String intent) throws ScannerException {
        return scan(intent, 300, "");
    }

    /** Opens the native Windows WIA camera acquisition dialog with live preview. */
    public static BufferedImage captureCamera() throws ScannerException {
        Path script;
        Path outFile;
        try {
            script = Files.createTempFile("nixa_camera_", ".ps1");
            Files.writeString(script, CAMERA_SCRIPT, StandardCharsets.UTF_8);
            outFile = Files.createTempFile("nixa_camera_", ".jpg");
            Files.deleteIfExists(outFile);
        } catch (IOException e) {
            throw new ScannerException("Camera temp file உருவாக்க முடியல்: " + e.getMessage());
        }
        try {
            Process process = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", script.toString(), "-OutPath", outFile.toString())
                    .redirectErrorStream(true).start();
            String output;
            try (var reader = process.inputReader(StandardCharsets.UTF_8)) {
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) buffer.append(line).append('\n');
                output = buffer.toString();
            }
            process.waitFor(120, TimeUnit.SECONDS);
            if (process.exitValue() == 2) return null;
            if (process.exitValue() != 0) {
                boolean permissionIssue = output.toLowerCase().contains("no wia device of the selected type");
                if (permissionIssue) {
                    throw new ScannerException(
                            "Camera-க்கு permission இல்லை போல. Windows Settings-ல் \"Let desktop apps access "
                            + "your camera\" ஐ ON பண்ணி மறுபடி முயற்சி செய்யவும்.", true);
                }
                throw new ScannerException("Windows Camera திறக்க முடியவில்லை" +
                        (output.isBlank() ? "" : ": " + output.trim()));
            }
            BufferedImage image = ImageIO.read(outFile.toFile());
            if (image == null) throw new IOException("Camera image readable இல்லை");
            return image;
        } catch (IOException e) {
            throw new ScannerException("Windows camera support கிடைக்கவில்லை: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScannerException("Camera capture interrupt ஆச்சு.");
        } finally {
            try { Files.deleteIfExists(script); } catch (IOException ignored) { }
            try { Files.deleteIfExists(outFile); } catch (IOException ignored) { }
        }
    }

    /** Scans from the selected WIA device at the requested resolution. */
    public static BufferedImage scan(String intent, int dpi, String deviceName) throws ScannerException {
        Path outFile;
        Path script;
        try {
            script = writeScript();
            outFile = Files.createTempFile("nixa_scan_", ".jpg");
            Files.deleteIfExists(outFile); // WIA needs to create this file itself
        } catch (IOException e) {
            throw new ScannerException("Temp file உருவாக்க முடியல்: " + e.getMessage());
        }

        int exitCode;
        String stderr;
        try {
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", script.toString(), "-Mode", "scan", "-OutPath", outFile.toString(),
                    "-Intent", intent, "-Dpi", String.valueOf(dpi),
                    "-DeviceName", deviceName == null ? "" : deviceName)
                    .redirectErrorStream(false)
                    .start();
            StringBuilder errBuf = new StringBuilder();
            try (var reader = p.errorReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) errBuf.append(line).append('\n');
            }
            boolean finished = p.waitFor(180, TimeUnit.SECONDS);
            exitCode = finished ? p.exitValue() : -1;
            stderr = errBuf.toString();
        } catch (IOException e) {
            throw new ScannerException(
                    "Scanner support (PowerShell/WIA) இந்த computer-ல் இல்ல, அல்லது இது Windows இல்ல.\n"
                            + "இது Windows-ல் மட்டும் வேலை செய்யும்.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScannerException("Scan interrupt ஆச்சு.");
        } finally {
            try { Files.deleteIfExists(script); } catch (IOException ignored) { }
        }

        if (exitCode == 2) {
            return null; // user cancelled - not an error
        }
        if (exitCode == 3) {
            throw new ScannerException(
                    "WIA (Windows scanner driver layer) இந்த computer-ல் கிடைக்கல், அல்லது இது Windows இல்ல.");
        }
        if (exitCode == 4) {
            throw new ScannerException("தேர்ந்தெடுத்த Scanner கிடைக்கவில்லை. Scanner list-ஐ refresh செய்து மறுபடி முயற்சி செய்யவும்.");
        }
        if (exitCode != 0) {
            String msg = stderr.isBlank() ? "" : (": " + stderr.trim());
            if (stderr.toLowerCase().contains("no scanner") || stderr.contains("0x80210015")) {
                throw new ScannerException(
                        "எந்த Scanner-உம் கண்டுபிடிக்க முடியல்.\n"
                                + "Scanner-ஐ USB-ல் connect பண்ணி, driver install ஆகிருக்கான்னு பாத்து மறுபடி முயற்சி செய்யவும்.");
            }
            throw new ScannerException("Scanner-ஐ இணைக்க முடியல்" + msg);
        }

        try {
            BufferedImage img = ImageIO.read(outFile.toFile());
            if (img == null) throw new IOException("Scan file readable இல்ல");
            return img;
        } catch (IOException e) {
            throw new ScannerException("Scan செய்த file-ஐ படிக்க முடியல்: " + e.getMessage());
        } finally {
            try { Files.deleteIfExists(outFile); } catch (IOException ignored) { }
        }
    }
}
