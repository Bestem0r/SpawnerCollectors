package me.bestem0r.spawnercollectors.collector;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.EntityExperience;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.loot.LootManager;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class EntityCollector {

    private final CustomEntityType entityType;

    private final List<CollectedSpawner> spawners = new ArrayList<>();
    private long entityAmount = 0;

    private final SCPlugin plugin;
    private final Collector collector;

    public EntityCollector(SCPlugin plugin, Collector collector, CustomEntityType entityType) {
        this.entityType = entityType;
        this.plugin = plugin;
        this.collector = collector;
    }

    /** Adds entities */
    public void attemptSpawn(boolean autoSell, OfflinePlayer player, double modifier) {

        long spawned = spawners.stream().mapToLong(CollectedSpawner::attemptSpawn).sum();
        spawned = (long) (spawned * modifier);

        if (autoSell && entityType != null && plugin.getLootManager().isRegistered(entityType.name())) {

            double worth = plugin.getLootManager().getPrice(entityType.name(), player) * spawned;
            if (worth > 0) {
                Economy economy = plugin.getEconomy();
                economy.depositPlayer(player, worth);
                plugin.getCollectorManager().addEarned(player, worth);
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

    public void addSpawners(int amount) {
        for (int i = 0; i < amount; i++) {
            spawners.add(new CollectedSpawner(plugin.getSpawnAmount(), plugin.getSpawnTimeMin(), plugin.getSpawnTimeMax()));
        }
    }

    public void setSpawners(int amount) {
        if (amount > spawners.size()) {
            addSpawners(amount - spawners.size());
        } else if (amount < spawners.size()) {
            removeSpawners(spawners.size() - amount);
        }
    }

    public void setEntityAmount(long entityAmount) {
        this.entityAmount = entityAmount;
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
    public ItemStack getEntityItem(OfflinePlayer owner) {
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
                    .replaceCurrency("%worth%", BigDecimal.valueOf(getTotalWorth(owner)))
                    .replaceCurrency("%avg_production%", getMinutelyProduction(owner))
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
    public double getTotalWorth(OfflinePlayer owner) {
        return Math.round(plugin.getLootManager().getPrice(entityType.name(), owner) * entityAmount * 100.0) / 100.0;
    }

    public double getWorth(OfflinePlayer owner, long amount) {
        return Math.round(plugin.getLootManager().getPrice(entityType.name(), owner) * amount * 100.0) / 100.0;
    }

    public void withdraw(Player player, long amount) {
        amount = Math.min(entityAmount, amount);
        removeEntities(amount);

        LootManager lootManager = plugin.getLootManager();
        for (ItemStack itemStack : lootManager.lootFromType(getEntityType(), player, amount)) {
            Map<Integer, ItemStack> drop = player.getInventory().addItem(itemStack);
            for (int i : drop.keySet()) {
                ItemStack item = drop.get(i);
                if (item != null && item.getType() != Material.AIR) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop.get(i));
                }
            }
        }
        if (ConfigManager.getBoolean("give_xp") && (!ConfigManager.getBoolean("more_permissions") || player.hasPermission("spawnercollectors.receive_xp"))) {

            int xp = 0;
            if (lootManager.isUseCustomLoot() && lootManager.getCustomXP().containsKey(getEntityType().name())) {
                xp = (int) (lootManager.getCustomXP().get(getEntityType().name()) * amount);
            } else {
                for (EntityExperience e : EntityExperience.values()) {
                    if (e.name().equals(getEntityType().name())) {
                        xp = e.getRandomAmount(amount);
                        break;
                    }
                }
            }

            if (ConfigManager.getBoolean("mending") && VersionUtils.getMCVersion() > 8) {
                List<ItemStack> mendable = new ArrayList<>();

                for (int i = 100; i < 104 && i < player.getInventory().getContents().length; i++) {
                    ItemStack item = player.getInventory().getContents()[i];
                    if (SpawnerUtils.canBeRepaired(item)) {
                        mendable.add(item);
                    }
                }
                ItemStack hand = player.getItemInHand();
                if (SpawnerUtils.canBeRepaired(hand)) {
                    mendable.add(hand);
                }

                if (!mendable.isEmpty()) {
                    for (int i = 0; i < amount && xp >= 2; i++) {
                        if (mendable.size() == 0) {
                            break;
                        }
                        ItemStack item = mendable.get(ThreadLocalRandom.current().nextInt(0, mendable.size()));
                        if (!SpawnerUtils.repair(item)) {
                            mendable.remove(item);
                        }
                        xp -= 2;
                    }
                }
                player.updateInventory();
            }
            if (xp >= 0) {
                player.giveExp(xp);
                Sound s = Sound.valueOf(VersionUtils.getMCVersion() > 8 ? "ENTITY_EXPERIENCE_ORB_PICKUP" : "ORB_PICKUP");
                player.playSound(player.getLocation(), s, 1.0F, 1.0F);
            }
        }
    }


    public void sell(Player player, long amount) {

        amount = Math.min(amount, getEntityAmount());
        boolean morePermissions = plugin.isMorePermissions();
        if (morePermissions && !player.hasPermission("spawnercollectors.sell")) {
            player.sendMessage(ConfigManager.getMessage("messages.no_permission_sell"));
            return;
        }
        Economy economy = plugin.getEconomy();
        double worth = getWorth(collector.getOwner(), amount);
        economy.depositPlayer(player, worth);

        if (getTotalWorth(collector.getOwner()) > 0) {
            SpawnerUtils.playSound(player, "sell");
            player.sendMessage(ConfigManager.getCurrencyBuilder("messages.sell")
                    .replaceCurrency("%worth%", BigDecimal.valueOf(worth))
                    .addPrefix()
                    .build());
        }

        removeEntities(amount);

        collector.updateEntityMenuIfView();
        collector.updateSpawnerMenuIfView();
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

    public BigDecimal getMinutelyProduction(OfflinePlayer owner) {
        int spawnerAmount = spawners.size();
        double avgTime = 60d / ((plugin.getSpawnTimeMax() + plugin.getSpawnTimeMin()) / 2.0d);
        double avgSpawns = plugin.getSpawnAmount() * spawnerAmount * avgTime;
        double avgMoney = plugin.getLootManager().getPrice(entityType.name(), owner) * avgSpawns;
        return BigDecimal.valueOf(avgMoney);
    }
}
