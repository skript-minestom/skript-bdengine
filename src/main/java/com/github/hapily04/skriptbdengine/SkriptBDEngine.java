package com.github.hapily04.skriptbdengine;


import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import com.github.hapily04.skriptbdengine.api.ModelManager;
import com.github.hapily04.skriptbdengine.registration.Registration;
import com.github.hapily04.skriptminestom.util.FileUtils;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SkriptBDEngine extends JavaPlugin {

    private static SkriptBDEngine instance;

    private final Logger logger = LoggerFactory.getLogger(SkriptBDEngine.class);
    private File functionsFolder;
    private File convertedFolder;
    private SkriptAddon addonInstance;
    private ModelManager modelManager;

    @Override
    public void onEnable() {
        File dataFolder = FileUtils.defendFile(new File(FileUtils.getServerDirectory(), "skript-bdengine"), true);
        functionsFolder = FileUtils.defendFile(new File(dataFolder, "functions"), true);
        convertedFolder = FileUtils.defendFile(new File(dataFolder, "converted"), true);
        modelManager = new ModelManager(convertedFolder);
        instance = this;

        addonInstance = Skript.registerAddon(this);
        new Registration(); // register types, functions, converters, etc.
        try {
            addonInstance.loadClasses("com.github.hapily04.skriptbdengine", "elements");
        } catch (IOException e) {
            logger.error("An error occurred whilst loading skript-bdengine's elements: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public File getFunctionsFolder() {
        return functionsFolder;
    }

    public File getConvertedFolder() {
        return convertedFolder;
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    public Logger logger() {
        return logger;
    }

    public static SkriptBDEngine getInstance() {
        return instance;
    }

}
