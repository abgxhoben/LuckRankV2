package de.pcblc.bungee.manager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.event.EventHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class VersionChecker implements Listener {

    private final Plugin plugin;
    private final int resourceId;
    private final Configuration config;
    private final Configuration messagesConfig;
    private final MySQLManager mySQLManager;
    private final boolean updateChecked = false;

    public VersionChecker(Plugin plugin, int resourceId, Configuration config, Configuration messagesConfig, MySQLManager mySQLManager) {
        this.plugin = plugin;
        this.resourceId = resourceId;
        this.config = config;
        this.messagesConfig = messagesConfig;
        this.mySQLManager = mySQLManager;
        checkForUpdates(true);  // Call this method at plugin startup
    }

    public void checkForUpdates(boolean logToConsoleOnly) {
        if (!config.getBoolean("updateNotificationsEnabled", true)) {
            return;
        }

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String latestVersion = in.readLine();

                    if (latestVersion != null && !latestVersion.isEmpty() && !plugin.getDescription().getVersion().equalsIgnoreCase(latestVersion)) {
                        String message = messagesConfig.getString("update.available")
                                .replace("{version}", latestVersion)
                                .replace("{resourceId}", String.valueOf(resourceId));
                        plugin.getLogger().log(Level.INFO, message);
                        if (!logToConsoleOnly) {
                            notifyPlayers(ChatColor.translateAlternateColorCodes('&', message));
                        }
                    } else {
                        plugin.getLogger().log(Level.INFO, "You are using the latest version of the plugin.");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private void notifyPlayers(String message) {
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.hasPermission("luckrank.see")) {
                    player.sendMessage(message);
                }
            }
        });
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        mySQLManager.setNotifyStatus(playerUUID, true);
        if (player.hasPermission("luckrank.see")) {
            checkForUpdates(false);
        }
    }

    public boolean isOutdated() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String latestVersion = in.readLine();

                if (latestVersion != null && !latestVersion.isEmpty() && !plugin.getDescription().getVersion().equalsIgnoreCase(latestVersion)) {
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check for updates: " + e.getMessage());
        }
        return false;
    }

}
