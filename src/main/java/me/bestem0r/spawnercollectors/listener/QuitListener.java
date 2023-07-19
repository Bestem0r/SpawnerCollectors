package me.bestem0r.spawnercollectors.listener;

import me.bestem0r.spawnercollectors.SCPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    private final SCPlugin plugin;

    public QuitListener(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCollectorManager().unload(event.getPlayer());
    }
}
