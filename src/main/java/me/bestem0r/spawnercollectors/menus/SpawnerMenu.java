package me.bestem0r.spawnercollectors.menus;

import me.bestem0r.spawnercollectors.EntityCollector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utilities.Color;
import me.bestem0r.spawnercollectors.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public abstract class SpawnerMenu {

    public static Inventory create(SCPlugin plugin, List<EntityCollector> collected, boolean autoSell) {
        Inventory inventory = Bukkit.createInventory(null, 54, new Color.Builder(plugin).path("menus.spawners.title").build());
        ItemStack[] items = new ItemStack[54];
        double totalWorth = 0;

        for (int i = 0; i < collected.size(); i++) {
            if (i == 54) { break; }
            items[i] = collected.get(i).getSpawnerItem();
            totalWorth += collected.get(i).getTotalWorth();
        }

        ItemStack filler = Methods.itemFromConfig(plugin, "menus.items.filler");

        for (int i = 45; i < 54; i++) {
            items[i] = filler;
        }

        ItemStack switchMenu = Methods.itemFromConfig(plugin, "menus.items.mobs");
        items[48] = switchMenu;

        ItemStack sellAll = Methods.itemFromConfig(plugin, "menus.items.sell_all");
        ItemMeta sellAllMeta = sellAll.getItemMeta();
        sellAllMeta.setLore(new Color.Builder(plugin).path("menus.items.sell_all.lore")
                .replaceWithCurrency("%worth%", String.valueOf(totalWorth)).buildLore());
        sellAll.setItemMeta(sellAllMeta);
        items[49] = sellAll;

        ItemStack autoSellItem = Methods.itemFromConfig(plugin, "menus.items.auto_sell_" + autoSell);
        items[50] = autoSellItem;

        inventory.setContents(items);

        return inventory;
    }
}
