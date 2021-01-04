package me.bestem0r.spawnercollectors;

import me.bestem0r.spawnercollectors.commands.CommandModule;
import me.bestem0r.spawnercollectors.commands.subcommands.GiveSpawnerCommand;
import me.bestem0r.spawnercollectors.commands.subcommands.MobsCommand;
import me.bestem0r.spawnercollectors.commands.subcommands.ReloadCommand;
import me.bestem0r.spawnercollectors.commands.subcommands.SpawnersCommand;
import me.bestem0r.spawnercollectors.events.Join;
import me.bestem0r.spawnercollectors.events.Quit;
import me.bestem0r.spawnercollectors.loot.ItemLoot;
import me.bestem0r.spawnercollectors.utils.ColorBuilder;
import me.bestem0r.spawnercollectors.utils.Database;
import me.bestem0r.spawnercollectors.utils.Methods;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.xml.crypto.Data;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static me.bestem0r.spawnercollectors.DataStoreMethod.MYSQL;
import static me.bestem0r.spawnercollectors.DataStoreMethod.YAML;

public class SCPlugin extends JavaPlugin {

    private Economy econ;

    public List<Collector> collectors = new ArrayList<>();
    public Map<EntityType, Double> prices = new HashMap<>();
    public Map<EntityType, String> materials = new HashMap<>();

    public List<String> log = new ArrayList<>();

    private final Map<OfflinePlayer, Double> earned = new HashMap<>();
    private final EnumMap<EntityType, List<ItemLoot>> customLoot = new EnumMap<>(EntityType.class);

    private boolean usingCustomLoot;
    private boolean usingHeadDB;
    private boolean morePermissions;
    private boolean giveXP;
    private int maxSpawners;

    private DataStoreMethod storeMethod = YAML;

    @Override
    public void onEnable() {
        super.onEnable();

        Metrics metricsLite = new Metrics(this, 9427);

        getConfig().options().copyDefaults();
        saveDefaultConfig();
        reloadConfig();

        Bukkit.getPluginManager().registerEvents(new Join(this), this);
        Bukkit.getPluginManager().registerEvents(new Quit(this), this);

        setupEconomy();
        loadValues();

        if (storeMethod == MYSQL) {
            Database.setup(this);
        }

        startSpawners();
        startMessages();

        CommandModule commandModule = new CommandModule.Builder(this)
                .addSubCommand("reload", new ReloadCommand(this))
                .addSubCommand("mobs", new MobsCommand(this))
                .addSubCommand("spawners", new SpawnersCommand(this))
                .addSubCommand("givespawner", new GiveSpawnerCommand(this))
                .build();
        commandModule.register("sc");

        for (Player player : Bukkit.getOnlinePlayers()) {
            collectors.add(new Collector(this, player));
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        saveLog();
        for (Collector collector : collectors) {
            collector.save();
        }
        if (storeMethod == MYSQL) {
            try {
                Database.getDataBaseConnection().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Bukkit.getScheduler().cancelTasks(this);
    }

    /** Loads values from config */
    private void loadValues() {
        this.usingHeadDB = getConfig().getBoolean("use_headdb");
        this.usingCustomLoot = getConfig().getBoolean("custom_loot_tables.enable");
        this.maxSpawners = getConfig().getInt("max_spawners");
        this.morePermissions = getConfig().getBoolean("more_permissions");
        this.giveXP = getConfig().getBoolean("give_xp");
        this.storeMethod = DataStoreMethod.valueOf(getConfig().getString("data_storage_method"));
        if (usingHeadDB && !Bukkit.getPluginManager().isPluginEnabled("HeadDatabase")) {
            Bukkit.getLogger().severe("[SpawnerCollectors] Could not find HeadDatabase. Defaulting to material IDs!");
            this.usingHeadDB = false;
        }
        loadCustomLoot();
        loadEntities();
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

        if (getStoreMethod() == MYSQL) {
            try {
                Database.getDataBaseConnection().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        Bukkit.getScheduler().cancelTasks(this);
        saveLog();
        prices.clear();
        materials.clear();
        loadValues();

        if (storeMethod == MYSQL) {
            Database.setup(this);
        }

        startSpawners();
        startMessages();
    }

    /** Starts async thread to replicate spawner mechanics */
    private void startSpawners() {
        long timer = 20L * getConfig().getInt("spawn_interval");
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
                    player.sendMessage(new ColorBuilder(this).path("messages.earned_notify")
                            .replaceWithCurrency("%worth%", String.valueOf(playerEarned))
                            .replace("%time%", String.valueOf(minutes))
                            .addPrefix()
                            .build());
                    Methods.playSound(this, player, "notification");
                }
            }
            earned.clear();
        }, (long) minutes * 20 * 60, (long) minutes * 20 * 60);
    }

    /** Loads custom loot tables from config */
    private void loadCustomLoot() {



        customLoot.clear();
        if (!usingCustomLoot) { return; }
        ConfigurationSection mobs = getConfig().getConfigurationSection("custom_loot_tables.mobs");
        for (String mob : mobs.getKeys(false)) {

            EntityType entityType = EntityType.valueOf(mob);
            ConfigurationSection items = getConfig().getConfigurationSection("custom_loot_tables.mobs." + mob);
            for (String item : items.getKeys(false)) {

                Material material = Material.valueOf(item);
                double probability = getConfig().getDouble("custom_loot_tables.mobs." + mob + "." + item + ".probability");
                int min = getConfig().getInt("custom_loot_tables.mobs." + mob + "." + item + ".min");
                int max = getConfig().getInt("custom_loot_tables.mobs." + mob + "." + item + ".max");

                List<ItemLoot> loot = (customLoot.containsKey(entityType) ? customLoot.get(entityType) : new ArrayList<>());
                loot.add(new ItemLoot(material, probability, min, max));
                customLoot.put(entityType, loot);
            }
        }
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
    public boolean isUsingCustomLoot() {
        return usingCustomLoot;
    }
    public EnumMap<EntityType, List<ItemLoot>> getCustomLoot() {
        return customLoot;
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
    public boolean doGiveXP() {
        return giveXP;
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
