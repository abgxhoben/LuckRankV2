package de.pcblc.common.config;

public interface ConfigAdapter {

    boolean getBoolean(String path, boolean fallback);

    int getInt(String path, int fallback);

    String getString(String path, String fallback);
}
