package me.bestem0r.spawnercollectors;

import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.command.*;
import me.bestem0r.spawnercollectors.database.SQLManager;
import me.bestem0r.spawnercollectors.listener.AFKListener;
import me.bestem0r.spawnercollectors.listener.BlockListener;
import me.bestem0r.spawnercollectors.listener.JoinListener;
import me.bestem0r.spawnercollectors.listener.QuitListener;
import me.bestem0r.spawnercollectors.loot.LootManager;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.CorePlugin;
import net.bestemor.core.command.CommandModule;
import net.bestemor.core.config.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static me.bestem0r.spawnercollectors.DataStoreMethod.MYSQL;
import static me.bestem0r.spawnercollectors.DataStoreMethod.YAML;

public final class SCPlugin extends CorePlugin {

    private Economy econ;

    public Map<UUID, Collector> collectors = new HashMap<>();

    public static List<String> log = new ArrayList<>();

    private LootManager lootManager;

    private final Map<UUID, Double> earned = new HashMap<>();

    private boolean usingHeadDB;
    private boolean morePermissions;
    private boolean disablePlace;

    private AFKListener afkListener;

    private int maxSpawners;

    private int spawnAmount;
    private int spawnTimeMin;
    private int spawnTimeMax;

    private SQLManager sqlManager;
    private DataStoreMethod storeMethod = YAML;

    @Override
    public void onPluginEnable() {
        Metrics metricsLite = new Metrics(this, 9427);

        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        this.afkListener = new AFKListener(this);
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

        //Bukkit.getLogger().warning("[SpawnerCollectors] §cYou are running a §aBETA 1.7.5-#1 of SpawnerCollectors! Please expect and report all bugs in my discord server");

        if (getConfig().getLong("auto_save") > 0) {
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                collectors.values().forEach(Collector::saveAsync);
            }, getConfig().getLong("auto_save") * 20, getConfig().getLong("auto_save") * 20);
        }
    }

    @Override
    public void onPluginDisable() {
        saveLog();
        for (Collector collector : collectors.values()) {
            collector.saveSync();

        }
        if (storeMethod == MYSQL) {
            this.getSqlManager().onDisable();
        }
    }

    @Override
    protected int getSpigotResourceID() {
        return 85852;
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
                earned.remove(null);
                for (UUID uuid : earned.keySet()) {

                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {

                        double playerEarned = Math.round(earned.get(uuid) * 100.0) / 100.0;
                        player.sendMessage(ConfigManager.getCurrencyBuilder("messages.earned_notify")
                                .replaceCurrency("%worth%", BigDecimal.valueOf(playerEarned))
                                .replace("%time%", String.valueOf(minutes))
                                .addPrefix()
                                .build());
                        SpawnerUtils.playSound(player, "notification");
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
        if (earned.containsKey(player.getUniqueId())) {
            earned.replace(player.getUniqueId(), earned.get(player.getUniqueId()) + amount);
        } else {
            earned.put(player.getUniqueId(), amount);
        }
    }

    @Override
    public boolean enableAutoUpdate() {
        return false;
    }
}
