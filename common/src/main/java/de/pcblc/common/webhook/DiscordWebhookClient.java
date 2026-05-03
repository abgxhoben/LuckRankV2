package de.pcblc.common.webhook;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DiscordWebhookClient {

    public WebhookResponse post(String webhookUrl, String payload) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(webhookUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        String responseBody = readResponseBody(connection, responseCode);
        return new WebhookResponse(responseCode, responseBody);
    }

    private String readResponseBody(HttpURLConnection connection, int responseCode) throws Exception {
        InputStream stream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }

        try (InputStream inputStream = stream) {
            byte[] bytes = new byte[4096];
            StringBuilder builder = new StringBuilder();
            int read;
            while ((read = inputStream.read(bytes)) != -1) {
                builder.append(new String(bytes, 0, read, StandardCharsets.UTF_8));
            }
            return builder.toString();
        }
    }

    public static final class WebhookResponse {

        private final int responseCode;
        private final String responseBody;

        public WebhookResponse(int responseCode, String responseBody) {
            this.responseCode = responseCode;
            this.responseBody = responseBody == null ? "" : responseBody;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}
