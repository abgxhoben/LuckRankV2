package de.pcblc.common.webhook;

import java.util.List;

public final class DiscordWebhookPayloadBuilder {

    public String buildPayload(String title, int color, String footerText, String footerIconUrl, List<WebhookField> fields) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"embeds\":[{");
        builder.append("\"title\":\"").append(escape(title)).append("\",");
        builder.append("\"color\":").append(color).append(",");
        builder.append("\"fields\":[");

        for (int index = 0; index < fields.size(); index++) {
            if (index > 0) {
                builder.append(",");
            }

            WebhookField field = fields.get(index);
            builder.append("{");
            builder.append("\"name\":\"").append(escape(field.getName())).append("\",");
            builder.append("\"value\":\"").append(escape(field.getValue())).append("\",");
            builder.append("\"inline\":true");
            builder.append("}");
        }

        builder.append("],");
        builder.append("\"footer\":{");
        builder.append("\"text\":\"").append(escape(footerText)).append("\",");
        builder.append("\"icon_url\":\"").append(escape(footerIconUrl)).append("\"");
        builder.append("}");
        builder.append("}]}");
        return builder.toString();
    }

    private String escape(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }
}
