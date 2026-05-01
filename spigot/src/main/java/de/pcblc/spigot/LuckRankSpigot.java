package de.pcblc.spigot;

import de.pcblc.spigot.command.RankCommand;
import de.pcblc.spigot.config.SpigotConfigAdapter;
import de.pcblc.spigot.listener.PlayerJoinListener;
import de.pcblc.spigot.service.NotificationService;
import de.pcblc.spigot.service.RankService;
import de.pcblc.spigot.service.UpdateCheckService;
import de.pcblc.spigot.service.WebhookService;
import de.pcblc.spigot.util.MessageService;
import de.pcblc.spigot.util.ResourceConfigurationLoader;
import de.pcblc.common.config.PluginConfiguration;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class LuckRankSpigot extends JavaPlugin {

    private NotificationService notificationService;

    @Override
    public void onEnable() {
        try {
            ResourceConfigurationLoader configurationLoader = new ResourceConfigurationLoader(this);
            FileConfiguration config = configurationLoader.load("config.yml");
            FileConfiguration messages = configurationLoader.load("messages.yml");

            PluginConfiguration pluginConfiguration = new PluginConfiguration(new SpigotConfigAdapter(config));
            if (!pluginConfiguration.isEnabled()) {
                getLogger().warning("LuckRank is disabled in config.yml.");
                return;
            }

            MessageService messageService = new MessageService(messages, config.getString("prefix", "&2Luck&aRank &8>> &7"));
            LuckPerms luckPerms = LuckPermsProvider.get();

            notificationService = new NotificationService(this, pluginConfiguration);
            notificationService.initialize();

            UpdateCheckService updateCheckService = new UpdateCheckService(this, pluginConfiguration, messageService);
            RankService rankService = new RankService(this, luckPerms);
            WebhookService webhookService = new WebhookService(this, pluginConfiguration);
            RankCommand rankCommand = new RankCommand(this, rankService, notificationService, updateCheckService, webhookService, messageService);

            PluginCommand command = getCommand("rank");
            if (command == null) {
                throw new IllegalStateException("Command 'rank' is missing in plugin.yml");
            }
            command.setExecutor(rankCommand);
            command.setTabCompleter(rankCommand);

            getServer().getPluginManager().registerEvents(
                    new PlayerJoinListener(notificationService, updateCheckService),
                    this
            );

            updateCheckService.checkForUpdates(true, false);
            getLogger().info("LuckRank Spigot enabled successfully.");
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "LuckRank Spigot could not be enabled.", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (notificationService != null) {
            notificationService.shutdown();
        }
    }
}
