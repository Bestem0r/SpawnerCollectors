package me.bestem0r.spawnercollectors.events;

import com.cryptomorin.xseries.XMaterial;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.ColorBuilder;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlace implements Listener {

    private final SCPlugin plugin;
    private final XMaterial spawner = XMaterial.SPAWNER;

    public BlockPlace(SCPlugin plugin) {
        this.plugin = plugin;


    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        XMaterial mat = XMaterial.matchXMaterial(event.getBlock().getType());
        if (this.plugin.isDisablePlace() && mat == spawner && !event.getPlayer().hasPermission("spawnercollectors.bypass_place")) {
            event.getPlayer().sendMessage((new ColorBuilder(this.plugin)).path("messages.no_permission_place_spawner").addPrefix().build());
            event.setCancelled(true);
        }

    }
}
