package me.bestem0r.spawnercollectors.menus;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.collector.EntityCollector;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.CorePlugin;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuConfig;
import net.bestemor.core.menu.MenuContent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SpawnerMenu extends Menu {

    private final Collector collector;
    private final CorePlugin plugin;
    private Instant nextWithdraw = Instant.now();

    private final List<Integer> slots;
    private final int singleSlot;

    public SpawnerMenu(CorePlugin corePlugin, Collector collector) {
        super(MenuConfig.fromConfig("menus.spawners"));
        this.collector = collector;
        this.plugin = corePlugin;
        this.slots = ConfigManager.getIntegerList("menus.spawners.slots");
        this.singleSlot = ConfigManager.getInt("menus.spawners.single_spawner_slot");
    }

    @Override
    protected void onCreate(MenuContent menuContent) {
        ItemStack filler = ConfigManager.getItem("menus.items.filler").build();
        menuContent.fillSlots(filler, ConfigManager.getIntArray("menus.spawners.filler_slots"));
        update();
    }

    @Override
    protected void onUpdate(MenuContent content) {
        double totalWorth = collector.getTotalWorth();

        content.setClickable(ConfigManager.getInt("menus.items.mobs.slot"), new Clickable(ConfigManager.getItem("menus.items.mobs")
                .replaceCurrency("%avg_production%", collector.getAverageProduction())
                .build(), (event) -> {
            Player player = (Player) event.getWhoClicked();

            boolean morePermissions = ConfigManager.getBoolean("more_permissions");
            if (morePermissions && !player.hasPermission("spawnercollectors.command.mobs")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_command"));
            } else {
                collector.openEntityMenu(player);
            }
        }));

        content.setClickable(ConfigManager.getInt("menus.items.sell_all.slot"), new Clickable(ConfigManager.getItem("menus.items.sell_all")
                .replaceCurrency("%avg_production%", collector.getAverageProduction())
                .replaceCurrency("%worth%", BigDecimal.valueOf(totalWorth)).build(), (event) -> {

            new ConfirmMenu(() -> collector.sellAll((Player) event.getWhoClicked())).open((Player) event.getWhoClicked());
        }));

        content.setClickable(ConfigManager.getInt("menus.items.auto_sell_slot"), new Clickable(ConfigManager.getItem("menus.items.auto_sell_" + collector.isAutoSell())
                .replaceCurrency("%avg_production%", collector.getAverageProduction())
                .build(), (event) -> {
            collector.toggleAutoSell((Player) event.getWhoClicked());
        }));

        for (int slot : slots) {
            int index = slots.indexOf(slot);
            if (index >= collector.getCollectorEntities().size() || (index > 0 && collector.isSingleEntity())) {
                if (slot != singleSlot || !collector.isSingleEntity()) {
                    content.setClickable(slot, new Clickable(null));
                }
                continue;
            }
            EntityCollector collected = collector.getCollectorEntities().get(index);
            ItemStack item = collected.getSpawnerItem();
            content.setClickable(collector.isSingleEntity() ? singleSlot : slot, new Clickable(item, event -> {

                Player player = (Player) event.getWhoClicked();

                if (Instant.now().isBefore(nextWithdraw)) {
                    player.sendMessage(ConfigManager.getMessage("messages.withdraw_too_fast"));
                    return;
                }

                boolean morePermissions = ConfigManager.getBoolean("more_permissions");

                if (morePermissions && !player.hasPermission("spawnercollectors.withdraw.spawner")) {
                    player.sendMessage(ConfigManager.getMessage("messages.no_permission_withdraw_spawner"));
                    return;
                }

                int withdrawAmount = Math.min(collected.getSpawnerAmount(), 1);
                if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                    withdrawAmount = Math.min(collected.getSpawnerAmount(), 64);
                }
                if (collector.isSingleEntity()) {
                    withdrawAmount = Math.min(collected.getSpawnerAmount() - 1, 64);
                }


                ItemStack spawner = SpawnerUtils.spawnerFromType(collected.getEntityType(), withdrawAmount, plugin);

                Map<Integer, ItemStack> drop = player.getInventory().addItem(spawner);
                for (int i : drop.keySet()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop.get(i));
                }
                collected.removeSpawners(withdrawAmount);

                if (collected.getSpawnerAmount() < 1) {
                    collected.sell(player, collected.getEntityAmount());
                    collector.getCollectorEntities().remove(collected);
                    collector.updateEntityMenu();
                }

                this.nextWithdraw = Instant.now().plusMillis(ConfigManager.getLong("withdraw_cooldown"));
                player.playSound(player.getLocation(), ConfigManager.getSound("sounds.withdraw"), 1f, 1f);
                SCPlugin.log.add(ChatColor.stripColor(new Date() + ": " + player.getName() + " withdrew " + withdrawAmount + " " + collected.getEntityType().name() + " Spawner"));
                update();
            }));

            totalWorth += collected.getTotalWorth(collector.getOwner());
        }

    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        //Add spawner
        if (slot >= event.getView().getTopInventory().getSize() && currentItem != null) {
            if (currentItem.getType().name().equals(VersionUtils.getMCVersion() < 13 ? "MOB_SPAWNER" : "SPAWNER")) {
                CustomEntityType type = SpawnerUtils.typeFromSpawner(currentItem);
                if (type != null && collector.addSpawner(player, type, currentItem.getAmount())) {
                    player.getInventory().setItem(event.getSlot(), null);
                }
            }
        }
    }
}
