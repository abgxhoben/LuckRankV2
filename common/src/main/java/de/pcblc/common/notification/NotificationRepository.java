package de.pcblc.common.notification;

import de.pcblc.common.config.PluginConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class NotificationRepository {

    private final PluginConfiguration configuration;
    private final File dataFolder;
    private Connection connection;
    private boolean available;

    public NotificationRepository(PluginConfiguration configuration, File dataFolder) {
        this.configuration = configuration;
        this.dataFolder = dataFolder;
    }

    public void initialize() throws SQLException {
        available = openConnection();
        if (!available) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS notifications (" +
                        "player_uuid VARCHAR(36) PRIMARY KEY," +
                        "notify BOOLEAN NOT NULL DEFAULT TRUE)"
        )) {
            statement.executeUpdate();
        }
    }

    public boolean isAvailable() {
        return available;
    }

    public void ensurePlayerExists(UUID playerId) throws SQLException {
        if (!available) {
            return;
        }

        try {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO notifications (player_uuid, notify) VALUES (?, ?) " +
                            "ON DUPLICATE KEY UPDATE notify = notify"
            )) {
                statement.setString(1, playerId.toString());
                statement.setBoolean(2, true);
                statement.executeUpdate();
                return;
            }
        } catch (SQLException ignored) {
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT OR IGNORE INTO notifications (player_uuid, notify) VALUES (?, ?)"
        )) {
            statement.setString(1, playerId.toString());
            statement.setBoolean(2, true);
            statement.executeUpdate();
        }
    }

    public boolean getNotificationState(UUID playerId) throws SQLException {
        if (!available) {
            return true;
        }

        ensurePlayerExists(playerId);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT notify FROM notifications WHERE player_uuid = ?"
        )) {
            statement.setString(1, playerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return !resultSet.next() || resultSet.getBoolean("notify");
            }
        }
    }

    public void setNotificationState(UUID playerId, boolean enabled) throws SQLException {
        if (!available) {
            return;
        }

        ensurePlayerExists(playerId);
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE notifications SET notify = ? WHERE player_uuid = ?"
        )) {
            statement.setBoolean(1, enabled);
            statement.setString(2, playerId.toString());
            statement.executeUpdate();
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    private boolean openConnection() throws SQLException {
        if ("mysql".equalsIgnoreCase(configuration.getDatabaseType())) {
            String host = configuration.getDatabaseHost();
            String database = configuration.getDatabaseName();
            String user = configuration.getDatabaseUser();
            String password = configuration.getDatabasePassword();

            if (host.isEmpty() || database.isEmpty() || user.isEmpty()) {
                return false;
            }

            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host + ":" + configuration.getDatabasePort() + "/" + database,
                    user,
                    password
            );
            return true;
        }

        File databaseFile = new File(dataFolder, "luckrank.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        return true;
    }
}
