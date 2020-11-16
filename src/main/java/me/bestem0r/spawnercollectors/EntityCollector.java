package me.bestem0r.spawnercollectors;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.bestem0r.spawnercollectors.utilities.Color;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EntityCollector {

    private final EntityType entityType;

    private final List<CollectedSpawner> spawners = new ArrayList<>();
    private int entityAmount;

    private final List<String> spawnerLore;
    private final List<String> entityLore;

    public EntityCollector(EntityType entityType, int entityAmount, int spawnerAmount) {
        this.entityType = entityType;

        FileConfiguration mainConfig = SCPlugin.getInstance().getConfig();
        this.spawnerLore = mainConfig.getStringList("menus.spawners.item_lore");
        this.entityLore = mainConfig.getStringList("menus.mobs.item_lore");

        this.entityAmount = entityAmount;
        for (int i = 0; i  < spawnerAmount; i++) {
            spawners.add(new CollectedSpawner());
        }
    }

    /** Adds entities */
    public void attemptSpawn(boolean autoSell, OfflinePlayer player) {

        int spawned = spawners.stream().mapToInt(CollectedSpawner::attemptSpawn).sum();
        if (autoSell) {
            Economy economy = SCPlugin.getEconomy();
            double worth = SCPlugin.prices.get(entityType) * spawned;
            economy.depositPlayer(player, worth);
            SCPlugin.addEarned(player, worth);
        } else {
            entityAmount += spawned;
        }
    }

    /** Adds spawner */
    public void addSpawner(int amount) {
        for (int i = 0; i < amount; i++) {
            spawners.add(new CollectedSpawner());
        }
    }

    /** Removes entities */
    public void removeEntities(int amount) {
        entityAmount -= amount;
    }
    /** Removes spawners */
    public void removeSpawners(int amount) {
        for (int i = 0; i < amount; i++) {
            spawners.remove(0);
        }
    }

    /** Returns ItemStack to display number of spawners */
    public ItemStack getSpawnerItem() {
        ItemStack item = new ItemStack(Material.SPAWNER);

        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + WordUtils.capitalizeFully(entityType.name().replaceAll("_", " ")));

        itemMeta.setLore(new Color.Builder(spawnerLore).replace("%amount%", String.valueOf(spawners.size())).buildLore());
        item.setItemMeta(itemMeta);
        return item;
    }

    /** Returns ItemStack to display number of entities */
    public ItemStack getEntityItem() {
        ItemStack item;
        if (SCPlugin.isUsingHeadDB()) {
            item = new HeadDatabaseAPI().getItemHead(SCPlugin.materials.get(entityType));
        } else {
            item = new ItemStack(Material.valueOf(SCPlugin.materials.get(entityType)));
        }

        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + WordUtils.capitalizeFully(entityType.name().replaceAll("_", " ")));

        itemMeta.setLore(new Color.Builder(entityLore)
                .replace("%amount%", String.valueOf(entityAmount))
                .replaceWithCurrency("%worth%", String.valueOf(getTotalWorth()))
                .buildLore());
        item.setItemMeta(itemMeta);
        return item;
    }

    /** Returns total worth (double) of the mobs collected */
    public double getTotalWorth() {
        return Math.round(SCPlugin.prices.get(entityType) * entityAmount * 100.0) / 100.0;
    }

    /** Getters */
    public EntityType getEntityType() {
        return entityType;
    }
    public int getEntityAmount() {
        return entityAmount;
    }
    public int getSpawnerAmount() {
        return spawners.size();
    }
}
