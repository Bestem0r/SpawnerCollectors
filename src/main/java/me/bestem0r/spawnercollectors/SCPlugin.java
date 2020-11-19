package me.bestem0r.spawnercollectors;

import me.bestem0r.spawnercollectors.commands.SCCompleter;
import me.bestem0r.spawnercollectors.commands.SCExecutor;
import me.bestem0r.spawnercollectors.events.Join;
import me.bestem0r.spawnercollectors.events.Quit;
import me.bestem0r.spawnercollectors.utilities.Color;
import me.bestem0r.spawnercollectors.utilities.MetricsLite;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SCPlugin extends JavaPlugin {

    private static SCPlugin instance;
    private static Economy econ;

    public static List<Collector> collectors = new ArrayList<>();
    public static HashMap<EntityType, Double> prices = new HashMap<>();
    public static HashMap<EntityType, String> materials = new HashMap<>();

    public static List<String> log = new ArrayList<>();

    private static final HashMap<OfflinePlayer, Double> earned = new HashMap<>();
    private static boolean usingHeadDB;

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;

        MetricsLite metricsLite = new MetricsLite(this, 9427);

        getConfig().options().copyDefaults();
        saveDefaultConfig();
        reloadConfig();

        Bukkit.getPluginManager().registerEvents(new Join(), this);
        Bukkit.getPluginManager().registerEvents(new Quit(), this);

        setupEconomy();
        loadValues();

        startSpawners();
        startMessages();


        getCommand("sc").setExecutor(new SCExecutor());
        getCommand("sc").setTabCompleter(new SCCompleter());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        saveLog();
        for (Collector collector : collectors) {
            collector.save();
        }

        Bukkit.getScheduler().cancelTasks(this);
    }

    /** Loads values from config */
    private void loadValues() {
        usingHeadDB = getConfig().getBoolean("use_headdb");
        if (usingHeadDB && !Bukkit.getPluginManager().isPluginEnabled("HeadDatabase")) {
            Bukkit.getLogger().severe("[SpawnerCollectors] Could not find HeadDatabase. Defaulting to material IDs!");
            usingHeadDB = false;
        }
        loadEntities();
    }

    /** Saves log */
    private void saveLog() {
        if (getConfig().getBoolean("log")) {
            Date date = new Date();
            String fileName = date.toString().replace(":","-");
            File file = new File(Bukkit.getServer().getPluginManager().getPlugin("SpawnerCollectors").getDataFolder() + "/logs/" + fileName + ".yml");
            FileConfiguration logConfig = YamlConfiguration.loadConfiguration(file);
            logConfig.set("log", log);

            try {
                logConfig.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /** Reloads config values */
    public void reloadValues() {
        reloadConfig();

        Bukkit.getScheduler().cancelTasks(this);
        saveLog();
        prices.clear();
        materials.clear();
        loadEntities();

        startSpawners();
        startMessages();
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
    /** Async message thread for earned money by auto-sell */
    private void startMessages() {
        int minutes = getConfig().getInt("notify_interval");
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (OfflinePlayer offlinePlayer : earned.keySet()) {

                if (offlinePlayer.isOnline()) {
                    Player player = offlinePlayer.getPlayer();
                    if (player == null) { continue; }

                    double playerEarned = Math.round(earned.get(offlinePlayer) * 100.0) / 100.0;
                    player.sendMessage(new Color.Builder().path("messages.earned_notify")
                            .replaceWithCurrency("%worth%", String.valueOf(playerEarned))
                            .replace("%time%", String.valueOf(minutes))
                            .addPrefix()
                            .build());
                    player.playSound(player.getLocation(), Sound.valueOf(SCPlugin.getInstance().getConfig().getString("sounds.notification")), 1, 1);
                }
            }
            earned.clear();
        }, minutes * 20 * 60, minutes * 20 * 60);
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


    /** Earned message methods */
    public static void addEarned(OfflinePlayer player, double amount) {
        if (earned.containsKey(player)) {
            earned.replace(player, earned.get(player) + amount);
        } else {
            earned.put(player, amount);
        }
    }
}
