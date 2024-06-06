package me.bestem0r.spawnercollectors.loot;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager {

    private final SCPlugin plugin;

    private final Map<String, Double> prices = new HashMap<>();
    private final Map<String, String> materials = new HashMap<>();

    private final Map<String, List<ItemLoot>> customLoot = new HashMap<>();
    private final Map<String, Integer> customXP = new HashMap<>();

    private boolean useCustomLoot = false;

    public LootManager(SCPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void load() {
        prices.clear();
        materials.clear();

        File lootFile = new File(plugin.getDataFolder(), "loot.yml");
        FileConfiguration lootConfig = YamlConfiguration.loadConfiguration(lootFile);
        useCustomLoot = lootConfig.getBoolean("custom_loot_tables.enable");

        loadCustomLoot();
        loadEntities();
    }

    /** Loads custom loot tables from config */
    private void loadCustomLoot() {

        customLoot.clear();
        customXP.clear();
        if (!useCustomLoot) { return; }

        File lootFile = new File(plugin.getDataFolder(), "loot.yml");
        FileConfiguration lootConfig = YamlConfiguration.loadConfiguration(lootFile);

        ConfigurationSection mobs = lootConfig.getConfigurationSection("custom_loot_tables.mobs");
        if (mobs != null) {
            for (String mob : mobs.getKeys(false)) {

                ConfigurationSection items = lootConfig.getConfigurationSection("custom_loot_tables.mobs." + mob);
                if (items == null) { continue; }
                for (String itemID : items.getKeys(false)) {

                    ItemStack item;
                    if (itemID.contains(":")) {
                        String[] split = itemID.split(":");
                        item = new ItemStack(Material.valueOf(split[0]), Short.parseShort(split[1]));
                    } else {
                        item = new ItemStack(Material.valueOf(itemID));
                    }
                    double probability = lootConfig.getDouble("custom_loot_tables.mobs." + mob + "." + itemID + ".probability");
                    int min = lootConfig.getInt("custom_loot_tables.mobs." + mob + "." + itemID + ".min");
                    int max = lootConfig.getInt("custom_loot_tables.mobs." + mob + "." + itemID + ".max");

                    List<ItemLoot> loot = (customLoot.containsKey(mob) ? customLoot.get(mob) : new ArrayList<>());
                    loot.add(new ItemLoot(item, probability, min, max));
                    customLoot.put(mob, loot);
                }
            }
        }
        ConfigurationSection xp = lootConfig.getConfigurationSection("custom_xp.mobs");
        if (xp != null) {
            for (String mob : xp.getKeys(false)) {
                customXP.put(mob, lootConfig.getInt("custom_xp.mobs." + mob));
            }
        }
    }

    /** Load entity prices and material strings */
    private void loadEntities() {
        File mobsFile = new File(plugin.getDataFolder(), "mobs.yml");
        FileConfiguration mobsConfig = YamlConfiguration.loadConfiguration(mobsFile);
        ConfigurationSection mobsSection = mobsConfig.getConfigurationSection("mobs");
        if (mobsSection == null) { return; }
        for (String entity : mobsSection.getKeys(false)) {
            prices.put(entity, mobsConfig.getDouble("mobs." + entity + ".price"));
            materials.put(entity, mobsConfig.getString("mobs." + entity + ".material"));
        }
    }

    /** Get loot from entity type */
    public List<ItemStack> lootFromType(CustomEntityType type, Player player, long amount) {
        if (useCustomLoot && plugin.getLootManager().getCustomLoot().containsKey(type.name())) {
            return lootFromCustom(plugin, type, amount);
        } else {
            if (VersionUtils.getMCVersion() >= 14 && !type.isCustom()) {
                return lootFromVanilla(type.getEntityType(), player, amount);
            } else {
                Bukkit.getLogger().severe("[SpawnerCollectors] Auto-generated loot is not supported for versions below 1.14! Please enable and use custom loot in config!");
                player.sendMessage(ChatColor.RED + "[SpawnerCollectors] Unable to auto-generate loot for versions below 1.14! Please contact an administrator and check the console!");
                return new ArrayList<>();
            }
        }
    }

    /** Generates loot from custom loot tables */
    private List<ItemStack> lootFromCustom(SCPlugin plugin, CustomEntityType type, long amount) {
        List<ItemStack> loot = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            for (ItemLoot itemLoot : plugin.getLootManager().getCustomLoot().get(type.name())) {
                itemLoot.getRandomLoot().ifPresent((loot::add));
            }
        }
        loot.removeIf(i -> i.getAmount() < 1);
        return loot;
    }

    /** Generates loot from vanilla loot tables */
    private List<ItemStack> lootFromVanilla(EntityType entityType, Player player, long amount) {
        List<ItemStack> loot = new ArrayList<>();
        Location location = player.getLocation();

        ItemStack handItem = player.getInventory().getItemInHand().clone();
        if (!ConfigManager.getBoolean("spawner.enable_looting_enchantment")) {
            player.setItemInHand(null);
            player.updateInventory();
        }
        for (int i = 0; i < amount; i++) {

            if (entityType == EntityType.MAGMA_CUBE) {
                int random = (int) (Math.random() * 4 + 1);
                if (random == 1) {
                    loot.add(new ItemStack(Material.MAGMA_CREAM));
                }
            } else {
                LootTables lootTables = LootTables.valueOf(entityType.name());
                LootTable lootTable = lootTables.getLootTable();

                LootContext.Builder contextBuilder = new LootContext.Builder(location).lootedEntity(player).killer(player);
                LootContext context = contextBuilder.build();
                loot.addAll(lootTable.populateLoot(ThreadLocalRandom.current(), context));
            }
        }
        if (!ConfigManager.getBoolean("spawner.enable_looting_enchantment")) {
            player.setItemInHand(handItem);
            player.updateInventory();
        }
        return loot;
    }

    public double getPrice(String entity) {
        return prices.getOrDefault(entity, 0.0);
    }

    public double getPrice(String entity, OfflinePlayer player) {
        if (player != null && player.isOnline()) {
            return getPrice(entity, player.getPlayer());
        } else {
            return getPrice(entity);
        }
    }


    public double getPrice(String entity, Player player) {
        if (player != null) {
            double boost = player.getEffectivePermissions().stream()
                    .filter(perm -> perm.getPermission().startsWith("spawnercollectors.boost."))
                    .map(perm -> perm.getPermission().replace("spawnercollectors.boost.", ""))
                    .mapToDouble(Double::parseDouble)
                    .max().orElse(1);

            return prices.getOrDefault(entity, 0.0) * (boost);
        }
        return prices.getOrDefault(entity, 0.0);
    }

    public Map<String, String> getMaterials() {
        return materials;
    }

    public Map<String, List<ItemLoot>> getCustomLoot() {
        return customLoot;
    }
    public Map<String, Integer> getCustomXP() {
        return customXP;
    }

    public boolean isUseCustomLoot() {
        return useCustomLoot;
    }

    public boolean isRegistered(String name) {
        return prices.containsKey(name);
    }
}
