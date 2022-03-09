package me.bestem0r.spawnercollectors.menus;

import com.cryptomorin.xseries.XMaterial;
import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.collector.EntityCollector;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.core.menu.MenuListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

public class SpawnerMenu extends Menu {

    private final Collector collector;

    public SpawnerMenu(MenuListener listener, Collector collector) {
        super(listener, 54,  ConfigManager.getString("menus.spawners.title"));
        this.collector = collector;
    }

    @Override
    protected void onCreate(MenuContent menuContent) {
        menuContent.fillBottom(ConfigManager.getItem("menus.items.filler").build());

        boolean morePermissions = ConfigManager.getBoolean("more_permissions");

        menuContent.setClickable(ConfigManager.getInt("menus.items.mobs.slot"), new Clickable(ConfigManager.getItem("menus.items.mobs").build(), (event) -> {
            Player player = (Player) event.getWhoClicked();

            if (morePermissions && !player.hasPermission("spawnercollectors.command.mobs")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_command"));
            } else {
                collector.openEntityMenu(player);
            }
        }));
        update();
    }

    @Override
    protected void onUpdate(MenuContent content) {
        double totalWorth = collector.getTotalWorth();

        content.setClickable(ConfigManager.getInt("menus.items.sell_all.slot"), new Clickable(ConfigManager.getItem("menus.items.sell_all")
                .replaceCurrency("%worth%", BigDecimal.valueOf(totalWorth)).build(), (event) -> {
            collector.sellAll((Player) event.getWhoClicked());
        }));

        content.setClickable(ConfigManager.getInt("menus.items.auto_sell_slot"), new Clickable(ConfigManager.getItem("menus.items.auto_sell_" + collector.isAutoSell()).build(), (event) -> {
            collector.toggleAutoSell((Player) event.getWhoClicked());
        }));

        for (int slot = 0; slot < 45; slot++) {
            if (slot < collector.getCollectorEntities().size()) {
                EntityCollector collected = collector.getCollectorEntities().get(slot);
                content.setClickable(slot, new Clickable(collected.getSpawnerItem(), (event) -> {

                    Player player = (Player) event.getWhoClicked();

                    boolean morePermissions = ConfigManager.getBoolean("more_permissions");

                    if (morePermissions && !player.hasPermission("spawnercollectors.withdraw.spawner")) {
                        player.sendMessage(ConfigManager.getMessage("messages.no_permission_withdraw_spawner"));
                        return;
                    }

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
                }));

                totalWorth += collected.getTotalWorth();
            } else {
                content.setClickable(slot, new Clickable(null));
            }
        }

    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        Bukkit.getLogger().info("[SpawnerCollectors] Running onClick in SpawnerMenu");
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        //Add spawner
        if (slot >= event.getView().getTopInventory().getSize() && currentItem != null) {
            if (currentItem.getType() == XMaterial.SPAWNER.parseMaterial()) {
                CustomEntityType type = SpawnerUtils.typeFromSpawner(currentItem);
                if (type != null && collector.addSpawner(player, type, currentItem.getAmount())) {
                    player.getInventory().setItem(event.getSlot(), null);
                }
            }
        }
    }
}
