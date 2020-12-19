package me.bestem0r.spawnercollectors.utils;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.loot.ItemLoot;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Methods {

    private Methods() {}

    /** Returns collector based on Player */
    public static Collector getCollector(SCPlugin plugin, Player player) {
        for (Collector collector : plugin.collectors) {
            if (collector.getOwner().getUniqueId() == player.getUniqueId()) {
                return collector;
            }
        }
        return new Collector(plugin, player);
    }


    /** Get loot from entity type */
    public static List<ItemStack> lootFromType(SCPlugin plugin, EntityType entityType, Player player, int amount) {
        if (plugin.isUsingCustomLoot() && plugin.getCustomLoot().containsKey(entityType)) {
            return lootFromCustom(plugin, entityType, amount);
        } else {
            if (XMaterial.isNewVersion()) {
                return lootFromVanilla(entityType, player, amount);
            } else {
                Bukkit.getLogger().severe("[SpawnerCollectors] Auto-generated loot is not supported for versions below 1.13! Please enable and use custom loot in config!");
                player.sendMessage(ChatColor.RED + "[SpawnerCollectors] Unable to auto-generate loot for versions below 1.13! Please contact an administrator and check the console!");
                return new ArrayList<>();
            }
        }
    }

    /** Generates loot from custom loot tables */
    private static List<ItemStack> lootFromCustom(SCPlugin plugin, EntityType entityType, int amount) {
        List<ItemStack> loot = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            for (ItemLoot itemLoot : plugin.getCustomLoot().get(entityType)) {
                itemLoot.getRandomLoot().ifPresent((loot::add));
            }
        }
        return loot;
    }

    /** Generates loot from vanilla loot tables */
    private static List<ItemStack> lootFromVanilla(EntityType entityType, Player player, int amount) {
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

    /** Returns spawner with set EntityType */
    public static ItemStack spawnerFromType(SCPlugin plugin, EntityType entityType, int amount) {
        ItemStack itemStack = new ItemStack(XMaterial.SPAWNER.parseMaterial(), amount);

        //Lots of casting...
        ItemMeta itemMeta = itemStack.getItemMeta();
        BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
        BlockState blockState = blockStateMeta.getBlockState();
        CreatureSpawner spawner = (CreatureSpawner) blockState;
        spawner.setSpawnedType(entityType);
        blockStateMeta.setBlockState(blockState);

        String entityName = WordUtils.capitalizeFully(entityType.name().replaceAll("_", " "));
        itemMeta.setDisplayName(new ColorBuilder(plugin).path("spawner_withdraw_name").replace("%entity%", entityName).build());

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    /** Plays sound for Player */
    public static void playSound(SCPlugin plugin, Player player, String soundPath) {
        FileConfiguration config = plugin.getConfig();
        XSound xSound = XSound.matchXSound(config.getString("sounds." + soundPath)).orElse(XSound.UI_BUTTON_CLICK);
        player.playSound(player.getLocation(), xSound.parseSound(), 1, 1);
    }

    /** Returns ItemStack from config */
    public static ItemStack itemFromConfig(SCPlugin plugin, String path) {
        FileConfiguration config = plugin.getConfig();
        XMaterial xMaterial = XMaterial.matchXMaterial(config.getString(path + ".material")).orElse(XMaterial.STONE);
        ItemStack itemStack = xMaterial.parseItem();
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(new ColorBuilder(plugin).path(path + ".name").build());
        itemMeta.setLore((new ColorBuilder(plugin).path(path + ".lore")).buildLore());
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }
}
