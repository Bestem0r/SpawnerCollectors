/*
 * Copyright (c) 2021. Vebjørn Viem Elvekrok
 * All rights reserved.
 */

package me.bestem0r.spawnercollectors.database;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.collector.EntityCollector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLManager {

    private final SCPlugin plugin;
    private final ConnectionPoolManager pool;

    public SQLManager(SCPlugin plugin) {

        this.plugin = plugin;
        this.pool = new ConnectionPoolManager(plugin);
    }

    public void setupEntityData() {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = pool.getConnection();
            statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS entity_data ("
                    + "id INT unsigned NOT NULL AUTO_INCREMENT,"
                    + "owner_uuid VARCHAR(150) NOT NULL,"
                    + "entity_type VARCHAR(150) NOT NULL,"
                    + "spawner_amount INT NOT NULL,"
                    + "entity_amount LONG NOT NULL,"
                    + "PRIMARY KEY (id))");
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.close(connection, statement, null);
        }
    }

    public void setupPlayerData() {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = pool.getConnection();
            statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS player_data ("
                    + "owner_uuid VARCHAR(150) NOT NULL,"
                    + "auto_sell BOOLEAN NOT NULL,"
                    + "PRIMARY KEY (owner_uuid))");
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.close(connection, statement, null);
        }
    }

    public void updateCollector(Collector collector) {

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = pool.getConnection();

            statement = connection.prepareStatement("REPLACE INTO player_data (owner_uuid, auto_sell) values (?,?)");
            statement.setString(1, collector.getUuid());
            statement.setBoolean(2, collector.isAutoSell());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.close(connection, statement, null);
        }
    }

    public void deleteEntityData(Collector collector) {

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = pool.getConnection();

            statement = connection.prepareStatement("DELETE FROM entity_data WHERE owner_uuid = ?");
            statement.setString(1, collector.getUuid());

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.close(connection, statement, null);
        }
    }

    public void insertEntityData(String uuid, EntityCollector spawner) {

        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = pool.getConnection();

            statement = connection.prepareStatement("INSERT INTO entity_data (owner_uuid, entity_type, spawner_amount, entity_amount) VALUES (?, ?, ?, ?)");

            statement.setString(1, uuid.toString());
            statement.setString(2, spawner.getEntityType().name());
            statement.setInt(3, spawner.getSpawnerAmount());
            statement.setLong(4, spawner.getEntityAmount());

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.close(connection, statement, null);
        }
    }

    public void loadCollector(Collector collector) {

        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            connection = pool.getConnection();

            statement = connection.prepareStatement("SELECT * FROM player_data WHERE owner_uuid = ?");
            statement.setString(1, collector.getUuid().toString());

            result = statement.executeQuery();

            if (result.next()) {
                collector.setAutoSell(result.getBoolean("auto_sell"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.close(connection, statement, result);
        }
    }

    public void deleteCollector(Collector collector) {
        deleteEntityData(collector);

        Connection connection = null;
        PreparedStatement statement = null;

        try {
            connection = pool.getConnection();

            statement = connection.prepareStatement("DELETE FROM player_data WHERE owner_uuid = ?");
            statement.setString(1, collector.getUuid().toString());

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.close(connection, statement, null);
        }
    }

    public List<EntityCollector> getEntityCollectors(String uuid) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet result = null;

        List<EntityCollector> entityCollectors = new ArrayList<>();

        try {
            connection = pool.getConnection();

            statement = connection.prepareStatement( "SELECT * FROM entity_data WHERE owner_uuid = ?");
            statement.setString(1, uuid);

            result = statement.executeQuery();

            while (result.next()) {
                int entityAmount = result.getInt("entity_amount");
                int spawnerAmount = result.getInt("spawner_amount");

                CustomEntityType type = new CustomEntityType(result.getString("entity_type"));
                entityCollectors.add(new EntityCollector(plugin, type, entityAmount, spawnerAmount));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.close(connection, statement, result);
        }
        return entityCollectors;
    }


    public void onDisable() {
        pool.closePool();
    }
}
