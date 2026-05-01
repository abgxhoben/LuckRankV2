package de.pcblc.bungee.config;

import de.pcblc.common.config.ConfigAdapter;
import net.md_5.bungee.config.Configuration;

public final class BungeeConfigAdapter implements ConfigAdapter {

    private final Configuration root;

    public BungeeConfigAdapter(Configuration root) {
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
