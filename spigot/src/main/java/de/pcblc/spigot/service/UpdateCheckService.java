package de.pcblc.spigot.service;

import de.pcblc.common.config.PluginConfiguration;
import de.pcblc.common.update.SpigotUpdateClient;
import de.pcblc.spigot.LuckRankSpigot;
import de.pcblc.spigot.util.MessageService;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public final class UpdateCheckService {

    private final LuckRankSpigot plugin;
    private final PluginConfiguration configuration;
    private final MessageService messageService;
    private final SpigotUpdateClient updateClient = new SpigotUpdateClient();
    private volatile boolean outdated;
    private volatile String latestVersion = "";

    public UpdateCheckService(LuckRankSpigot plugin, PluginConfiguration configuration, MessageService messageService) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.messageService = messageService;
    }

    public void checkForUpdates(boolean consoleOnly, boolean notifyOnlineWatchers) {
        if (!configuration.isUpdateNotificationsEnabled()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                latestVersion = updateClient.fetchLatestVersion(configuration.getUpdateResourceId());
                outdated = latestVersion != null
                        && !latestVersion.trim().isEmpty()
                        && !plugin.getDescription().getVersion().equalsIgnoreCase(latestVersion.trim());

                if (outdated) {
                    plugin.getLogger().info(messageService.stripColor(messageService.raw(
                            "update.available",
                            "version", latestVersion,
                            "resourceId", String.valueOf(configuration.getUpdateResourceId())
                    )));
                } else if (!consoleOnly) {
                    plugin.getLogger().info("LuckRank is up to date.");
                }

                if (outdated && notifyOnlineWatchers) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                            notifyPlayerIfOutdated(player);
                        }
                    });
                }
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to check for updates.", exception);
            }
        });
    }

    public void notifyPlayerIfOutdated(Player player) {
        if (!outdated || !player.hasPermission("luckrank.see")) {
            return;
        }

        player.sendMessage(messageService.raw(
                "update.available",
                "version", latestVersion,
                "resourceId", String.valueOf(configuration.getUpdateResourceId())
        ));
    }

    public boolean isOutdated() {
        return outdated;
    }
}
