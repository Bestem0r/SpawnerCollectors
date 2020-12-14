package me.bestem0r.spawnercollectors.events;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import org.bukkit.Bukkit;
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
        String uuid = event.getPlayer().getUniqueId().toString();
        File file = new File(Bukkit.getPluginManager().getPlugin("SpawnerCollectors").getDataFolder() + "/collectors/" + uuid + ".yml");
        if (file.exists()) {
            plugin.collectors.add(new Collector(plugin, file));
        }
    }
}
