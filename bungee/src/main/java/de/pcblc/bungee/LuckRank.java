package de.pcblc.bungee;

import de.pcblc.bungee.command.RankCommand;
import de.pcblc.bungee.config.BungeeConfigAdapter;
import de.pcblc.bungee.listener.PlayerJoinListener;
import de.pcblc.bungee.service.NotificationService;
import de.pcblc.bungee.service.RankService;
import de.pcblc.bungee.service.UpdateCheckService;
import de.pcblc.bungee.service.WebhookService;
import de.pcblc.bungee.util.MessageService;
import de.pcblc.bungee.util.ResourceConfigurationLoader;
import de.pcblc.common.config.PluginConfiguration;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

import java.util.logging.Level;

public final class LuckRank extends Plugin {

    private NotificationService notificationService;
    private UpdateCheckService updateCheckService;
    private MessageService messageService;
    private ResourceConfigurationLoader configurationLoader;
    private RankCommand rankCommand;
    private PlayerJoinListener playerJoinListener;

    @Override
    public void onEnable() {
        try {
            configurationLoader = new ResourceConfigurationLoader(this);
            loadRuntimeState();
            getLogger().info("LuckRank enabled successfully.");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "LuckRank could not be enabled.", exception);
        }
    }

    @Override
    public void onDisable() {
        if (notificationService != null) {
            notificationService.shutdown();
        }
        if (rankCommand != null) {
            getProxy().getPluginManager().unregisterCommand(rankCommand);
        }
        if (playerJoinListener != null) {
            getProxy().getPluginManager().unregisterListener(playerJoinListener);
        }
    }

    public boolean reloadPluginState() {
        try {
            loadRuntimeState();
            return true;
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "LuckRank could not be reloaded.", exception);
            return false;
        }
    }

    public MessageService getMessageService() {
        return messageService;
    }

    private void loadRuntimeState() {
        Configuration config = configurationLoader.load("config.yml");
        Configuration messages = configurationLoader.load("messages.yml");

        PluginConfiguration pluginConfiguration = new PluginConfiguration(new BungeeConfigAdapter(config));
        if (!pluginConfiguration.isEnabled()) {
            throw new IllegalStateException("LuckRank is disabled in config.yml.");
        }

        MessageService loadedMessageService = new MessageService(messages, config.getString("prefix", "&2Luck&aRank &8>> &7"));
        LuckPerms luckPerms = LuckPermsProvider.get();

        NotificationService loadedNotificationService = new NotificationService(this, pluginConfiguration);
        loadedNotificationService.initialize();

        UpdateCheckService loadedUpdateCheckService = new UpdateCheckService(this, pluginConfiguration, loadedMessageService);
        RankService loadedRankService = new RankService(this, luckPerms);
        WebhookService loadedWebhookService = new WebhookService(this, pluginConfiguration);
        RankCommand loadedRankCommand = new RankCommand(
                this,
                loadedRankService,
                loadedNotificationService,
                loadedUpdateCheckService,
                loadedWebhookService,
                loadedMessageService
        );
        PlayerJoinListener loadedPlayerJoinListener = new PlayerJoinListener(
                loadedNotificationService,
                loadedUpdateCheckService
        );

        /*
         * Prepare the replacement runtime before closing the current one. The
         * old registrations are retained until the new registrations succeed,
         * so a failed reload can be rolled back without a dead database handle.
         */
        boolean commandRegistered = false;
        boolean listenerRegistered = false;
        try {
            getProxy().getPluginManager().registerCommand(this, loadedRankCommand);
            commandRegistered = true;
            getProxy().getPluginManager().registerListener(this, loadedPlayerJoinListener);
            listenerRegistered = true;
        } catch (RuntimeException exception) {
            if (listenerRegistered) {
                getProxy().getPluginManager().unregisterListener(loadedPlayerJoinListener);
            }
            if (commandRegistered) {
                getProxy().getPluginManager().unregisterCommand(loadedRankCommand);
            }
            if (rankCommand != null) {
                getProxy().getPluginManager().registerCommand(this, rankCommand);
            }
            if (playerJoinListener != null) {
                getProxy().getPluginManager().registerListener(this, playerJoinListener);
            }
            loadedNotificationService.shutdown();
            throw exception;
        }

        NotificationService oldNotificationService = notificationService;
        RankCommand oldRankCommand = rankCommand;
        PlayerJoinListener oldPlayerJoinListener = playerJoinListener;
        if (oldRankCommand != null) {
            getProxy().getPluginManager().unregisterCommand(oldRankCommand);
        }
        if (oldPlayerJoinListener != null) {
            getProxy().getPluginManager().unregisterListener(oldPlayerJoinListener);
        }

        notificationService = loadedNotificationService;
        updateCheckService = loadedUpdateCheckService;
        messageService = loadedMessageService;
        rankCommand = loadedRankCommand;
        playerJoinListener = loadedPlayerJoinListener;

        if (oldNotificationService != null) {
            oldNotificationService.shutdown();
        }
        updateCheckService.checkForUpdates(true, false);
    }
}
