package me.bestem0r.spawnercollectors;

import me.bestem0r.spawnercollectors.collector.CollectorManager;
import me.bestem0r.spawnercollectors.command.*;
import me.bestem0r.spawnercollectors.database.SQLManager;
import me.bestem0r.spawnercollectors.listener.AFKListener;
import me.bestem0r.spawnercollectors.listener.BlockListener;
import me.bestem0r.spawnercollectors.listener.JoinListener;
import me.bestem0r.spawnercollectors.listener.QuitListener;
import me.bestem0r.spawnercollectors.loot.LootManager;
import me.bestem0r.spawnercollectors.placeholders.PlaceholderAPIExpansion;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.CorePlugin;
import net.bestemor.core.command.CommandModule;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static me.bestem0r.spawnercollectors.DataStoreMethod.MYSQL;
import static me.bestem0r.spawnercollectors.DataStoreMethod.YAML;

public final class SCPlugin extends CorePlugin {

    private Economy econ;

    public static List<String> log = new ArrayList<>();

    private LootManager lootManager;
    private CollectorManager collectorManager;

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

        SpawnerUtils.loadHooks();

        this.collectorManager = new CollectorManager(this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        this.afkListener = new AFKListener(this);
        Bukkit.getPluginManager().registerEvents(afkListener, this);

        this.lootManager = new LootManager(this);
        setupEconomy();
        setupPlaceholders();
        loadValues();

        if (storeMethod == MYSQL) {
            sqlManager = new SQLManager(this);
            sqlManager.setupEntityData();
            sqlManager.setupPlayerData();
        }

        collectorManager.startSpawners();
        collectorManager.startMessages();

        CommandModule commandModule = new CommandModule.Builder(this)
                .addSubCommand("reload", new ReloadCommand(this))
                .addSubCommand("mobs", new MobsCommand(this))
                .addSubCommand("spawners", new SpawnersCommand(this))
                .addSubCommand("givespawner", new GiveSpawnerCommand(this))
                .addSubCommand("open", new OpenCommand(this))
                .addSubCommand("migrate", new MigrateCommand(this))
                .addSubCommand("withdraw", new WithdrawCommand(this))
                .permissionPrefix("spawnercollectors.command")
                .build();

        commandModule.register("sc");

        //Bukkit.getLogger().warning("[SpawnerCollectors] §cYou are running a §aBETA 1.7.5-#1 of SpawnerCollectors! Please expect and report all bugs in my discord server");

        collectorManager.load();
    }

    @Override
    public void onPluginDisable() {
        saveLog();
        collectorManager.saveAll();
        if (storeMethod == MYSQL) {
            this.getSqlManager().onDisable();
        }
    }

    @Override
    public void onEnable() {
        convertConfigs();
        super.onEnable();
    }

    private void convertConfigs() {
        File mobFile = new File(this.getDataFolder() + "/mobs.yml");
        if (!mobFile.exists()) {
            this.saveResource("mobs.yml", false);
            //Convert from old config
            if (getConfig().getConfigurationSection("materials") != null && getConfig().getConfigurationSection("prices") != null) {
                FileConfiguration mobsConfig = YamlConfiguration.loadConfiguration(mobFile);
                ConfigurationSection materials = getConfig().getConfigurationSection("materials");
                for (String key : materials.getKeys(false)) {
                    mobsConfig.set("mobs." + key + ".material", materials.getString(key));
                    mobsConfig.set("mobs." + key + ".price", getConfig().getDouble("prices." + key));
                }
                try {
                    mobsConfig.save(mobFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        File lootFile = new File(this.getDataFolder() + "/loot.yml");
        if (!lootFile.exists()) {
            this.saveResource("loot.yml", false);
            //Convert from old config
            FileConfiguration lootConfig = YamlConfiguration.loadConfiguration(lootFile);
            if (getConfig().getConfigurationSection("custom_loot_tables") != null) {
                ConfigurationSection lootTables = getConfig().getConfigurationSection("custom_loot_tables");
                lootConfig.set("custom_loot_tables", lootTables);
            }
            if (getConfig().getConfigurationSection("custom_xp") != null) {
                ConfigurationSection xp = getConfig().getConfigurationSection("custom_xp");
                lootConfig.set("custom_xp", xp);
            }
            try {
                lootConfig.save(lootFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

        saveLog();
        loadValues();

        collectorManager.startSpawners();
        collectorManager.startMessages();
    }

    /** Setup Vault Economy and Permission integration */
    private void setupEconomy() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            econ = economyProvider.getProvider();
        } else {
            Bukkit.getLogger().info("Could not find Economy Provider!");
        }
    }
    /** Setup PlaceholderAPI integration */
    private void setupPlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIExpansion(this).register();
        } else {
            Bukkit.getLogger().info("Could not find PlaceholderAPI!");
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

    public CollectorManager getCollectorManager() {
        return collectorManager;
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
}
