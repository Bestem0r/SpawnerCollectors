/*
 * Copyright (c) 2021. Vebjørn Viem Elvekrok
 * All rights reserved.
 */

package me.bestem0r.spawnercollectors.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.bestem0r.spawnercollectors.SCPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConnectionPoolManager {

    private final SCPlugin plugin;

    private HikariDataSource hikari;

    private String hostname;
    private String port;
    private String database;
    private String username;
    private String password;

    private int minimumConnections;
    private int maximumConnections;
    private long connectionTimeout;

    public ConnectionPoolManager(SCPlugin plugin) {
        this.plugin = plugin;

        init();
        setupPool();
    }

    private void init() {
        FileConfiguration config = plugin.getConfig();

        this.username = config.getString("user");
        this.password = config.getString("password");
        this.database = config.getString("database");
        this.hostname = config.getString("address");
        this.port = config.getString("port");

        this.minimumConnections = 5;
        this.maximumConnections = 100;
        this.connectionTimeout = 30000;
    }

    private void setupPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
                "jdbc:mysql://" +
                        hostname +
                        ":" +
                        port +
                        "/" +
                        database +
                        "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC" +
                        "&characterEncoding=latin1&useConfigs=maxPerformance"
        );
        config.setDriverClassName("com.mysql.jdbc.Driver");
        config.setUsername(username);
        config.setPassword(password);
        config.setMinimumIdle(minimumConnections);
        config.setMaximumPoolSize(maximumConnections);
        config.setConnectionTimeout(connectionTimeout);
        config.setLeakDetectionThreshold(3000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        //config.setConnectionTestQuery(testQuery);
        this.hikari = new HikariDataSource(config);
    }


    public void close(Connection conn, PreparedStatement ps, ResultSet res) {
        if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        if (ps != null) try { ps.close(); } catch (SQLException ignored) {}
        if (res != null) try { res.close(); } catch (SQLException ignored) {}
    }


    public Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }

    public void closePool() {
        if (hikari != null && !hikari.isClosed()) {
            hikari.close();
        }
    }
}

