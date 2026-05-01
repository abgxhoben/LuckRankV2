package de.pcblc.bungee.listener;

import de.pcblc.bungee.service.NotificationService;
import de.pcblc.bungee.service.UpdateCheckService;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public final class PlayerJoinListener implements Listener {

    private final NotificationService notificationService;
    private final UpdateCheckService updateCheckService;

    public PlayerJoinListener(NotificationService notificationService, UpdateCheckService updateCheckService) {
        this.notificationService = notificationService;
        this.updateCheckService = updateCheckService;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        notificationService.ensurePlayerExists(event.getPlayer().getUniqueId());
        updateCheckService.notifyPlayerIfOutdated(event.getPlayer());
    }
}
