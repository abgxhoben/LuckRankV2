package de.pcblc.spigot.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MessageService {

    private final FileConfiguration messages;
    private final String prefix;

    public MessageService(FileConfiguration messages, String prefix) {
        this.messages = messages;
        this.prefix = colorize(prefix);
    }

    public String prefixed(String path, String... placeholders) {
        return prefix + raw(path, placeholders);
    }

    public String prefixedRaw(String rawLine) {
        return prefix + colorize(rawLine);
    }

    public String raw(String path, String... placeholders) {
        return colorize(resolve(path, placeholders));
    }

    public List<String> getLines(String path) {
        String text = messages.getString(path, "");
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(text.split("\\r?\\n"));
    }

    public String stripColor(String text) {
        return ChatColor.stripColor(text);
    }

    private String resolve(String path, String... placeholders) {
        String value = messages.getString(path, path);
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            value = value.replace("{" + placeholders[index] + "}", placeholders[index + 1]);
        }
        return value;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
