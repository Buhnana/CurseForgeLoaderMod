package com.Ragex.mcheliloader;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

@Mod(
        modid = "mcheliloader",
        name = "mcheliloader",
        dependencies = "required-after:Forge@[10.13.2.1230,)"
)
public class mcheliloader {
    private File minecraftDir;
    private static final Logger LOGGER = LogManager.getLogger(mcheliloader.class.getName());

    // Name of the source folder to be moved (it is assumed to already exist).
    private static final String SOURCE_FOLDER_NAME = "Mchelio";
    // Name of the destination folder in the mods directory.
    private static final String VEHICLES_FOLDER_NAME = "mchelio";

    // This boolean is only in-memory for this run.
    private static boolean moved = false;

    // Name of a flag file to persist that the folder has been moved.
    private static final String FLAG_FILE_NAME = "mchelilo_moved.flag";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        minecraftDir = event.getModConfigurationDirectory().getParentFile();

        // Check if the flag file exists (i.e. moving has already been done)
        File flagFile = new File(minecraftDir, FLAG_FILE_NAME);
        if (flagFile.exists()) {
            LOGGER.info("Mchelio folder already moved. Skipping moving process.");
            return; // Do nothing if already moved.
        }

        Path modsDir = Paths.get(minecraftDir.getPath(), "mods");

        // Set custom font size for JOptionPane dialogs.
        setCustomFont();

        // Create a tiny, always-on-top frame for JOptionPane dialogs.
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setSize(1, 1);
        frame.setLocationRelativeTo(null);

        // Inform the user that the folder move is starting.
        JOptionPane.showMessageDialog(frame, "Please do not close the Forge application. Mchelio is being moved and this might take a moment.",
                "Moving Folder", JOptionPane.INFORMATION_MESSAGE);

        try {
            // Define the source folder location.
            // This example assumes the folder "Mchelio" exists in the Minecraft directory.
            Path sourceFolder = Paths.get(minecraftDir.getPath(), SOURCE_FOLDER_NAME);

            if (Files.exists(sourceFolder) && Files.isDirectory(sourceFolder)) {
                // Define the target folder in the mods directory.
                Path targetFolder = modsDir.resolve(VEHICLES_FOLDER_NAME);
                Files.move(sourceFolder, targetFolder, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("Moved the Mchelio folder to the mods folder.");
            } else {
                LOGGER.error("Source folder '" + SOURCE_FOLDER_NAME + "' does not exist in the Minecraft directory. Skipping Mchelio moving process.");
            }

            // Create the flag file to persist that the folder has been moved.
            if (!flagFile.createNewFile()) {
                LOGGER.error("Failed to create flag file; moving might run again next launch.");
            }
            moved = true;

            // Inform the user that a nuclear tech mod is required.
            JOptionPane.showMessageDialog(frame,
                    "McheliO requires a nuclear tech mod for proper functionality. Please install a compatible nuclear tech mod.",
                    "Missing Dependency", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            LOGGER.error("Failed to move the folder.", e);
        }

        // Deliberately crash the game to force a restart.
        throw new RuntimeException("Intentional crash from loader mod.");
    }

    private void setCustomFont() {
        // Set a custom font for JOptionPane dialogs.
        Font customFont = new Font("Arial", Font.PLAIN, 18);
        UIManager.put("OptionPane.messageFont", customFont);
        UIManager.put("OptionPane.buttonFont", customFont);
    }
}
