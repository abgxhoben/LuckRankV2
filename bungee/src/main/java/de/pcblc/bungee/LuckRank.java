package de.pcblc.bungee;

import net.md_5.bungee.api.plugin.Plugin;

public final class LuckRank extends Plugin {

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
