package me.bestem0r.spawnercollectors;

import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.command.*;
import me.bestem0r.spawnercollectors.database.SQLManager;
import me.bestem0r.spawnercollectors.events.AFKListener;
import me.bestem0r.spawnercollectors.events.BlockListener;
import me.bestem0r.spawnercollectors.events.JoinListener;
import me.bestem0r.spawnercollectors.events.QuitListener;
import me.bestem0r.spawnercollectors.loot.LootManager;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.command.CommandModule;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import net.bestemor.core.menu.MenuListener;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.io.FileUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

import static me.bestem0r.spawnercollectors.DataStoreMethod.MYSQL;
import static me.bestem0r.spawnercollectors.DataStoreMethod.YAML;

public final class SCPlugin extends JavaPlugin {

    private Economy econ;

    public Map<UUID, Collector> collectors = new HashMap<>();

    public static List<String> log = new ArrayList<>();

    private LootManager lootManager;

    private final Map<OfflinePlayer, Double> earned = new HashMap<>();

    private boolean usingHeadDB;
    private boolean morePermissions;
    private boolean disablePlace;

    private AFKListener afkListener;

    private int maxSpawners;

    private int spawnAmount;
    private int spawnTimeMin;
    private int spawnTimeMax;

    private MenuListener menuListener;

    private SQLManager sqlManager;
    private DataStoreMethod storeMethod = YAML;

