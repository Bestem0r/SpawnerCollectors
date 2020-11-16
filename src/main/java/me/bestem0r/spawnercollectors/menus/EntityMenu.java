package me.bestem0r.spawnercollectors.menus;

import me.bestem0r.spawnercollectors.EntityCollector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class EntityMenu {

    public static Inventory create(List<EntityCollector> collected, boolean autoSell) {
        Inventory inventory = Bukkit.createInventory(null, 54, new Color.Builder().path("menus.mobs.title").build());
        ItemStack[] items = new ItemStack[54];
        double totalWorth = 0;

        for (int i = 0; i < collected.size(); i++) {
            if (i == 45) { break; }
            items[i] = collected.get(i).getEntityItem();
            totalWorth += collected.get(i).getTotalWorth();
        }
        FileConfiguration config = SCPlugin.getInstance().getConfig();

        ItemStack filler = new MenuItem.Builder(Material.valueOf(config.getString("menus.items.filler.material")))
                .nameFromPath("menus.items.filler.name")
                .lore(new Color.Builder().path("menus.items.filler.lore").buildLore())
                .build();

        for (int i = 45; i < 54; i++) {
            items[i] = filler;
        }
        ItemStack switchMenu = new MenuItem.Builder(Material.valueOf(config.getString("menus.items.spawners.material")))
                .nameFromPath("menus.items.spawners.name")
                .lore(new Color.Builder().path("menus.items.spawners.lore").buildLore())
                .build();
        items[48] = switchMenu;

        ItemStack sellAll = new MenuItem.Builder(Material.valueOf(config.getString("menus.items.sell_all.material")))
                .nameFromPath("menus.items.sell_all.name")
                .lore(new Color.Builder().path("menus.items.sell_all.lore")
                        .replaceWithCurrency("%worth%", String.valueOf(totalWorth)).buildLore())
                .build();
        items[49] = sellAll;

        ItemStack autoSellItem = new MenuItem.Builder(Material.valueOf(config.getString("menus.items.auto_sell_" + autoSell + ".material")))
                .nameFromPath("menus.items.auto_sell_" + autoSell + ".name")
                .lore(new Color.Builder().path("menus.items.auto_sell_" + autoSell + ".lore").buildLore())
                .build();
        items[50] = autoSellItem;

        inventory.setContents(items);

        return inventory;
    }
}
