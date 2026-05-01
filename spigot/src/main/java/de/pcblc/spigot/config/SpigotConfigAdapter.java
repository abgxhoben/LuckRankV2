package de.pcblc.spigot.config;

import de.pcblc.common.config.ConfigAdapter;
import org.bukkit.configuration.file.FileConfiguration;

public final class SpigotConfigAdapter implements ConfigAdapter {

    private final FileConfiguration root;

    public SpigotConfigAdapter(FileConfiguration root) {
        this.root = root;
    }

    @Override
    public boolean getBoolean(String path, boolean fallback) {
        return root.getBoolean(path, fallback);
    }

    @Override
    public int getInt(String path, int fallback) {
        return root.getInt(path, fallback);
    }

    @Override
    public String getString(String path, String fallback) {
        return root.getString(path, fallback);
    }
}
