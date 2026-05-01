package de.pcblc.spigot.listener;

import de.pcblc.spigot.service.NotificationService;
import de.pcblc.spigot.service.UpdateCheckService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {

    private final NotificationService notificationService;
    private final UpdateCheckService updateCheckService;

    public PlayerJoinListener(NotificationService notificationService, UpdateCheckService updateCheckService) {
        this.notificationService = notificationService;
        this.updateCheckService = updateCheckService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        notificationService.ensurePlayerExists(event.getPlayer().getUniqueId());
        updateCheckService.notifyPlayerIfOutdated(event.getPlayer());
    }
}
