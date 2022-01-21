package me.bestem0r.spawnercollectors.menus;

import com.cryptomorin.xseries.XMaterial;
import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.collector.EntityCollector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.ConfigManager;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SpawnerMenu extends Menu {

    private final Collector collector;

    public SpawnerMenu(MenuListener listener, Collector collector) {
        super(listener, 54,  ConfigManager.getString("menus.spawners.title"));
        this.collector = collector;
    }

    @Override
    protected void create(Inventory inventory) {

        fillBottom(ConfigManager.getItem("menus.items.filler").build());
        inventory.setItem(ConfigManager.getInt("menus.items.mobs.slot"), ConfigManager.getItem("menus.items.mobs").build());

        update(inventory);
    }

    @Override
    protected void update(Inventory inventory) {
        double totalWorth = 0;

        for (int i = 0; i < 45; i++) {
            if (i < collector.getCollectorEntities().size()) {
                inventory.setItem(i, collector.getCollectorEntities().get(i).getSpawnerItem());
                totalWorth += collector.getCollectorEntities().get(i).getTotalWorth();
            } else {
                inventory.setItem(i, null);
            }
        }

        inventory.setItem(ConfigManager.getInt("menus.items.sell_all.slot"), ConfigManager.getItem("menus.items.sell_all")
                .replaceCurrency("%worth%", BigDecimal.valueOf(totalWorth)).build());

        inventory.setItem(ConfigManager.getInt("menus.items.auto_sell_slot"), ConfigManager.getItem("menus.items.auto_sell_" + collector.isAutoSell()).build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();


        boolean morePermissions = ConfigManager.getBoolean("more_permissions");

        if (slot == ConfigManager.getInt("menus.items.mobs.slot")) {
            if (morePermissions && !player.hasPermission("spawnercollectors.command.mobs")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_command"));
            } else {
                collector.openEntityMenu(player);
            }
            return;
        }
        if (slot == ConfigManager.getInt("menus.items.sell_all.slot")) {
            collector.sellAll(player);
            return;
        }
        if (slot == ConfigManager.getInt("menus.items.auto_sell_slot")) {
            collector.toggleAutoSell(player);
            return;
        }
        //Add spawner
        if (slot >= event.getView().getTopInventory().getSize() && currentItem != null) {
            if (currentItem.getType() == XMaterial.SPAWNER.parseMaterial()) {
                CustomEntityType type = SpawnerUtils.typeFromSpawner(currentItem);
                if (type != null && collector.addSpawner(player, type, currentItem.getAmount())) {
                    player.getInventory().setItem(event.getSlot(), null);
                }
            }
        }
        //Withdraw spawner
        if (slot < 54) {
            if (slot >= collector.getCollectorEntities().size() || slot < 0) { return; }
            if (morePermissions && !player.hasPermission("spawnercollectors.withdraw.spawner")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_withdraw_spawner"));
                return;
            }

            EntityCollector collected = collector.getCollectorEntities().get(slot);
            if (collected == null) { return; }

            int withdrawAmount = Math.min(collected.getSpawnerAmount(), 64);

            ItemStack spawner = SpawnerUtils.spawnerFromType(collected.getEntityType(), withdrawAmount);

            Map<Integer, ItemStack> drop = player.getInventory().addItem(spawner);
            for (int i : drop.keySet()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop.get(i));
            }
            collected.removeSpawners(withdrawAmount);

            if (collected.getSpawnerAmount() < 1) {
                collector.sell(player, collected);
                collector.getCollectorEntities().remove(collected);
                collector.updateEntityMenu();
            }

            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.withdraw"), 1f, 1f);
            SCPlugin.log.add(ChatColor.stripColor(new Date() + ": " + player.getName() + " withdrew " + withdrawAmount + " " + collected.getEntityType() + " Spawner"));
            update();
        }
    }
}
