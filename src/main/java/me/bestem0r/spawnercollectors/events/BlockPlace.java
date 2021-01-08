package me.bestem0r.spawnercollectors.events;

import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.ColorBuilder;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlace implements Listener {
    private final SCPlugin plugin;

    public BlockPlace(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (this.plugin.isDisablePlace() && event.getBlock().getType() == Material.SPAWNER && !event.getPlayer().hasPermission("spawnercollectors.bypass_place")) {
            event.getPlayer().sendMessage((new ColorBuilder(this.plugin)).path("messages.no_permission_place_spawner").addPrefix().build());
            event.setCancelled(true);
        }

    }
}
