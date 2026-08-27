package de.pcblc.spigot.service;

import de.pcblc.common.rank.BaseRankService;
import de.pcblc.spigot.LuckRankSpigot;
import net.luckperms.api.LuckPerms;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

public final class RankService extends BaseRankService<Player> {

    private final LuckRankSpigot plugin;

    public RankService(LuckRankSpigot plugin, LuckPerms luckPerms) {
        super(luckPerms);
        this.plugin = plugin;
    }

    @Override
    protected boolean hasPermission(Player actor, String permission) {
        return actor.hasPermission(permission);
    }

    @Override
    protected UUID getUniqueId(Player player) {
        return player.getUniqueId();
    }

    @Override
    protected Player findOnlinePlayer(String playerName) {
        return plugin.getServer().getPlayerExact(playerName);
    }

    @Override
    protected boolean isOnline(Player player) {
        return player.isOnline();
    }

    @Override
    protected String getTargetServer(Player player) {
        return plugin.getServer().getName();
    }

    @Override
    protected String getActorServer(Player actor) {
        return plugin.getServer().getName();
    }

    @Override
    protected void scheduleConfirmationExpiry(UUID actorId, String confirmationKey) {
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> clearPendingRemoval(actorId, confirmationKey),
                20L * 30L
        );
    }

    @Override
    protected void logWarning(String message, Exception exception) {
        plugin.getLogger().log(Level.WARNING, message, exception);
    }
}
