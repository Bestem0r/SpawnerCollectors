package me.bestem0r.spawnercollectors;

import me.bestem0r.spawnercollectors.events.InventoryClick;
import me.bestem0r.spawnercollectors.menus.EntityMenu;
import me.bestem0r.spawnercollectors.menus.SpawnerMenu;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Collector {

    private final File file;
    private final FileConfiguration config;
    private final OfflinePlayer owner;

    private final List<CollectedSpawner> collectedSpawners = new ArrayList<>();

    private Inventory spawnerMenu;
    private Inventory entityMenu;

    private boolean autoSell;

    public Collector(File file) {
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
        this.owner = Bukkit.getOfflinePlayer(UUID.fromString(config.getString("owner_uuid")));

        this.autoSell = config.getBoolean("auto_sell");

        //Loads values from config
        ConfigurationSection entitySection = config.getConfigurationSection("entities");
        if (entitySection != null) {
            for (String entityKey : entitySection.getKeys(false)) {
                int entityAmount = config.getInt("entities." + entityKey);
                int spawnerAmount = config.getInt("spawners." + entityKey);
                collectedSpawners.add(new CollectedSpawner(EntityType.valueOf(entityKey), entityAmount, spawnerAmount));
            }
        }
    }

    /** Runs when player interacts with spawner menu */
    public void spawnerMenuInteract(int slot, Player player) {

    }

    /** Runs when player interacts with entity menu */
    public void entityMenuInteract(int slot, Player player) {

    }

    /** Attempts to spawn virtual mobs */
    public void attemptSpawn() {
        for (CollectedSpawner collectedSpawner : collectedSpawners) {
            collectedSpawner.attemptSpawn();
        }
        updateSpawnerMenuIfView();
    }

    /** Toggles auto-sell */
    private void toggleAutoSell() {
        autoSell = !autoSell;
        updateEntityMenu();
    }

    /** Returns spawner Inventory */
    public void openSpawnerMenu(Player player) {
        if (this.spawnerMenu == null) {
            this.spawnerMenu = SpawnerMenu.create(collectedSpawners, autoSell);
        } else {
            this.spawnerMenu.setContents(SpawnerMenu.create(collectedSpawners, autoSell).getContents());
        }
        player.openInventory(spawnerMenu);
        Bukkit.getPluginManager().registerEvents(new InventoryClick(this, player, InventoryClick.Menu.SPAWNER), SCPlugin.getInstance());
    }
    /** Returns entity Inventory */
    public void openEntityMenu(Player player) {
        if (this.entityMenu == null) {
            this.entityMenu = EntityMenu.create(collectedSpawners, autoSell);
        } else {
            this.entityMenu.setContents(EntityMenu.create(collectedSpawners, autoSell).getContents());
        }
        player.openInventory(entityMenu);
        Bukkit.getPluginManager().registerEvents(new InventoryClick(this, player, InventoryClick.Menu.SPAWNER), SCPlugin.getInstance());
    }

    /** Updates content of spawner menu */
    private void updateSpawnerMenu() {
        this.spawnerMenu.setContents(SpawnerMenu.create(collectedSpawners, autoSell).getContents());
    }

    /** Updates content of entity menu */
    private void updateEntityMenu() {
        this.entityMenu.setContents(EntityMenu.create(collectedSpawners, autoSell).getContents());
    }

    /** Updates spawner menu if a player is currently viewing it */
    private void updateSpawnerMenuIfView() {
        if (spawnerMenu != null) {
            if (spawnerMenu.getViewers().size() > 0) {
                updateSpawnerMenu();
            }
        }
    }

    /** Saves collector */
    public void save() {
        for (CollectedSpawner spawner : collectedSpawners) {
            config.set("spawners." + spawner.getEntityType().name(), spawner.getSpawnerAmount());
            config.set("entities." + spawner.getEntityType().name(), spawner.getEntityAmount());
            config.set("auto_sell", autoSell);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Getters */
    public OfflinePlayer getOwner() {
        return owner;
    }
}
