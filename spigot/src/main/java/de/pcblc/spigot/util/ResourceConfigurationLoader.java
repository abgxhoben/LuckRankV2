package de.pcblc.spigot.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ResourceConfigurationLoader {

    private final JavaPlugin plugin;

    public ResourceConfigurationLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public FileConfiguration load(String fileName) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }

        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        return YamlConfiguration.loadConfiguration(file);
    }
}
