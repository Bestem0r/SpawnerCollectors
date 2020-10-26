package me.bestem0r.spawnercollectors.utilities;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import org.bukkit.entity.Player;

public abstract class Methods {

    /** Returns collector based on Player */
    public Collector getCollector(Player player) {
        for (Collector collector : SCPlugin.collectors) {
            if (collector.getOwner().getUniqueId() == player.getUniqueId()) {
                return collector;
            }
        }
        return null;
    }
}
