package me.bestem0r.spawnercollectors.events;

import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class Quit implements Listener {

    private final SCPlugin plugin;

    public Quit(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Collector collector = SpawnerUtils.getCollector(plugin, event.getPlayer());
        plugin.collectors.remove(collector);
        collector.saveAsync();
    }
}
