package me.bestem0r.spawnercollectors.menus;

import me.bestem0r.spawnercollectors.CollectorEntity;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class SpawnerMenu {

    public static Inventory create(List<CollectorEntity> collectorEntities, boolean autoSell) {
        Inventory inventory = Bukkit.createInventory(null, 54, new Color.Builder().path("menus.spawners.title").build());
        ItemStack[] items = new ItemStack[54];

        for (int i = 0; i < collectorEntities.size(); i++) {
            if (i == 54) { break; }
            items[i] = collectorEntities.get(i).getSpawnerItem();
        }

        FileConfiguration config = SCPlugin.getInstance().getConfig();

        ItemStack filler = new MenuItem.Builder(Material.valueOf(config.getString("menus.items.filler.material")))
                .nameFromPath("menus.items.filler.name")
                .lore(new Color.Builder().path("menus.items.filler.lore").buildLore())
                .build();

        for (int i = 45; i < 54; i++) {
            items[i] = filler;
        }
        ItemStack switchMenu = new MenuItem.Builder(Material.valueOf(config.getString("menus.items.mobs.material")))
                .nameFromPath("menus.items.mobs.name")
                .lore(new Color.Builder().path("menus.items.mobs.lore").buildLore())
                .build();
        items[48] = switchMenu;

        ItemStack autoSellItem = new MenuItem.Builder(Material.valueOf(config.getString("menus.items.auto_sell_" + autoSell + ".material")))
                .nameFromPath("menus.items.auto_sell_" + autoSell + ".name")
                .lore(new Color.Builder().path("menus.items.auto_sell_" + autoSell + ".lore").buildLore())
                .build();
        items[50] = autoSellItem;

        inventory.setContents(items);

        return inventory;
    }
}
