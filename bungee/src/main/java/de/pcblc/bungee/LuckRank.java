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

    @Override
    public void onEnable() {
        try {
            ResourceConfigurationLoader configurationLoader = new ResourceConfigurationLoader(this);
            Configuration config = configurationLoader.load("config.yml");
            Configuration messages = configurationLoader.load("messages.yml");

            PluginConfiguration pluginConfiguration = new PluginConfiguration(new BungeeConfigAdapter(config));
            if (!pluginConfiguration.isEnabled()) {
                getLogger().warning("LuckRank is disabled in config.yml.");
                return;
            }

            MessageService messageService = new MessageService(messages, config.getString("prefix", "&2Luck&aRank &8>> &7"));
            LuckPerms luckPerms = LuckPermsProvider.get();

            this.notificationService = new NotificationService(this, pluginConfiguration);
            notificationService.initialize();

            UpdateCheckService updateCheckService = new UpdateCheckService(this, pluginConfiguration, messageService);
            RankService rankService = new RankService(this, luckPerms);
            WebhookService webhookService = new WebhookService(this, pluginConfiguration);

            getProxy().getPluginManager().registerCommand(
                    this,
                    new RankCommand(this, rankService, notificationService, updateCheckService, webhookService, messageService)
            );
            getProxy().getPluginManager().registerListener(
                    this,
                    new PlayerJoinListener(notificationService, updateCheckService)
            );

            updateCheckService.checkForUpdates(true, false);
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
}
