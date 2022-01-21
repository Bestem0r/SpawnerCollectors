package me.bestem0r.spawnercollectors.loot;

import com.cryptomorin.xseries.XMaterial;
import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.ConfigManager;
import me.bestem0r.spawnercollectors.utils.EntityBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

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

    public LootManager(SCPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void load() {
        prices.clear();
        materials.clear();

        loadCustomLoot();
        loadEntities();
    }

    /** Loads custom loot tables from config */
    private void loadCustomLoot() {

        customLoot.clear();
        customXP.clear();
        if (!ConfigManager.getBoolean(("custom_loot_tables.enable"))) { return; }

        ConfigurationSection mobs = plugin.getConfig().getConfigurationSection("custom_loot_tables.mobs");
        if (mobs != null) {
            for (String mob : mobs.getKeys(false)) {

                ConfigurationSection items = plugin.getConfig().getConfigurationSection("custom_loot_tables.mobs." + mob);
                for (String item : items.getKeys(false)) {

                    Material material = XMaterial.matchXMaterial(item).orElse(XMaterial.STONE).parseMaterial();
                    //Bukkit.getLogger().info("Loaded " + material);
                    double probability = plugin.getConfig().getDouble("custom_loot_tables.mobs." + mob + "." + item + ".probability");
                    int min = plugin.getConfig().getInt("custom_loot_tables.mobs." + mob + "." + item + ".min");
                    int max = plugin.getConfig().getInt("custom_loot_tables.mobs." + mob + "." + item + ".max");

                    List<ItemLoot> loot = (customLoot.containsKey(mob) ? customLoot.get(mob) : new ArrayList<>());
                    loot.add(new ItemLoot(material, probability, min, max));
                    customLoot.put(mob, loot);
                }
            }
        }
        ConfigurationSection xp = plugin.getConfig().getConfigurationSection("custom_xp.mobs");
        if (xp != null) {
            for (String mob : xp.getKeys(false)) {
                customXP.put(mob, plugin.getConfig().getInt("custom_xp.mobs." + mob));
            }
        }
    }

    /** Load entity prices and material strings */
    private void loadEntities() {
        ConfigurationSection priceSection = plugin.getConfig().getConfigurationSection("prices");
        if (priceSection == null) { return; }
        for (String entity : priceSection.getKeys(false)) {
            prices.put(entity, plugin.getConfig().getDouble("prices." + entity));
        }
        ConfigurationSection materialSection = plugin.getConfig().getConfigurationSection("materials");
        if (materialSection == null) { return; }
        for (String entity : materialSection.getKeys(false)) {
            materials.put(entity, plugin.getConfig().getString("materials." + entity));
        }
    }

    /** Get loot from entity type */
    public List<ItemStack> lootFromType(CustomEntityType type, Player player, long amount) {
        if (ConfigManager.getBoolean(("custom_loot_tables.enable")) && plugin.getLootManager().getCustomLoot().containsKey(type.name())) {
            return lootFromCustom(plugin, type, amount);
        } else {
            if (XMaterial.isNewVersion() && !type.isCustom()) {
                return lootFromVanilla(type.getEntityType(), player, amount);
            } else {
                Bukkit.getLogger().severe("[SpawnerCollectors] Auto-generated loot is not supported for versions below 1.13! Please enable and use custom loot in config!");
                player.sendMessage(ChatColor.RED + "[SpawnerCollectors] Unable to auto-generate loot for versions below 1.13! Please contact an administrator and check the console!");
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

        location.setY(location.getY() - 5);
        for (int i = 0; i < amount; i++) {
            Entity entity = EntityBuilder.createEntity(entityType, location);

            if (entityType == EntityType.MAGMA_CUBE) {
                int random = (int) (Math.random() * 4 + 1);
                if (random == 1) {
                    loot.add(new ItemStack(Material.MAGMA_CREAM));
                    continue;
                }
            }

            Lootable lootable = (Lootable) entity;
            LootTable lootTable = lootable.getLootTable();

            if (lootTable == null) { return new ArrayList<>(); }
            LootContext.Builder contextBuilder = new LootContext.Builder(location).lootedEntity(entity).killer(player);

            entity.remove();
            LootContext context = contextBuilder.build();
            loot.addAll(lootTable.populateLoot(ThreadLocalRandom.current(), context));
        }
        return loot;
    }


    public Map<String, Double> getPrices() {
        return prices;
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
}
