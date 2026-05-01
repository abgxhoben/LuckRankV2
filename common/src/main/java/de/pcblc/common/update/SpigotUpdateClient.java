package de.pcblc.common.update;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class SpigotUpdateClient {

    public String fetchLatestVersion(int resourceId) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "https://api.spigotmc.org/legacy/update.php?resource=" + resourceId
        ).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return reader.readLine();
        }
    }
}
