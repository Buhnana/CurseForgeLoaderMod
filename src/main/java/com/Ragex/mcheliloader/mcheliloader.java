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

    // Names of the resource folders in the jar:
    private static final String RESOURCE_FOLDER_DW = "/DWbout-it-1";
    private static final String RESOURCE_FOLDER_VEHICLES = "/mchelio-new-vehicles";

    // These are the names after copying into the mods folder:
    private static final String EXTRACTED_FOLDER_DW = "DWbout-it-1";
    private static final String EXTRACTED_FOLDER_VEHICLES = "mchelio-new-vehicles";
    private static final String VEHICLES_FOLDER_NAME = "mchelio";

    // Flag file to mark that installation has been performed
    private static final String INSTALL_FLAG_FILENAME = "mchelio_installed.flag";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        minecraftDir = event.getModConfigurationDirectory().getParentFile();
        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");
        Path installFlag = modsDir.resolve(INSTALL_FLAG_FILENAME);

        // If the flag exists, skip the extraction
        if (Files.exists(installFlag)) {
            LOGGER.info("Installation already completed. Skipping extraction.");
            return;
        }

        // Set custom font size for JOptionPane
        setCustomFont();

        // Show "Don't close" message
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setSize(1, 1);
        frame.setLocationRelativeTo(null);

        JOptionPane.showMessageDialog(frame,
                "Please do not close the forge application. McheliO is extracting and will take longer than normal.",
                "Extracting",
                JOptionPane.INFORMATION_MESSAGE);

        try {
            // Instead of unzipping zip files, copy the resource folders from within this jar
            copyResourceFolder(RESOURCE_FOLDER_DW, modsDir);
            copyResourceFolder(RESOURCE_FOLDER_VEHICLES, modsDir);

            Path extractedFolderDW = modsDir.resolve(EXTRACTED_FOLDER_DW);
            Path extractedFolderVehicles = modsDir.resolve(EXTRACTED_FOLDER_VEHICLES);

            // Handle the HBM extraction from the DW folder if it exists
            if (Files.exists(extractedFolderDW)) {
                handleHBMExtraction(extractedFolderDW, modsDir);
                deleteFolderRecursively(extractedFolderDW);
            } else {
                LOGGER.error("Extracted folder 'DWbout-it-1' does not exist. Skipping HBM handling.");
            }

            // Handle the vehicles extraction: move the vehicles folder to its final name
            if (Files.exists(extractedFolderVehicles)) {
                Path targetFolder = modsDir.resolve(VEHICLES_FOLDER_NAME);
                Files.move(extractedFolderVehicles, targetFolder, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Copied and moved the mchelio vehicles files to the mods folder.");
            } else {
                LOGGER.error("Extracted folder 'mchelio-new-vehicles' does not exist. Skipping Mchelio handling.");
            }

            // Create the flag file so this extraction does not run again
            markInstalled(installFlag);

            // Show success message
            JOptionPane.showMessageDialog(frame,
                    "McheliO was successfully extracted. Please restart your instance.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            LOGGER.error("Failed to extract or move the files.", e);
        }

        // Terminate the application (or let it continue if that suits your design)
        System.exit(0);
    }

    /**
     * Copies a folder resource (and its sub-resources) from inside the jar to a destination directory.
     *
     * @param resourceFolder The resource folder path inside the jar (should start with a '/')
     * @param destDir        The destination directory as a Path
     * @throws IOException if an IO error occurs.
     */
    private void copyResourceFolder(String resourceFolder, Path destDir) throws IOException {
        // Determine the path to the current jar file.
        String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        jarPath = URLDecoder.decode(jarPath, "UTF-8");

        try (JarFile jar = new JarFile(jarPath)) {
            // Remove the leading "/" from resourceFolder for matching JarEntry names
            String resourceFolderPath = resourceFolder.startsWith("/") ? resourceFolder.substring(1) : resourceFolder;
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();
                // Only process entries that start with the desired folder name
                if (entryName.startsWith(resourceFolderPath)) {
                    // Get the relative path (e.g. if resourceFolderPath is "DWbout-it-1", then remove that prefix)
                    String relativePath = entryName.substring(resourceFolderPath.length());
                    // Construct the output path
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

    private void handleHBMExtraction(Path extractedFolder, Path modsDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extractedFolder, "*.txt")) {
            for (Path entry : stream) {
                if (entry.getFileName().toString().contains("RTM")) {
                    // Dynamically set the MOD_FILE_NAME based on the TXT file name
                    String modFileName = entry.getFileName().toString().replace(".txt", ".jar");

                    // Move the TXT file to the mods folder and rename it to .jar
                    Path jarFilePath = modsDir.resolve(modFileName);
                    Files.move(entry, jarFilePath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Moved and renamed the Nuclear Tech TXT file to JAR.");
                    break;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to find or move the RTM TXT file.", e);
        }
    }

    private void deleteFolderRecursively(Path folder) throws IOException {
        if (!Files.exists(folder)) return;
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

    private void setCustomFont() {
        // Set a custom font for JOptionPane dialogs
        Font customFont = new Font("Arial", Font.PLAIN, 18);
        UIManager.put("OptionPane.messageFont", customFont);
        UIManager.put("OptionPane.buttonFont", customFont);
    }

    /**
     * Marks the installation as complete by creating a flag file.
     *
     * @param flagPath The path to the flag file.
     * @throws IOException if an IO error occurs.
     */
    private void markInstalled(Path flagPath) throws IOException {
        Files.createFile(flagPath);
        LOGGER.info("Installation flag created at: " + flagPath);
    }
}