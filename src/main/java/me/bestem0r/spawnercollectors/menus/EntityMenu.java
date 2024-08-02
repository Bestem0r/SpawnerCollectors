package me.bestem0r.spawnercollectors.menus;

import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.collector.EntityCollector;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

public class EntityMenu extends Menu {

    private final Collector collector;
    private Instant nextWithdraw = Instant.now();

    public EntityMenu(Collector collector) {
        super(54, ConfigManager.getString("menus.mobs.title"));
        this.collector = collector;
    }

    @Override
    protected void onCreate(MenuContent menuContent) {
        menuContent.fillBottom(ConfigManager.getItem("menus.items.filler").build());
        update();
    }

    @Override
    protected void onUpdate(MenuContent content) {
        double totalWorth = 0;

        for (int slot = 0; slot < 45; slot++) {
            if (slot >= collector.getCollectorEntities().size() || (slot > 0 && collector.isSingleEntity())) {
                if (slot != 22 || !collector.isSingleEntity()) {
                    content.setClickable(slot, new Clickable(null));
                }
                continue;
            }
            EntityCollector collected = collector.getCollectorEntities().get(slot);
            content.setClickable(collector.isSingleEntity() ? 22 : slot, new Clickable(collected.getEntityItem(collector.getOwner()), (event) -> {

                Player player = (Player) event.getWhoClicked();
                boolean swap = ConfigManager.getBoolean("menus.mobs.swap_left_right");
                boolean left = event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT;
                boolean right = event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT;

                if ((!swap && left) || (swap && right)) {

                    if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                        new ConfirmMenu(() -> collected.sell(player, collected.getEntityAmount())).open(player);
                    } else {
                        collected.sell(player, 100);
                    }

                } else if ((!swap && right) || (swap && left)) {

                    if (collected.getEntityAmount() <= 0) {
                        return;
                    }

                    boolean morePermissions = ConfigManager.getBoolean("more_permissions");
                    if (morePermissions && !player.hasPermission("spawnercollectors.withdraw.mob")) {
                        player.sendMessage(ConfigManager.getMessage("messages.no_permission_withdraw_mob"));
                        return;
                    }
                    if (Instant.now().isBefore(nextWithdraw)) {
                        player.sendMessage(ConfigManager.getMessage("messages.withdraw_too_fast"));
                        return;
                    }
                    if (!SpawnerUtils.hasAvailableSlot(player) && ConfigManager.getBoolean("cancel_overflowing_items")) {
                        player.sendMessage(ConfigManager.getMessage("messages.inventory_full"));
                        return;
                    }

                    if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                        while (Arrays.stream(player.getInventory().getStorageContents())
                                .anyMatch(Objects::isNull) && collected.getEntityAmount() > 0) {
                            collected.withdraw(player, 10);
                        }
                    } else {
                        collected.withdraw(player, 1);
                    }

                    this.nextWithdraw = Instant.now().plusMillis(ConfigManager.getLong("withdraw_cooldown"));
                    player.playSound(player.getLocation(), ConfigManager.getSound("sounds.withdraw"), 1f, 1f);
                    update();
                }

            }));

            totalWorth += collected.getTotalWorth(collector.getOwner());
        }

        if (!collector.isSingleEntity() || ConfigManager.getBoolean("enable_enhanced_spawner_stack")) {
            content.setClickable(ConfigManager.getInt("menus.items.spawners.slot"), new Clickable(ConfigManager.getItem("menus.items.spawners")
                    .replaceCurrency("%avg_production%", collector.getAverageProduction())
                    .build(), (event) -> {
                Player player = (Player) event.getWhoClicked();
                if (ConfigManager.getBoolean("more_permissions") && !player.hasPermission("spawnercollectors.command.spawners")) {
                    player.sendMessage(ConfigManager.getMessage("messages.no_permission_command"));
                } else {
                    collector.openSpawnerMenu(player);
                }
            }));
        }

        content.setClickable(ConfigManager.getInt("menus.items.sell_all.slot"), new Clickable(ConfigManager.getItem("menus.items.sell_all")
                .replaceCurrency("%worth%", BigDecimal.valueOf(totalWorth))
                .replaceCurrency("%avg_production%", collector.getAverageProduction())
                .replace("%mobs%", String.valueOf(collector.getTotalMobCount())).build(), (event) -> {

            new ConfirmMenu(() -> collector.sellAll((Player) event.getWhoClicked())).open((Player) event.getWhoClicked());
        }));
        content.setClickable(ConfigManager.getInt("menus.items.auto_sell_slot"), new Clickable(ConfigManager.getItem("menus.items.auto_sell_" + collector.isAutoSell())
                .replaceCurrency("%avg_production%", collector.getAverageProduction())
                .build(), (event) -> {

            collector.toggleAutoSell((Player) event.getWhoClicked());
        }));
    }
}
