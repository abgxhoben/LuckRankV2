package de.pcblc.bungee;

import de.pcblc.bungee.command.RankCommand;
import de.pcblc.bungee.manager.MySQLManager;
import de.pcblc.bungee.manager.VersionChecker;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;


public class LuckRank extends Plugin {

    public static String PREFIX;
    private Configuration config;
    private Configuration messagesConfig;
    private MySQLManager mySQLManager;
    private VersionChecker versionChecker;


    @Override
    public void onEnable() {

        config = loadConfig();
        loadPrefix();
        loadMessagesConfig();
        mySQLManager = new MySQLManager(config);
        mySQLManager.createTable();
        if (!config.getBoolean("enabled", true)) {
            getLogger().warning("LuckRank is disabled in the config.yml. Plugin will now disable itself.");
            getProxy().getPluginManager().unregisterListeners(this);
            return;
        }

        if (!mySQLManager.isValidConfig()) {
            getLogger().severe("Database configuration is missing or invalid. Please update the config.yml with your database details.");
            getProxy().getPluginManager().unregisterListeners(this);
            return;
        }
        displayAsciiArt();

        int resourceId = 113728;
        VersionChecker versionChecker = new VersionChecker(this, resourceId, config, messagesConfig, mySQLManager);
        getProxy().getPluginManager().registerListener(this, versionChecker);
        getProxy().getPluginManager().registerCommand(this, new RankCommand(this, config, messagesConfig, versionChecker, mySQLManager));
    }

    @Override
    public void onDisable() {
    }


    private void displayAsciiArt() {
        getLogger().info("§6.____                   __   __________                __    ");
        getLogger().info("§6|    |    __ __   ____ |  | _\\______   \\_____    ____ |  | __");
        getLogger().info("§6|    |   |  |  \\_/ ___\\|  |/ /|       _/\\__  \\  /    \\|  |/ /");
        getLogger().info("§6|    |___|  |  /\\  \\___|    < |    |   \\ / __ \\|   |  \\    < ");
        getLogger().info("§6|_______ \\____/  \\___  >__|_ \\|____|_  /(____  /___|  /__|_ \\");
        getLogger().info("§6        \\/           \\/     \\/       \\/      \\/     \\/     \\/");
        getLogger().info("§6LuckRank Plugin has loaded! | made by pcblc | Discord: https://discord.gg/WhfJhRvgrq");
    }

    private void loadPrefix() {
        String prefix = config.getString("prefix", "&2Luck&aRank &8» &7");
        PREFIX = ChatColor.translateAlternateColorCodes('&', prefix);
    }

    private Configuration loadConfig() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File configFile = new File(dataFolder, "config.yml");

        if (!configFile.exists()) {
            try (InputStream is = getResourceAsStream("config.yml")) {
                Files.copy(is, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void loadMessagesConfig() {
        File messagesFile = new File(getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            try (InputStream in = getResourceAsStream("messages.yml")) {
                Files.copy(in, messagesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create messages.yml", e);
            }
        }

        try {
            messagesConfig = ConfigurationProvider.getProvider(YamlConfiguration.class).load(messagesFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not load messages.yml", e);
        }
    }
}
