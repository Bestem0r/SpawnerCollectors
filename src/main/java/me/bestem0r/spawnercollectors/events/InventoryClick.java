package me.bestem0r.spawnercollectors.events;

import me.bestem0r.spawnercollectors.Collector;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryClick implements Listener {

    public enum Menu {
        SPAWNER,
        ENTITY
    }

    private final Collector collector;
    private final Player player;
    private final Menu menu;

    public InventoryClick(Collector collector, Player player, Menu menu) {
        this.collector = collector;
        this.player = player;
        this.menu = menu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (this.player != event.getWhoClicked()) { return; }

        switch (menu) {
            case ENTITY:
                event.setCancelled(true);
                collector.entityMenuInteract(event.getRawSlot(), player);
                break;
            case SPAWNER:
                event.setCancelled(true);
                collector.spawnerMenuInteract(event.getRawSlot(), player);
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        HandlerList.unregisterAll(this);
    }

}
