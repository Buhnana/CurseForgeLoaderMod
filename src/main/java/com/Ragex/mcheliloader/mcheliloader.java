package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Mod(
        modid = "mcheliloader",
        name = "mcheliloader",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloader {
    private File minecraftDir;
    private static final Logger LOGGER = LogManager.getLogger(mcheliloader.class.getName());

    // Resource folder in the jar that holds the McheliO files.
    // Note: This folder must be packaged inside your loader jar as /mchelio-new-vehicles.
    private static final String RESOURCE_FOLDER_VEHICLES = "/mchelio-new-vehicles";

    // After extraction, the folder will initially be called "mchelio-new-vehicles"
    // and then will be moved/renamed to "mchelio" in the mods folder.
    private static final String EXTRACTED_FOLDER_VEHICLES = "mchelio-new-vehicles";
    private static final String VEHICLES_FOLDER_NAME = "mchelio";

    // Flag file to mark that installation has been performed.
    private static final String INSTALL_FLAG_FILENAME = "mchelio_installed.flag";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        minecraftDir = event.getModConfigurationDirectory().getParentFile();
        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");
        Path installFlag = modsDir.resolve(INSTALL_FLAG_FILENAME);

        // If the installation flag exists, skip installation.
        if (Files.exists(installFlag)) {
            LOGGER.info("McheliO installation already completed. Skipping installation.");
            return;
        }

        // Inform the user that a Nuclear Tech Mod is required for McheliO to function.
        JOptionPane.showMessageDialog(null,
                "McheliO requires a compatible Nuclear Tech Mod to function properly.\n" +
                        "Please ensure that you have a Nuclear Tech Mod (e.g., RTM) installed.",
                "Missing Nuclear Tech Mod",
                JOptionPane.WARNING_MESSAGE);

        // Set a custom font for the dialogs.
        setCustomFont();

        // Create a minimal always-on-top frame to display installation messages.
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setSize(1, 1);
        frame.setLocationRelativeTo(null);

        JOptionPane.showMessageDialog(frame,
                "Please do not close the Forge application.\n" +
                        "McheliO is installing. This may take a moment.",
                "Installing McheliO",
                JOptionPane.INFORMATION_MESSAGE);

        try {
            // Copy the resource folder from inside the jar to the mods directory.
            copyResourceFolder(RESOURCE_FOLDER_VEHICLES, modsDir);

            // Rename/move the extracted folder to the final folder name "mchelio".
            Path extractedFolderVehicles = modsDir.resolve(EXTRACTED_FOLDER_VEHICLES);
            if (Files.exists(extractedFolderVehicles)) {
                Path targetFolder = modsDir.resolve(VEHICLES_FOLDER_NAME);
                Files.move(extractedFolderVehicles, targetFolder, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("McheliO folder installed successfully into mods directory as '" + VEHICLES_FOLDER_NAME + "'.");
            } else {
                LOGGER.error("Resource folder '" + EXTRACTED_FOLDER_VEHICLES + "' not found after copying.");
            }

            // Create a flag file so that installation does not run again.
            Files.createFile(installFlag);
        } catch (IOException e) {
            LOGGER.error("Failed to install the McheliO folder into the mods directory.", e);
        }

        JOptionPane.showMessageDialog(frame,
                "McheliO installation is complete.\nPlease restart your instance.",
                "Installation Complete",
                JOptionPane.INFORMATION_MESSAGE);

        // Optionally exit the application if desired.
        System.exit(0);
    }

    /**
     * Copies a folder resource (and its subdirectories) from inside the jar to a destination directory.
     *
     * @param resourceFolder The folder path inside the jar, starting with '/'.
     * @param destDir        The destination directory.
     * @throws IOException if an I/O error occurs.
     */
    private void copyResourceFolder(String resourceFolder, Path destDir) throws IOException {
        // Locate the jar file containing this class.
        String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        jarPath = URLDecoder.decode(jarPath, "UTF-8");

        try (JarFile jar = new JarFile(jarPath)) {
            // Remove the leading "/" from the resourceFolder for matching JarEntry names.
            String resourceFolderPath = resourceFolder.startsWith("/") ? resourceFolder.substring(1) : resourceFolder;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                // Only process entries that start with the desired folder.
                if (entryName.startsWith(resourceFolderPath)) {
                    // Compute the relative path for the entry.
                    String relativePath = entryName.substring(resourceFolderPath.length());
                    // Construct the output path.
                    Path outPath = destDir.resolve(resourceFolderPath + relativePath);
                    if (entry.isDirectory()) {
                        Files.createDirectories(outPath);
                    } else {
                        Files.createDirectories(outPath.getParent());
                        try (InputStream in = jar.getInputStream(entry)) {
                            Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }
        }
    }

    private void setCustomFont() {
        Font customFont = new Font("Arial", Font.PLAIN, 18);
        UIManager.put("OptionPane.messageFont", customFont);
        UIManager.put("OptionPane.buttonFont", customFont);
    }
}
