package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cpw.mods.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
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
    private static final String FLAG_FILE_NAME = "mchelilo_extracted.flag";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        minecraftDir = event.getModConfigurationDirectory().getParentFile();
        File flagFile = new File(minecraftDir, FLAG_FILE_NAME);

        if (flagFile.exists() && Files.exists(Paths.get(minecraftDir.getPath(), "mods", VEHICLES_FOLDER_NAME))) {
            LOGGER.info("McheliO already extracted. Skipping extraction process.");
            return;
        }

        if (flagFile.exists() && Files.exists(Paths.get(minecraftDir.getPath(), "mods", EXTRACTED_FOLDER_VEHICLES))) {
            LOGGER.info("McheliO already extracted. Albeit weirdly, skipping extraction process.");
            return;
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

        // Only run GUI notifications on client
        if (isClient()) {
            runClientStartDialog();
        } else {
            LOGGER.info("Starting McheliO extraction on server. This may take a while.");
        }

        try {
            unzipResourceToDirectory("/mchelio.zip", modsDir.toString());

            Path extractedFolderVehicles = Paths.get(modsDir.toString(), EXTRACTED_FOLDER_VEHICLES);
            if (Files.exists(extractedFolderVehicles)) {
                Path targetFolder = modsDir.resolve(VEHICLES_FOLDER_NAME);
                Files.move(extractedFolderVehicles, targetFolder, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Unzipped and moved the mchelio files to mods folder.");
            } else {
                LOGGER.error("Extracted folder '" + EXTRACTED_FOLDER_VEHICLES + "' does not exist. Skipping Mchelio handling.");
            }

            if (!flagFile.createNewFile()) {
                LOGGER.error("Failed to create flag file; extraction might run again next launch.");
            }

            if (isClient()) {
                runClientEndDialog();
            } else {
                LOGGER.info("McheliO installed on server. Restart recommended.");
            }

        } catch (IOException e) {
            LOGGER.error("Failed to extract or move the files.", e);
        }

        if (isClient()) {
            throw new RuntimeException("Intentional crash from loader mod.");
        } else {
            LOGGER.warn("Extraction complete on server. Manual restart required.");
        }
    }

    private boolean isClient() {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT;
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

                try {
                    String name = entry.getName();
                    LOGGER.info("Processing zip entry: " + name);
                } catch (Exception e) {
                    LOGGER.error("Malformed zip entry name detected!", e);
                    continue;
                }

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

    @SideOnly(Side.CLIENT)
    private void runClientStartDialog() {
        setCustomFont();
        javax.swing.JFrame frame = new javax.swing.JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setSize(1, 1);
        frame.setLocationRelativeTo(null);

        JOptionPane.showMessageDialog(frame,
                "Please do not close the Forge application. McheliO is extracting and will take longer than normal.",
                "Extracting", JOptionPane.INFORMATION_MESSAGE);
    }

    @SideOnly(Side.CLIENT)
    private void runClientEndDialog() {
        javax.swing.JFrame frame = new javax.swing.JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setSize(1, 1);
        frame.setLocationRelativeTo(null);

        JOptionPane.showMessageDialog(frame,
                "McheliO requires a nuclear tech mod for nukes.\nRecommended: RTM (full support).\nJamesH2 fork: not recommended.\nOriginal HBM: experimental.\nSome recipes may also require XenoFactions.\nIf already installed, restart your game.",
                "McheliO Installed - Restart Required", JOptionPane.INFORMATION_MESSAGE);
    }

    @SideOnly(Side.CLIENT)
    private void setCustomFont() {
        Font customFont = new Font("Arial", Font.PLAIN, 18);
        UIManager.put("OptionPane.messageFont", customFont);
        UIManager.put("OptionPane.buttonFont", customFont);
    }
}