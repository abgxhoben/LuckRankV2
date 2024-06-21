package de.pcblc.spigot;


import de.pcblc.spigot.command.RankCommand;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class LuckRank extends JavaPlugin {

    public static String PREFIX;
    private FileConfiguration config;
    private FileConfiguration messagesConfig;
    private MySQLManager mySQLManager;

    @Override
    public void onEnable() {
        loadConfig();
        loadMessagesConfig();
        mySQLManager = new MySQLManager(config);
        mySQLManager.createTable();
        if (!config.getBoolean("enabled", true)) {
            getLogger().warning("LuckRank is disabled in the config.yml. Plugin will now disable itself.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!mySQLManager.isValidConfig()) {
            getLogger().severe("Database configuration is missing or invalid. Please update the config.yml with your database details.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        displayAsciiArt();

        int resourceId = 113728;
        VersionChecker versionChecker = new VersionChecker(this, resourceId, config, messagesConfig, mySQLManager);
        getServer().getPluginManager().registerEvents(versionChecker, this);
        getCommand("rank").setExecutor(new RankCommand(config,this, messagesConfig, versionChecker, mySQLManager));
        getCommand("rank").setTabCompleter(new RankCommand(config, this, messagesConfig, versionChecker, mySQLManager));
    }

    @Override
    public void onDisable() {

    }

    private void loadConfig() {
        saveDefaultConfig();
        config = getConfig();
        String prefix = config.getString("prefix");

        if (prefix != null) {
            PREFIX = ChatColor.translateAlternateColorCodes('&', prefix);
        } else {
            PREFIX = ChatColor.translateAlternateColorCodes('&', "&2Luck&aRank &8» &7");
        }

    }

    private void loadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }



    private void displayAsciiArt() {
        getServer().getConsoleSender().sendMessage("§6.____                   __   __________                __    ");
        getServer().getConsoleSender().sendMessage("§6|    |    __ __   ____ |  | _\\______   \\_____    ____ |  | __");
        getServer().getConsoleSender().sendMessage("§6|    |   |  |  \\_/ ___\\|  |/ /|       _/\\__  \\  /    \\|  |/ /");
        getServer().getConsoleSender().sendMessage("§6|    |___|  |  /\\  \\___|    < |    |   \\ / __ \\|   |  \\    < ");
        getServer().getConsoleSender().sendMessage("§6|_______ \\____/  \\___  >__|_ \\|____|_  /(____  /___|  /__|_ \\");
        getServer().getConsoleSender().sendMessage("§6        \\/           \\/     \\/       \\/      \\/     \\/     \\/");
        getServer().getConsoleSender().sendMessage("§6LuckRank Plugin has loaded! | made by pcblc | Discord: https://discord.gg/WhfJhRvgrq");
    }
}
