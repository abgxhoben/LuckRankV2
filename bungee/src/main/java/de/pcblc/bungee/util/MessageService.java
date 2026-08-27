package de.pcblc.bungee.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.config.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MessageService {

    private final Configuration messages;
    private final String prefix;

    public MessageService(Configuration messages, String prefix) {
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
        String text = colorize(resolve(messages.getString(path, "")));
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(text.split("\\r?\\n"));
    }

    public BaseComponent[] toComponents(String text) {
        return TextComponent.fromLegacyText(text);
    }

    public String stripColor(String text) {
        return ChatColor.stripColor(text);
    }

    private String resolve(String path, String... placeholders) {
        return resolveValue(messages.getString(path, path), placeholders);
    }

    private String resolveValue(String value, String... placeholders) {
        if (value == null) {
            return "";
        }
        for (int index = 0; index + 1 < placeholders.length; index += 2) {
            String replacement = placeholders[index + 1] == null ? "" : placeholders[index + 1];
            value = value.replace("{" + placeholders[index] + "}", replacement);
            value = value.replace("%" + placeholders[index] + "%", replacement);
        }
        return value.replace("\\n", "\n");
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
