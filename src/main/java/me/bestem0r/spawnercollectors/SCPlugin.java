package me.bestem0r.spawnercollectors;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SCPlugin extends JavaPlugin {

    private static SCPlugin instance;
    private static Economy econ;

    public static List<Collector> collectors = new ArrayList<>();
    public static HashMap<EntityType, Double> prices = new HashMap<>();
    public static HashMap<EntityType, String> materials = new HashMap<>();

    private static boolean usingHeadDB;

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;

        loadValues();

        loadCollectors();
        startSpawners();
    }

    @Override
    public void onDisable() {
        super.onDisable();

        for (Collector collector : collectors) {
            collector.save();
        }

        Bukkit.getScheduler().cancelTasks(this);
    }

    /** Loads values from config */
    private void loadValues() {
        usingHeadDB = getConfig().getBoolean("use_headdb");
        loadEntities();
    }

    /** Starts async thread to replicate spawner mechanics */
    private void startSpawners() {
        long timer = 20 * getConfig().getInt("spawn_interval");
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Collector collector : collectors) {
                collector.attemptSpawn();
            }
        }, timer, timer);
    }

    /** Load entity prices and material strings */
    private void loadEntities() {
        ConfigurationSection priceSection = getConfig().getConfigurationSection("prices");
        if (priceSection == null) { return; }
        for (String entity : priceSection.getKeys(false)) {
            prices.put(EntityType.valueOf(entity), getConfig().getDouble("prices." + entity));
        }
        ConfigurationSection materialSection = getConfig().getConfigurationSection("materials");
        if (materialSection == null) { return; }
        for (String entity : materialSection.getKeys(false)) {
            materials.put(EntityType.valueOf(entity), getConfig().getString("materials." + entity));
        }
    }

    /** Setup Vault Economy integration */
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        } else {
            Bukkit.getLogger().info("Could not find Economy Provider!");
        }

        return (econ != null);
    }

    /** Load collectors from config */
    private void loadCollectors() {
        File folder = new File(Bukkit.getPluginManager().getPlugin("SpawnerCollectors").getDataFolder() + "/collectors");
        if (folder.exists() && folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                collectors.add(new Collector(file));
            }
        }
    }

    /** Getters */
    public static SCPlugin getInstance() {
        return instance;
    }
    public static Economy getEconomy() {
        return econ;
    }
    public static boolean isUsingHeadDB() {
        return usingHeadDB;
    }
}
