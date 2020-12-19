package me.bestem0r.spawnercollectors.menus;

import me.bestem0r.spawnercollectors.EntityCollector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.ColorBuilder;
import me.bestem0r.spawnercollectors.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public abstract class EntityMenu {

    public static Inventory create(SCPlugin plugin, List<EntityCollector> collected, boolean autoSell) {
        Inventory inventory = Bukkit.createInventory(null, 54, new ColorBuilder(plugin).path("menus.mobs.title").build());
        ItemStack[] items = new ItemStack[54];
        double totalWorth = 0;

        for (int i = 0; i < collected.size(); i++) {
            if (i == 45) {
                break;
            }
            items[i] = collected.get(i).getEntityItem();
            totalWorth += collected.get(i).getTotalWorth();
        }

        ItemStack filler = Methods.itemFromConfig(plugin, "menus.items.filler");

        for (int i = 45; i < 54; i++) {
            items[i] = filler;
        }
        ItemStack switchMenu = Methods.itemFromConfig(plugin, "menus.items.spawners");
        items[48] = switchMenu;

        ItemStack sellAll = Methods.itemFromConfig(plugin, "menus.items.sell_all");
        ItemMeta sellAllMeta = sellAll.getItemMeta();
        sellAllMeta.setLore(new ColorBuilder(plugin).path("menus.items.sell_all.lore")
                .replaceWithCurrency("%worth%", String.valueOf(totalWorth)).buildLore());
        sellAll.setItemMeta(sellAllMeta);
        items[49] = sellAll;

        ItemStack autoSellItem = Methods.itemFromConfig(plugin, "menus.items.auto_sell_" + autoSell);
        items[50] = autoSellItem;

        inventory.setContents(items);

        return inventory;
    }
}
