package me.bestem0r.spawnercollectors.listener;

import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final SCPlugin plugin;

    public JoinListener(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.collectors.put(event.getPlayer().getUniqueId(), new Collector(plugin, event.getPlayer().getUniqueId()));
    }
}
