package de.pcblc.bungee.util;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class ResourceConfigurationLoader {

    private final Plugin plugin;

    public ResourceConfigurationLoader(Plugin plugin) {
        this.plugin = plugin;
    }

    public Configuration load(String fileName) {
        ensureDataFolder();
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            copyDefaultFile(fileName, file);
        }

        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load " + fileName, exception);
        }
    }

    private void ensureDataFolder() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder.");
        }
    }

    private void copyDefaultFile(String resourceName, File targetFile) {
        try (InputStream stream = plugin.getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource " + resourceName);
            }
            Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write " + resourceName, exception);
        }
    }
}
