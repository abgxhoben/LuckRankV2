package de.pcblc.bungee.manager;

import net.md_5.bungee.config.Configuration;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLManager {

    private final String url;
    private final String user;
    private final String password;
    private final String type;
    private boolean validConfig = true;

    public MySQLManager(Configuration config) {
        this.type = config.getString("database.type");
        if (this.type.equalsIgnoreCase("mysql")) {
            String host = config.getString("database.host");
            int port = config.getInt("database.port");
            String database = config.getString("database.database");
            this.user = config.getString("database.user");
            this.password = config.getString("database.password");

            if (host == null || port == -1 || database == null || user == null || password == null) {
                this.validConfig = false;
                this.url = null;
            } else {
                this.url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            }
        } else if (this.type.equalsIgnoreCase("sqlite")) {
            this.user = null;
            this.password = null;
            this.url = "jdbc:sqlite:" + new File("plugins/LuckRank/luckrank.db").getAbsolutePath();
        } else {
            this.validConfig = false;
            this.url = null;
            this.user = null;
            this.password = null;
        }
    }

    public boolean isValidConfig() {
        return validConfig;
    }

    public Connection getConnection() throws SQLException {
        if (!validConfig) {
            throw new SQLException("Invalid database configuration.");
        }
        return DriverManager.getConnection(url, user, password);
    }

    public void createTable() {
        if (!validConfig) return;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS notifications (player_uuid VARCHAR(36) PRIMARY KEY, notify BOOLEAN NOT NULL DEFAULT TRUE)")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean getNotifyStatus(String playerUUID) {
        if (!validConfig) return true;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT notify FROM notifications WHERE player_uuid = ?")) {
            statement.setString(1, playerUUID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getBoolean("notify");
            } else {
                setNotifyStatus(playerUUID, true); // Default to true if not found
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return true; // Default to true in case of error
        }
    }

    public void setNotifyStatus(String playerUUID, boolean notify) {
        if (!validConfig) return;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "REPLACE INTO notifications (player_uuid, notify) VALUES (?, ?)")) {
            statement.setString(1, playerUUID);
            statement.setBoolean(2, notify);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}