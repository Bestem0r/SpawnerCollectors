package me.bestem0r.spawnercollectors.loot;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.ItemBuilder;
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

    private final List<ItemCompression> itemCompressions = new ArrayList<>();

    private boolean useCustomLoot = false;
    private boolean compressItems = false;

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
        loadCompressions();
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

                    ItemStack item = new ItemBuilder(items.getConfigurationSection(itemID)).build();

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

    private void loadCompressions() {
        itemCompressions.clear();

        File lootFile = new File(plugin.getDataFolder(), "loot.yml");
        FileConfiguration lootConfig = YamlConfiguration.loadConfiguration(lootFile);

        this.compressItems = lootConfig.getBoolean("item_compression.enable");
        if (!compressItems) {
            return;
        }

        ConfigurationSection itemsSection = lootConfig.getConfigurationSection("item_compression.items");
        if (itemsSection != null) {
            for (String itemKey : itemsSection.getKeys(false)) {
                Material material = Material.valueOf(itemKey);
                ConfigurationSection compressionSection = itemsSection.getConfigurationSection(itemKey);

                for (String amountKey : compressionSection.getKeys(false)) {
                    int amount = Integer.parseInt(amountKey);
                    ConfigurationSection compressedItemSection = compressionSection.getConfigurationSection(amountKey);

                    SimpleItem itemForCompression = new SimpleItem(material.name());
                    ItemStack compressedItem = new ItemBuilder(compressedItemSection).build();

                    ItemCompression itemCompression = new ItemCompression(itemForCompression, amount, compressedItem);
                    itemCompressions.add(itemCompression);
                }
            }
        }
    }

    public List<ItemStack> compressItems(List<ItemStack> items) {

        if (!compressItems) {
            return items;
        }

        Map<SimpleItem, Integer> mergedItems = new HashMap<>();
        Map<SimpleItem, ItemStack> rawItems = new HashMap<>();

        for (ItemStack item : items) {
            SimpleItem simpleItem = new SimpleItem(item);
            mergedItems.put(simpleItem, mergedItems.getOrDefault(simpleItem, 0) + item.getAmount());
            rawItems.put(simpleItem, item);
        }

        List<ItemStack> compressedItems = new ArrayList<>();
        for (Map.Entry<SimpleItem, Integer> entry : mergedItems.entrySet()) {
            SimpleItem simpleItem = entry.getKey();
            int totalAmount = entry.getValue();

            int remaining = totalAmount;
            for (ItemCompression compression : itemCompressions) {
                if (!compression.shouldCompress(simpleItem, totalAmount)) {
                    continue;
                }
                int compressionAmount = compression.getAmount();
                int compressedAmount = compressionAmount == 0 ? 0 : totalAmount / compressionAmount;
                if (compressedAmount > 0) {
                    addSplitStacks(compressedItems, compression.getCompressedItem(), compressedAmount);
                }
                remaining = totalAmount % compressionAmount;
            }

            if (remaining > 0) {
                addSplitStacks(compressedItems,rawItems.get(simpleItem), remaining);
            }
        }

        return compressedItems;
    }

    private void addSplitStacks(List<ItemStack> items, ItemStack item, int amount) {
        int stacks = amount / item.getMaxStackSize();
        int leftOver = amount % item.getMaxStackSize();

        for (int i = 0; i < stacks; i++) {
            ItemStack clone = item.clone();
            clone.setAmount(item.getMaxStackSize());
            items.add(clone);
        }
        if (leftOver > 0) {
            ItemStack clone = item.clone();
            clone.setAmount(leftOver);
            items.add(clone);
        }
    }

    /** Get loot from entity type */
    public List<ItemStack> lootFromType(CustomEntityType type, Player player, long amount, boolean compress) {
        List<ItemStack> loot;
        if (useCustomLoot && plugin.getLootManager().getCustomLoot().containsKey(type.name())) {
            loot =  lootFromCustom(plugin, type, amount);
        } else {
            if (VersionUtils.getMCVersion() >= 14 && !type.isCustom()) {
                loot = lootFromVanilla(type.getEntityType(), player, amount);
            } else {
                Bukkit.getLogger().severe("[SpawnerCollectors] Auto-generated loot is not supported for versions below 1.14! Please enable and use custom loot in config!");
                player.sendMessage(ChatColor.RED + "[SpawnerCollectors] Unable to auto-generate loot for versions below 1.14! Please contact an administrator and check the console!");
                loot = new ArrayList<>();
            }
        }

        if (compress) {
            loot = compressItems(loot);
        }
        return loot;
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
            } else if (entityType == EntityType.SLIME) {
                int random = (int) (Math.random() * 4 + 1);
                if (random == 1) {
                    loot.add(new ItemStack(Material.SLIME_BALL));
                }
            } else if (entityType == EntityType.SHEEP) {
                if (VersionUtils.getMCVersion() < 13) {
                    loot.add(new ItemStack(Material.valueOf("WOOL"), 1, (short) 1));
                } else {
                    loot.add(new ItemStack(Material.valueOf("WHITE_WOOL"), 1));
                }
                int random = (int) (Math.random() * 2 + 1);
                loot.add(new ItemStack(Material.COOKED_MUTTON, random));
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

    public boolean isCompressItems() {
        return compressItems;
    }

    public boolean isRegistered(String name) {
        return prices.containsKey(name);
    }
}
