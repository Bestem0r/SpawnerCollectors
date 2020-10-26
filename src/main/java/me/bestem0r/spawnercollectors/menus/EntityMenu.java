package me.bestem0r.spawnercollectors.menus;

import me.bestem0r.spawnercollectors.CollectedSpawner;
import me.bestem0r.spawnercollectors.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class EntityMenu {

    public static Inventory create(List<CollectedSpawner> collectedSpawners, boolean autoSell) {
        Inventory inventory = Bukkit.createInventory(null, 54, new Color.Builder().path("menus.mobs.title").build());
        ItemStack[] items = new ItemStack[54];

        for (int i = 0; i < collectedSpawners.size(); i++) {
            if (i == 54) { break; }
            items[i] = collectedSpawners.get(i).getEntityItem();
        }
        ItemStack filler =

        inventory.setContents(items);

        return inventory;
    }
}
