package de.pcblc.common.config;

public final class PluginConfiguration {

    private static final int UPDATE_RESOURCE_ID = 113728;

    private final ConfigAdapter root;

    public PluginConfiguration(ConfigAdapter root) {
        this.root = root;
    }

    public boolean isEnabled() {
        return root.getBoolean("enabled", true);
    }

    public boolean isUpdateNotificationsEnabled() {
        return root.getBoolean("updateNotificationsEnabled", true);
    }

    public int getUpdateResourceId() {
        return UPDATE_RESOURCE_ID;
    }

    public String getDatabaseType() {
        return root.getString("database.type", "sqlite");
    }

    public String getDatabaseHost() {
        return root.getString("database.host", "");
    }

    public int getDatabasePort() {
        return root.getInt("database.port", 3306);
    }

    public String getDatabaseName() {
        return root.getString("database.database", "");
    }

    public String getDatabaseUser() {
        return root.getString("database.user", "");
    }

    public String getDatabasePassword() {
        return root.getString("database.password", "");
    }

    public boolean isWebhookEnabled() {
        return root.getBoolean("webhook.enabled", false);
    }

    public String getWebhookUrl() {
        return root.getString("webhook.url", "");
    }

    public String getWebhookValue(String path, String fallback) {
        return root.getString("webhook." + path, fallback);
    }

    public int getWebhookColor(String path, int fallback) {
        return root.getInt("webhook." + path, fallback);
    }
}
