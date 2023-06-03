package me.bestem0r.spawnercollectors.placeholders;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.collector.EntityCollector;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {
    private final SCPlugin plugin;

    public PlaceholderAPIExpansion(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public String getIdentifier() {
        return "spawnercollectors";
    }

    @Override
    public String getAuthor() {
        return "bestem0r";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        // spawnercollectors_amount[_<entity-type>]
        if (params.startsWith("amount")) {
            Collector collector = SpawnerUtils.getCollector(this.plugin, player);

            // global spawners amount
            if (params.equals("amount")) {
                int amount = 0;

                for (EntityCollector entityCollector : collector.getCollectorEntities()) {
                    amount += entityCollector.getSpawnerAmount();
                }

                return String.valueOf(amount);
            }

            // per-mob spawners amount
            CustomEntityType entityType = new CustomEntityType(params.substring(7));
            Optional<EntityCollector> entityCollector = collector.getCollectorEntities().stream().filter((c) -> c.getEntityType().name().equals(entityType.name())).findAny();

            return entityCollector.map(value -> String.valueOf(value.getSpawnerAmount())).orElse("0");
        }

        // spawnercollectors_limit[_<entity-type>]
        if (params.startsWith("limit")) {
            // global max limit
            if (params.equals("limit")) {
                return String.valueOf(this.plugin.getMaxSpawners());
            }

            // per-mob permission limit
            CustomEntityType entityType = new CustomEntityType(params.substring(6));
            player.sendMessage(entityType.name());
            return String.valueOf(SpawnerUtils.getMaxSpawners(player, entityType));
        }

        return null;
    }
}
