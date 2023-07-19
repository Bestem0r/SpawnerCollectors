package me.bestem0r.spawnercollectors.collector;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;

public class PlacedCollector extends Collector {

    private final Location location;

    public PlacedCollector(SCPlugin plugin, Location location, OfflinePlayer owner) {
        super(plugin, SpawnerUtils.locationToBase64(location), owner);
        this.location = location;
    }

    @Override
    public boolean addSpawner(Player player, CustomEntityType type, int amount) {

        Block block = location.getBlock();
        if (block.isEmpty()) {
            return true;
        }
        if (!block.getType().name().contains("SPAWNER")) {
            return false;
        }
        if (!(block.getState() instanceof CreatureSpawner)) {
            return false;
        }
        CreatureSpawner spawner = (CreatureSpawner) block.getState();
        if (spawner.getSpawnedType() != type.getEntityType()) {
            return false;
        }

        return super.addSpawner(player, type, amount);
    }

    @Override
    public boolean isSingleEntity() {
        return true;
    }
}
