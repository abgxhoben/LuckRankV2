package de.pcblc.spigot;

import org.bukkit.plugin.java.JavaPlugin;

public final class LuckRank extends JavaPlugin {

    @Override
    public void onEnable() {

        displayAsciiArt();
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
}
