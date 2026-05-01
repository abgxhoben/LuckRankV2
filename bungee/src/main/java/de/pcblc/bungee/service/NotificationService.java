package de.pcblc.bungee.service;

import de.pcblc.bungee.LuckRank;
import de.pcblc.common.config.PluginConfiguration;
import de.pcblc.common.notification.NotificationRepository;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public final class NotificationService {

    private final LuckRank plugin;
    private final NotificationRepository repository;

    public NotificationService(LuckRank plugin, PluginConfiguration configuration) {
        this.plugin = plugin;
        this.repository = new NotificationRepository(configuration, plugin.getDataFolder());
    }

    public void initialize() {
        try {
            repository.initialize();
            if (!repository.isAvailable()) {
                plugin.getLogger().warning("Notification storage is unavailable. Notifications will default to enabled.");
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not initialize notification storage.", exception);
        }
    }

    public void ensurePlayerExists(UUID playerId) {
        try {
            repository.ensurePlayerExists(playerId);
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not insert notification preference.", exception);
        }
    }

    public boolean toggle(UUID playerId) {
        boolean nextState = !isEnabled(playerId);
        setEnabled(playerId, nextState);
        return nextState;
    }

    public boolean isEnabled(UUID playerId) {
        try {
            return repository.getNotificationState(playerId);
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not read notification preference.", exception);
            return true;
        }
    }

    public void broadcastToWatchers(String message) {
        for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
            if (player.hasPermission("luckrank.see") && isEnabled(player.getUniqueId())) {
                player.sendMessage(TextComponent.fromLegacyText(message));
            }
        }
    }

    public void shutdown() {
        try {
            repository.close();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not close notification storage.", exception);
        }
    }

    private void setEnabled(UUID playerId, boolean enabled) {
        try {
            repository.setNotificationState(playerId, enabled);
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not update notification preference.", exception);
        }
    }
}
