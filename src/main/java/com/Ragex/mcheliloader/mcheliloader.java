package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
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

    private static final String EXTRACTED_FOLDER_VEHICLES = "mchelio-new-vehicles";
    private static final String VEHICLES_FOLDER_NAME = "mchelio";
    // This boolean is only in-memory for this run.
    private static boolean extracted = false;

    // Name of a flag file to persist that extraction has run.
    private static final String FLAG_FILE_NAME = "mchelilo_extracted.flag";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        minecraftDir = event.getModConfigurationDirectory().getParentFile();

        // Check if the flag file exists (i.e. extraction has already been done)
        File flagFile = new File(minecraftDir, FLAG_FILE_NAME);
        if (flagFile.exists() && Files.exists(Paths.get(minecraftDir.getPath(), "mods", VEHICLES_FOLDER_NAME))) { //old logic, || Files.exists(Paths.get(minecraftDir.getPath(), "mods", VEHICLES_FOLDER_NAME))
            LOGGER.info("McheliO already extracted. Skipping extraction process.");
            return; // Do nothing if already extracted.
        }

        if (flagFile.exists() && Files.exists(Paths.get(minecraftDir.getPath(), "mods", EXTRACTED_FOLDER_VEHICLES))) { //old logic, || Files.exists(Paths.get(minecraftDir.getPath(), "mods", VEHICLES_FOLDER_NAME))
            LOGGER.info("McheliO already extracted. Albeit weirdly, skipping extraction process.");
            return; // Do nothing if already extracted.
        }

        Path vehiclesFolder = Paths.get(minecraftDir.getPath(), "mods", VEHICLES_FOLDER_NAME);
        if (Files.exists(vehiclesFolder)) {
            try {
                Files.walk(vehiclesFolder)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                LOGGER.info("Deleted existing McheliO directory to prevent extraction issues.");
            } catch (IOException e) {
                LOGGER.error("Failed to delete existing McheliO directory.", e);
            }
        }

        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");

        // Set custom font size for JOptionPane
        setCustomFont();

        // Create a tiny, always-on-top frame for JOptionPane dialogs.
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setSize(1, 1);
        frame.setLocationRelativeTo(null);

        // Inform the user that extraction is starting.
        JOptionPane.showMessageDialog(frame, "Please do not close the Forge application. McheliO is extracting and will take longer than normal.",
                "Extracting", JOptionPane.INFORMATION_MESSAGE);

        try {
            // Uncomment or adjust if you need to extract another resource.
            // unzipResourceToDirectory("/ntm.zip", modsDir.toString());
            unzipResourceToDirectory("/mchelio.zip", modsDir.toString());

            Path extractedFolderVehicles = Paths.get(modsDir.toString(), EXTRACTED_FOLDER_VEHICLES);

            // Handle Mchelio extraction: move the folder if it exists.
            if (Files.exists(extractedFolderVehicles)) {
                Path targetFolder = modsDir.resolve(VEHICLES_FOLDER_NAME);
                Files.move(extractedFolderVehicles, targetFolder, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Unzipped and moved the mchelio files to mods folder.");
            } else {
                LOGGER.error("Extracted folder '" + EXTRACTED_FOLDER_VEHICLES + "' does not exist. Skipping Mchelio handling.");
            }

            // Create the flag file and set the boolean to true so that this code never runs again.
            if (!flagFile.createNewFile()) {
                LOGGER.error("Failed to create flag file; extraction might run again next launch.");
            }
            extracted = true;

            // Notify user about nuclear tech mod requirement
            // Notify user about nuclear tech and XenoFactions dependencies
            JOptionPane.showMessageDialog(frame,
                    "McheliO requires a nuclear tech mod for nukes.\nRecommended: RTM (full support).\nJamesH2 fork: not recommended.\nOriginal HBM: experimental.\nSome recipes may also require XenoFactions.\nIf already installed, restart your game.",
                    "McheliO Installed - Restart Required", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            LOGGER.error("Failed to extract or move the files.", e);
        }

        // Deliberately crash the game to force a restart.
        throw new RuntimeException("Intentional crash from loader mod.");
    }

    private void unzipResourceToDirectory(String resourcePath, String destDir) throws IOException {
        try (InputStream zipStream = getClass().getResourceAsStream(resourcePath)) {
            if (zipStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            Path tempZipFile = Files.createTempFile("tempZip", ".zip");
            Files.copy(zipStream, tempZipFile, StandardCopyOption.REPLACE_EXISTING);
            unzipFile(tempZipFile.toString(), destDir);
            Files.delete(tempZipFile);
        }
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
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private void setCustomFont() {
        // Set a custom font for JOptionPane dialogs.
        Font customFont = new Font("Arial", Font.PLAIN, 18);
        UIManager.put("OptionPane.messageFont", customFont);
        UIManager.put("OptionPane.buttonFont", customFont);
    }
}