    @Override
    public void onEnable() {
        Metrics metricsLite = new Metrics(this, 9427);

        if (!new File(getDataFolder() + "/config.yml").exists()) {
            if (VersionUtils.getMCVersion() < 13) {

                String fileName = VersionUtils.getMCVersion() == 8 ? "config_1_8.yml" : "config_legacy.yml";
                InputStream stream = getResource(fileName);

                File target = new File(getDataFolder() + "/config.yml");
                try {
                    FileUtils.copyInputStreamToFile(Objects.requireNonNull(stream), target);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                saveDefaultConfig();
            }
        }
        getConfig().options().copyDefaults();
        ConfigManager.setConfig(getConfig());
        ConfigManager.setPrefixPath("prefix");

        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        this.afkListener = new AFKListener(this);
        this.menuListener = new MenuListener(this);
        Bukkit.getPluginManager().registerEvents(menuListener, this);
        Bukkit.getPluginManager().registerEvents(afkListener, this);

        this.lootManager = new LootManager(this);
        setupEconomy();
        loadValues();

        if (storeMethod == MYSQL) {
            sqlManager = new SQLManager(this);
            sqlManager.setupEntityData();
            sqlManager.setupPlayerData();
        }

        startSpawners();
        startMessages();

        CommandModule commandModule = new CommandModule.Builder(this)
                .addSubCommand("reload", new ReloadCommand(this))
                .addSubCommand("mobs", new MobsCommand(this))
                .addSubCommand("spawners", new SpawnersCommand(this))
                .addSubCommand("givespawner", new GiveSpawnerCommand(this))
                .addSubCommand("open", new OpenCommand(this))
                .addSubCommand("migrate", new MigrateCommand(this))
                .permissionPrefix("spawnercollectors.command")
                .build();

        commandModule.register("sc");

        for (Player player : Bukkit.getOnlinePlayers()) {
            collectors.put(player.getUniqueId(), new Collector(this, player.getUniqueId()));
        }

        //Bukkit.getLogger().warning("[SpawnerCollectors] §cYou are running a §aBETA 1.7.1-#1 of SpawnerCollectors! Please expect and report all bugs in my discord server");

        if (getConfig().getLong("auto_save") > 0) {
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                collectors.values().forEach(Collector::saveAsync);
            }, getConfig().getLong("auto_save") * 20, getConfig().getLong("auto_save") * 20);
        }
    }

    @Override
    public void onDisable() {
        saveLog();
        for (Collector collector : collectors.values()) {
            collector.saveSync();

        }
        if (storeMethod == MYSQL) {
            this.getSqlManager().onDisable();
        }
    }

    /** Loads values from config */
    private void loadValues() {
        this.usingHeadDB = getConfig().getBoolean("use_headdb");
        this.maxSpawners = getConfig().getInt("max_spawners");
        this.morePermissions = getConfig().getBoolean("more_permissions");
        this.storeMethod = DataStoreMethod.valueOf(getConfig().getString("data_storage_method"));
        this.disablePlace = getConfig().getBoolean("disable_spawner_placing");
        if (usingHeadDB && !Bukkit.getPluginManager().isPluginEnabled("HeadDatabase")) {
            Bukkit.getLogger().severe("[SpawnerCollectors] Could not find HeadDatabase. Defaulting to material IDs!");
            this.usingHeadDB = false;
        }
        this.spawnAmount = getConfig().getInt("spawner.spawns");
        this.spawnTimeMin = getConfig().getInt("spawner.min_time");
        this.spawnTimeMax = getConfig().getInt("spawner.max_time");
        lootManager.load();
    }

    /** Saves log */
    private void saveLog() {
        if (getConfig().getBoolean("log")) {
            Date date = new Date();
            String fileName = date.toString().replace(":","-");
            File file = new File(this.getDataFolder() + "/logs/" + fileName + ".yml");
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
    public void reloadValues(){
        reloadConfig();
        saveDefaultConfig();


        Bukkit.getScheduler().cancelTasks(this);
        saveLog();
        lootManager.load();
        loadValues();

        startSpawners();
        startMessages();
    }

    /** Starts async thread to replicate spawner mechanics */
    private void startSpawners() {
        long timer = 20L * getConfig().getInt("spawn_interval");
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                for (Collector collector : collectors.values()) {
                    collector.attemptSpawn();
                }
            } catch (ConcurrentModificationException e) {
                Bukkit.getLogger().severe("[SpawnerCollectors] ConcurrentModificationException in spawner thread!");
            }

        }, timer, timer);
    }
    /** Async message thread for earned money by auto-sell */
    private void startMessages() {
        int minutes = getConfig().getInt("notify_interval");
        if (minutes > 0) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                for (OfflinePlayer offlinePlayer : earned.keySet()) {

                    if (offlinePlayer.isOnline()) {
                        Player player = offlinePlayer.getPlayer();
                        if (player == null) { continue; }

                        double playerEarned = Math.round(earned.get(offlinePlayer) * 100.0) / 100.0;
                        player.sendMessage(ConfigManager.getCurrencyBuilder("messages.earned_notify")
                                .replaceCurrency("%worth%", BigDecimal.valueOf(playerEarned))
                                .replace("%time%", String.valueOf(minutes))
                                .addPrefix()
                                .build());
                        SpawnerUtils.playSound(this, player, "notification");
                    }
                }
                earned.clear();
            }, (long) minutes * 20 * 60, (long) minutes * 20 * 60);
        }
    }

    /** Setup Vault Economy integration */
    private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        } else {
            Bukkit.getLogger().info("Could not find Economy Provider!");
        }

    }

    //Getters
    public Economy getEconomy() {
        return econ;
    }
    public boolean isUsingHeadDB() {
        return usingHeadDB;
    }
    public LootManager getLootManager() {
        return lootManager;
    }
    public DataStoreMethod getStoreMethod() {
        return storeMethod;
    }
    public int getMaxSpawners() {
        return maxSpawners;
    }
    public boolean isMorePermissions() {
        return morePermissions;
    }
    public boolean isDisablePlace() {
        return disablePlace;
    }
    public int getSpawnAmount() {
        return spawnAmount;
    }
    public int getSpawnTimeMin() {
        return spawnTimeMin;
    }
    public int getSpawnTimeMax() {
        return spawnTimeMax;
    }
    public AFKListener getAfkChecker() {
        return afkListener;
    }
    public SQLManager getSqlManager() {
        return sqlManager;
    }
    public MenuListener getMenuListener() {
        return menuListener;
    }

    public void setStoreMethod(DataStoreMethod storeMethod) {
        this.storeMethod = storeMethod;
        if (storeMethod == MYSQL) {
            if (sqlManager != null) {
                sqlManager.onDisable();
            }
            this.sqlManager = new SQLManager(this);
            sqlManager.setupPlayerData();
            sqlManager.setupPlayerData();
        }
    }

    public void loadAll() {
        if (storeMethod == YAML) {
            collectors.values().forEach(Collector::saveSync);
            for (File file : new File(getDataFolder() + "/collectors/").listFiles()) {
                collectors.values().add(new Collector(this, UUID.fromString(file.getName().split(".")[0])));
            }
        }
    }

    public void saveAll() {
        collectors.values().forEach(Collector::saveSync);
    }

    /** Earned message methods */
    public void addEarned(OfflinePlayer player, double amount) {
        if (earned.containsKey(player)) {
            earned.replace(player, earned.get(player) + amount);
        } else {
            earned.put(player, amount);
        }
    }
}
