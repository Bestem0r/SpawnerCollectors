package me.bestem0r.spawnercollectors.events;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;

public class Join implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        File file = new File(Bukkit.getPluginManager().getPlugin("SpawnerCollectors").getDataFolder() + "/collectors/" + uuid + ".yml");
        if (file.exists()) {
            SCPlugin.collectors.add(new Collector(file));
        }
    }
}
