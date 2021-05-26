package me.bestem0r.spawnercollectors;

import com.cryptomorin.xseries.XMaterial;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.bestem0r.spawnercollectors.utils.ColorBuilder;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EntityCollector {

    private final EntityType entityType;

    private final List<CollectedSpawner> spawners = new ArrayList<>();
    private int entityAmount;

    private final List<String> spawnerLore;
    private final List<String> entityLore;

    private final SCPlugin plugin;

    public EntityCollector(SCPlugin plugin, EntityType entityType, int entityAmount, int spawnerAmount) {
        this.entityType = entityType;
        this.plugin = plugin;

        FileConfiguration mainConfig = plugin.getConfig();
        this.spawnerLore = mainConfig.getStringList("menus.spawners.item_lore");
        this.entityLore = mainConfig.getStringList("menus.mobs.item_lore");

        this.entityAmount = entityAmount;
        for (int i = 0; i  < spawnerAmount; i++) {
            spawners.add(new CollectedSpawner(plugin.getSpawnAmount(), plugin.getSpawnTimeMin(), plugin.getSpawnTimeMax()));
        }
    }

    /** Adds entities */
    public void attemptSpawn(boolean autoSell, OfflinePlayer player) {

        int spawned = spawners.stream().mapToInt(CollectedSpawner::attemptSpawn).sum();
        if (autoSell) {
            Economy economy = plugin.getEconomy();
            double worth = plugin.prices.get(entityType) * spawned;
            economy.depositPlayer(player, worth);
            plugin.addEarned(player, worth);
        } else {
            entityAmount += spawned;
        }
    }

    /** Adds spawner */
    public void addSpawner(int amount) {
        for (int i = 0; i < amount; i++) {
            spawners.add(new CollectedSpawner(plugin.getSpawnAmount(), plugin.getSpawnTimeMin(), plugin.getSpawnTimeMax()));
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
        ItemStack item = XMaterial.SPAWNER.parseItem();

        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.RESET + WordUtils.capitalizeFully(entityType.name().replaceAll("_", " ")));

        itemMeta.setLore(new ColorBuilder(plugin, spawnerLore).replace("%amount%", String.valueOf(spawners.size())).buildLore());
        item.setItemMeta(itemMeta);
        return item;
    }

    /** Returns ItemStack to display number of entities */
    public ItemStack getEntityItem() {
        try {
            ItemStack item;
            String material = plugin.materials.get(entityType);

            if (plugin.isUsingHeadDB() && material.startsWith("hdb:")) {
                item = new HeadDatabaseAPI().getItemHead(material.substring(4));
            } else {
                XMaterial xMaterial = XMaterial.matchXMaterial(material).orElse(XMaterial.STONE);
                item = xMaterial.parseItem();
            }

            ItemMeta itemMeta = item.getItemMeta();

            itemMeta.setDisplayName(ChatColor.RESET + WordUtils.capitalizeFully(entityType.name().replaceAll("_", " ")));

            itemMeta.setLore(new ColorBuilder(plugin, entityLore)
                    .replace("%amount%", String.valueOf(entityAmount))
                    .replaceWithCurrency("%worth%", String.valueOf(getTotalWorth()))
                    .buildLore());
            item.setItemMeta(itemMeta);
            return item;
        } catch (Exception e) {
            Bukkit.getLogger().severe("[SpawnerCollectors] Could not retrieve material for entity '" + entityType + "'! Is it registered in the config?");
            e.printStackTrace();
            return null;
        }

    }

    /** Returns total worth (double) of the mobs collected */
    public double getTotalWorth() {
        return Math.round(plugin.prices.get(entityType) * entityAmount * 100.0) / 100.0;
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
