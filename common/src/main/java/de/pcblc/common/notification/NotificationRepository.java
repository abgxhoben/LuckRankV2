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
    private boolean mysql;

    public NotificationRepository(PluginConfiguration configuration, File dataFolder) {
        this.configuration = configuration;
        this.dataFolder = dataFolder;
    }

    public synchronized void initialize() throws SQLException {
        close();
        try {
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
        } catch (SQLException exception) {
            close();
            throw exception;
        }
    }

    public synchronized boolean isAvailable() {
        return available && connection != null;
    }

    public synchronized void ensurePlayerExists(UUID playerId) throws SQLException {
        if (!isAvailable()) {
            return;
        }

        String sql = mysql
                ? "INSERT INTO notifications (player_uuid, notify) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE player_uuid = player_uuid"
                : "INSERT OR IGNORE INTO notifications (player_uuid, notify) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setBoolean(2, true);
            statement.executeUpdate();
        }
    }

    public synchronized boolean getNotificationState(UUID playerId) throws SQLException {
        if (!isAvailable()) {
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

    public synchronized void setNotificationState(UUID playerId, boolean enabled) throws SQLException {
        if (!isAvailable()) {
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

    public synchronized void close() throws SQLException {
        try {
            if (connection != null) {
                connection.close();
            }
        } finally {
            connection = null;
            available = false;
            mysql = false;
        }
    }

    private boolean openConnection() throws SQLException {
        String databaseType = configuration.getDatabaseType();
        if (databaseType == null || databaseType.trim().isEmpty()) {
            databaseType = "sqlite";
        }

        if ("mysql".equalsIgnoreCase(databaseType.trim())) {
            mysql = true;
            loadDriver("com.mysql.cj.jdbc.Driver");
            String host = configuration.getDatabaseHost();
            String database = configuration.getDatabaseName();
            String user = configuration.getDatabaseUser();
            String password = configuration.getDatabasePassword();

            if (host == null || database == null || user == null
                    || host.trim().isEmpty() || database.trim().isEmpty() || user.trim().isEmpty()) {
                mysql = false;
                return false;
            }

            connection = DriverManager.getConnection(
                    "jdbc:mysql://" + host.trim() + ":" + configuration.getDatabasePort() + "/" + database.trim(),
                    user.trim(),
                    password == null ? "" : password
            );
            return true;
        }

        if (!"sqlite".equalsIgnoreCase(databaseType.trim())) {
            throw new SQLException("Unsupported database type: " + databaseType);
        }

        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new SQLException("Could not create plugin data folder: " + dataFolder.getAbsolutePath());
        }

        mysql = false;
        loadDriver("org.sqlite.JDBC");
        File databaseFile = new File(dataFolder, "luckrank.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        return true;
    }

    private void loadDriver(String className) throws SQLException {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new SQLException("Database driver is missing: " + className, exception);
        }
    }
}
