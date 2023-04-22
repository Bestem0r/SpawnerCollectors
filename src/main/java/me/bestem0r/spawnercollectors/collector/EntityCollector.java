package me.bestem0r.spawnercollectors.collector;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.config.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EntityCollector {

    private final CustomEntityType entityType;

    private final List<CollectedSpawner> spawners = new ArrayList<>();
    private long entityAmount;

    private final SCPlugin plugin;

    public EntityCollector(SCPlugin plugin, CustomEntityType entityType, long entityAmount, int spawnerAmount) {
        this.entityType = entityType;
        this.plugin = plugin;

        this.entityAmount = entityAmount;
        for (int i = 0; i  < spawnerAmount; i++) {
            spawners.add(new CollectedSpawner(plugin.getSpawnAmount(), plugin.getSpawnTimeMin(), plugin.getSpawnTimeMax()));
        }
    }

    /** Adds entities */
    public void attemptSpawn(boolean autoSell, OfflinePlayer player) {

        long spawned = spawners.stream().mapToLong(CollectedSpawner::attemptSpawn).sum();

        if (autoSell && entityType != null && plugin.getLootManager().getPrices().containsKey(entityType.name())) {

            double worth = plugin.getLootManager().getPrices().get(entityType.name()) * spawned;
            if (worth > 0) {
                Economy economy = plugin.getEconomy();
                economy.depositPlayer(player, worth);
                plugin.addEarned(player, worth);
            }
        } else {
            int maxConfig = plugin.getConfig().getInt("max_mobs");
            int maxPermission = SpawnerUtils.getMaxMobs(player, entityType);

            int max = (maxPermission < 0 ? maxConfig : Math.max(maxPermission, maxConfig));
            if (max > 0 && (entityAmount + spawned) > max) {
                entityAmount = max;
            } else {
                entityAmount += spawned;
            }
        }
    }

    /** Adds spawner */
    public void addSpawner(int amount) {
        for (int i = 0; i < amount; i++) {
            spawners.add(new CollectedSpawner(plugin.getSpawnAmount(), plugin.getSpawnTimeMin(), plugin.getSpawnTimeMax()));
        }
    }

    /** Removes entities */
    public void removeEntities(long amount) {
        entityAmount -= amount;
    }
    /** Removes spawners */
    public void removeSpawners(int amount) {
        if (amount > 0) {
            spawners.subList(0, amount).clear();
        }
    }

    /** Returns ItemStack to display number of spawners */
    public ItemStack getSpawnerItem() {
        ItemStack item = SpawnerUtils.spawnerFromType(entityType, (int) Math.min(Math.max(getSpawnerAmount(), 1), 64), plugin);

        ItemMeta itemMeta = item.getItemMeta();

        itemMeta.setDisplayName(ChatColor.RESET + WordUtils.capitalizeFully(entityType.name().replaceAll("_", " ")));

        itemMeta.setLore(ConfigManager.getListBuilder("menus.spawners.item_lore").replace("%amount%", String.valueOf(spawners.size())).build());
        item.setItemMeta(itemMeta);
        return item;
    }

    /** Returns ItemStack to display number of entities */
    public ItemStack getEntityItem() {
        try {
            ItemStack item;
            String material = plugin.getLootManager().getMaterials().get(entityType.name());

            if (plugin.isUsingHeadDB() && material.startsWith("hdb:")) {
                item = new HeadDatabaseAPI().getItemHead(material.substring(4));
            } else {
                if (material.contains(":")) {
                    String[] split = material.split(":");
                    item = new ItemStack(Material.valueOf(split[0]), Short.parseShort(split[1]));
                } else {
                    item = new ItemStack(Material.valueOf(material));
                }
            }

            ItemMeta itemMeta = item.getItemMeta();

            itemMeta.setDisplayName(ChatColor.RESET + WordUtils.capitalizeFully(entityType.name().replaceAll("_", " ")));

            itemMeta.setLore(ConfigManager.getListBuilder("menus.mobs.item_lore")
                    .replace("%amount%", String.valueOf(entityAmount))
                    .replaceCurrency("%worth%", BigDecimal.valueOf(getTotalWorth()))
                    .replaceCurrency("%avg_production%", getMinutelyProduction())
                    .build());

            item.setAmount((int) Math.min(Math.max(entityAmount, 1), 64));
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
        return Math.round(plugin.getLootManager().getPrices().getOrDefault(entityType.name(), (double) 0) * entityAmount * 100.0) / 100.0;
    }

    public void clear() {
        entityAmount = 0;
    }

    public CustomEntityType getEntityType() {
        return entityType;
    }
    public long getEntityAmount() {
        return entityAmount;
    }
    public int getSpawnerAmount() {
        return spawners.size();
    }

    public BigDecimal getMinutelyProduction() {
        int spawnerAmount = spawners.size();
        double avgTime = 60d / ((plugin.getSpawnTimeMax() + plugin.getSpawnTimeMin()) / 2.0d);
        double avgSpawns = plugin.getSpawnAmount() * spawnerAmount * avgTime;
        double avgMoney = plugin.getLootManager().getPrices().getOrDefault(entityType.name(), (double) 0) * avgSpawns;
        return BigDecimal.valueOf(avgMoney);
    }
}
