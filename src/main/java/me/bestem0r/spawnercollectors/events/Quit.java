package me.bestem0r.spawnercollectors.events;

import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
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
        Collector collector = plugin.collectors.get(event.getPlayer().getUniqueId());
        plugin.collectors.remove(event.getPlayer().getUniqueId());
        collector.saveAsync();
    }
}
