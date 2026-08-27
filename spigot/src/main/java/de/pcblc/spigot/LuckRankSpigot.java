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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class LuckRankSpigot extends JavaPlugin {

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
        HandlerList.unregisterAll(this);
        rankCommand = null;
        playerJoinListener = null;
    }

    public boolean reloadPluginState() {
        try {
            loadRuntimeState();
            return true;
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "LuckRank Spigot could not be reloaded.", exception);
            return false;
        }
    }

    public MessageService getMessageService() {
        return messageService;
    }

    private void loadRuntimeState() {
        FileConfiguration config = configurationLoader.load("config.yml");
        FileConfiguration messages = configurationLoader.load("messages.yml");

        PluginConfiguration pluginConfiguration = new PluginConfiguration(new SpigotConfigAdapter(config));
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

        PluginCommand command = getCommand("rank");
        if (command == null) {
            loadedNotificationService.shutdown();
            throw new IllegalStateException("Command 'rank' is missing in plugin.yml");
        }

        /* Prepare the command and listener as one replaceable runtime. */
        CommandExecutor oldExecutor = command.getExecutor();
        TabCompleter oldCompleter = command.getTabCompleter();
        try {
            command.setExecutor(loadedRankCommand);
            command.setTabCompleter(loadedRankCommand);
            HandlerList.unregisterAll(this);
            getServer().getPluginManager().registerEvents(loadedPlayerJoinListener, this);
        } catch (RuntimeException exception) {
            command.setExecutor(oldExecutor);
            command.setTabCompleter(oldCompleter);
            HandlerList.unregisterAll(this);
            if (playerJoinListener != null) {
                getServer().getPluginManager().registerEvents(playerJoinListener, this);
            }
            loadedNotificationService.shutdown();
            throw exception;
        }

        NotificationService oldNotificationService = notificationService;

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
