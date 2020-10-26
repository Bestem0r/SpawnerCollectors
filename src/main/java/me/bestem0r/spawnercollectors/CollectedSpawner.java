package me.bestem0r.spawnercollectors;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CollectedSpawner {

    private final EntityType entityType;

    private int spawnerAmount = 0;
    private int entityAmount = 0;

    private final List<String> spawnerLore;
    private final List<String> entityLore;

    private Instant nextSpawn;

    public CollectedSpawner(EntityType entityType, int entityAmount, int spawnerAmount) {
        this.entityType = entityType;

        FileConfiguration mainConfig = SCPlugin.getInstance().getConfig();
        this.spawnerLore = mainConfig.getStringList("menus.spawners.item_lore");
        this.entityLore = mainConfig.getStringList("menus.mobs.item_lore");

        this.entityAmount = entityAmount;
        this.spawnerAmount = spawnerAmount;

        this.nextSpawn = Instant.now().plusSeconds(ThreadLocalRandom.current().nextInt(0, 41));
    }

    /** Adds entities */
    public void attemptSpawn() {
        if (Instant.now().isAfter(nextSpawn)) {
            entityAmount = entityAmount + spawnerAmount * 4;
            this.nextSpawn = Instant.now().plusSeconds(ThreadLocalRandom.current().nextInt(0, 41));
        }
    }

    /** Adds spawner */
    public void addSpawner(int amount) {
        spawnerAmount = spawnerAmount = amount;
    }

    /** Returns ItemStack to display number of spawners */
    public ItemStack getSpawnerItem() {
        ItemStack item = new ItemStack(Material.SPAWNER);

        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(WordUtils.capitalizeFully(entityType.name().replaceAll("_", " ")));
        List<String> lore = new ArrayList<>();
        for (String line : spawnerLore) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line
                    .replace("%amount%", String.valueOf(spawnerAmount))));
        }
        itemMeta.setLore(lore);
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
        itemMeta.setDisplayName(WordUtils.capitalizeFully(entityType.name().replaceAll("_", " ")));
        List<String> lore = new ArrayList<>();
        for (String line : entityLore) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line
                    .replace("%amount%", String.valueOf(entityAmount))));
        }
        itemMeta.setLore(lore);
        return item;
    }

    /** Getters */
    public EntityType getEntityType() {
        return entityType;
    }
    public int getEntityAmount() {
        return entityAmount;
    }
    public int getSpawnerAmount() {
        return spawnerAmount;
    }
}
