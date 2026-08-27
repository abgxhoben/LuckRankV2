package de.pcblc.common.webhook;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class DiscordWebhookPayloadBuilder {

    public String buildPayload(String title, int color, String footerText, String footerIconUrl, List<WebhookField> fields) {
        if (title == null) {
            title = "LuckRank";
        }
        if (footerText == null) {
            footerText = "LuckRank";
        }
        if (fields == null) {
            fields = Collections.emptyList();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("{\"embeds\":[{");
        builder.append("\"title\":\"").append(escape(title)).append("\",");
        builder.append("\"color\":").append(color).append(",");
        builder.append("\"fields\":[");

        int writtenFields = 0;
        for (WebhookField field : fields) {
            if (field == null) {
                continue;
            }
            if (writtenFields++ > 0) {
                builder.append(",");
            }
            builder.append("{");
            builder.append("\"name\":\"").append(escape(field.getName())).append("\",");
            builder.append("\"value\":\"").append(escape(field.getValue())).append("\",");
            builder.append("\"inline\":true");
            builder.append("}");
        }

        builder.append("],");
        builder.append("\"footer\":{");
        builder.append("\"text\":\"").append(escape(footerText)).append("\"");
        if (isValidUrl(footerIconUrl)) {
            builder.append(",");
            builder.append("\"icon_url\":\"").append(escape(footerIconUrl)).append("\"");
        }
        builder.append("}");
        builder.append("}]}");
        return builder.toString();
    }

    private boolean isValidUrl(String input) {
        if (input == null) {
            return false;
        }

        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.toUpperCase(Locale.ROOT).contains("YOUR_") || trimmed.toUpperCase(Locale.ROOT).contains("_HERE")) {
            return false;
        }

        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String escape(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
