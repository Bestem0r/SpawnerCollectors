package me.bestem0r.spawnercollectors.utilities;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Methods {


    /** Creates new collector file with default values */
    public static Collector createCollector(Player player) {
        String uuid = player.getUniqueId().toString();
        File file = new File(Bukkit.getPluginManager().getPlugin("SpawnerCollectors").getDataFolder() + "/collectors/" + uuid + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("owner_uuid", uuid);
        config.set("auto_sell", false);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collector collector = new Collector(file);
        SCPlugin.collectors.add(collector);
        return collector;
    }

    /** Returns collector based on Player */
    public static Collector getCollector(Player player) {
        for (Collector collector : SCPlugin.collectors) {
            if (collector.getOwner().getUniqueId() == player.getUniqueId()) {
                return collector;
            }
        }
        return createCollector(player);
    }


    /** Get loot from entity type */
    public static ArrayList<ItemStack> lootFromType(EntityType entityType, Player player, int amount) {
        ArrayList<ItemStack> loot = new ArrayList<>();
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
}
