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

        if (notificationService != null) {
            notificationService.shutdown();
        }

        NotificationService loadedNotificationService = new NotificationService(this, pluginConfiguration);
        loadedNotificationService.initialize();

        UpdateCheckService loadedUpdateCheckService = new UpdateCheckService(this, pluginConfiguration, loadedMessageService);
        RankService rankService = new RankService(this, luckPerms);
        WebhookService webhookService = new WebhookService(this, pluginConfiguration);

        getProxy().getPluginManager().unregisterCommands(this);
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().getPluginManager().registerCommand(
                this,
                new RankCommand(this, rankService, loadedNotificationService, loadedUpdateCheckService, webhookService, loadedMessageService)
        );
        getProxy().getPluginManager().registerListener(
                this,
                new PlayerJoinListener(loadedNotificationService, loadedUpdateCheckService)
        );

        notificationService = loadedNotificationService;
        updateCheckService = loadedUpdateCheckService;
        messageService = loadedMessageService;

        updateCheckService.checkForUpdates(true, false);
    }
}
