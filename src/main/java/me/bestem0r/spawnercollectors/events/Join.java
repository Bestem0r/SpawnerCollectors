package me.bestem0r.spawnercollectors.events;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.DataStoreMethod;
import me.bestem0r.spawnercollectors.SCPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;

public class Join implements Listener {

    private final SCPlugin plugin;

    public Join(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.collectors.add(new Collector(plugin, event.getPlayer()));
    }
}
