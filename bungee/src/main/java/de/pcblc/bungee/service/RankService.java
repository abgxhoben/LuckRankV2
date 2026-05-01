package de.pcblc.bungee.service;

import de.pcblc.bungee.LuckRank;
import de.pcblc.common.rank.BaseRankService;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class RankService extends BaseRankService<ProxiedPlayer> {

    private final LuckRank plugin;

    public RankService(LuckRank plugin, LuckPerms luckPerms) {
        super(luckPerms);
        this.plugin = plugin;
    }

    @Override
    protected boolean hasPermission(ProxiedPlayer actor, String permission) {
        return actor.hasPermission(permission);
    }

    @Override
    protected UUID getUniqueId(ProxiedPlayer player) {
        return player.getUniqueId();
    }

    @Override
    protected ProxiedPlayer findOnlinePlayer(String playerName) {
        return plugin.getProxy().getPlayer(playerName);
    }

    @Override
    protected boolean isOnline(ProxiedPlayer player) {
        return player.isConnected();
    }

    @Override
    protected String getTargetServer(ProxiedPlayer player) {
        return player.getServer() == null ? "Unknown" : player.getServer().getInfo().getName();
    }

    @Override
    protected String getActorServer(ProxiedPlayer actor) {
        return getTargetServer(actor);
    }

    @Override
    protected void scheduleConfirmationExpiry(UUID actorId) {
        plugin.getProxy().getScheduler().schedule(plugin, () -> clearPendingRemoval(actorId), 30L, TimeUnit.SECONDS);
    }

    @Override
    protected void logWarning(String message, Exception exception) {
        plugin.getLogger().log(Level.WARNING, message, exception);
    }
}
