package me.bestem0r.spawnercollectors.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public class Database {

    private static Connection dataBaseConnection;

    private Database() {}

    public static void setup(JavaPlugin plugin) {

        FileConfiguration config = plugin.getConfig();
        String user = config.getString("user");
        String password = config.getString("password");
        String databaseName = config.getString("database");
        String address = config.getString("address");
        String port = config.getString("port");
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://" + address + ":" + port + "/" + databaseName + "?autoReconnect=true&useSSL=false", user, password);

            dataBaseConnection = connection;
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet playerData = metaData.getTables(null, null, "player_data", null);
            if (!playerData.next()) {
                String dogTable = "CREATE TABLE player_data ("
                        + "owner_uuid VARCHAR(150) NOT NULL,"
                        + "auto_sell BOOLEAN NOT NULL,"
                        + "PRIMARY KEY (owner_uuid))";

                Statement statement = connection.createStatement();
                statement.executeUpdate(dogTable);
                Bukkit.getLogger().info("[SpawnerCollectors] MySQL player_data table created!");
            }
            ResultSet entityData = metaData.getTables(null, null, "entity_data", null);
            if (!entityData.next()) {
                String dogTable = "CREATE TABLE entity_data ("
                        + "id INT unsigned NOT NULL AUTO_INCREMENT,"
                        + "owner_uuid VARCHAR(150) NOT NULL,"
                        + "entity_type VARCHAR(150) NOT NULL,"
                        + "spawner_amount INT NOT NULL,"
                        + "entity_amount INT NOT NULL,"
                        + "PRIMARY KEY (id))";

                Statement statement = connection.createStatement();
                statement.executeUpdate(dogTable);
                Bukkit.getLogger().info("[SpawnerCollectors] MySQL entity_data table created!");
            }


        } catch(Exception e){ e.printStackTrace();}
    }

    public static Connection getDataBaseConnection() {
        return dataBaseConnection;
    }
}

