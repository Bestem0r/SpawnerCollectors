package me.bestem0r.spawnercollectors.menus;

import me.bestem0r.spawnercollectors.EntityExperience;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.collector.EntityCollector;
import me.bestem0r.spawnercollectors.loot.LootManager;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import net.bestemor.core.menu.Clickable;
import net.bestemor.core.menu.Menu;
import net.bestemor.core.menu.MenuContent;
import net.bestemor.core.menu.MenuListener;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class EntityMenu extends Menu {

    private final Collector collector;
    private final LootManager lootManager;
    private Instant nextWithdraw = Instant.now();

    public EntityMenu(MenuListener listener, Collector collector, LootManager lootManager) {
        super(listener, 54, ConfigManager.getString("menus.mobs.title"));
        this.collector = collector;
        this.lootManager = lootManager;
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
            if (slot < collector.getCollectorEntities().size()) {
                EntityCollector collected = collector.getCollectorEntities().get(slot);
                content.setClickable(slot, new Clickable(collected.getEntityItem(), (event) -> {

                    Player player = (Player) event.getWhoClicked();
                    if (event.getClick() == ClickType.LEFT) {
                        collector.sell(player, collected);
                    }
                    //Withdraw
                    if (event.getClick() == ClickType.RIGHT) {

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

                        long withdrawAmount = Math.min(collected.getEntityAmount(), 64);

                        collected.removeEntities(withdrawAmount);

                        for (ItemStack itemStack : lootManager.lootFromType(collected.getEntityType(), player, withdrawAmount)) {
                            Map<Integer, ItemStack> drop = player.getInventory().addItem(itemStack);
                            for (int i : drop.keySet()) {
                                ItemStack item = drop.get(i);
                                if (item != null && item.getType() != Material.AIR) {
                                    player.getWorld().dropItemNaturally(player.getLocation(), drop.get(i));
                                }
                            }
                        }
                        if (ConfigManager.getBoolean("give_xp") && (!ConfigManager.getBoolean("more_permissions") || player.hasPermission("spawnercollectors.receive_xp"))) {

                            int xp = 0;
                            if (lootManager.isUseCustomLoot() && lootManager.getCustomXP().containsKey(collected.getEntityType().name())) {
                                xp = (int) (lootManager.getCustomXP().get(collected.getEntityType().name()) * withdrawAmount);
                            } else {
                                for (EntityExperience e : EntityExperience.values()) {
                                    if (e.name().equals(collected.getEntityType().name())) {
                                        xp = e.getRandomAmount(withdrawAmount);
                                        break;
                                    }
                                }
                            }

                            if (ConfigManager.getBoolean("mending") && VersionUtils.getMCVersion() > 8) {
                                List<ItemStack> mendable = new ArrayList<>();

                                for (int i = 100; i < 104 && i < player.getInventory().getContents().length; i++) {
                                    ItemStack item = player.getInventory().getContents()[i];
                                    if (canBeRepaired(item)) {
                                        mendable.add(item);
                                    }
                                }
                                ItemStack hand = player.getItemInHand();
                                if (canBeRepaired(hand)) {
                                    mendable.add(hand);
                                }

                                if (!mendable.isEmpty()) {
                                    for (int i = 0; i < withdrawAmount && xp >= 2; i++) {
                                        if (mendable.size() == 0) {
                                            break;
                                        }
                                        ItemStack item = mendable.get(ThreadLocalRandom.current().nextInt(0, mendable.size()));
                                        if (!repair(item)) {
                                            mendable.remove(item);
                                        }
                                        xp -= 2;
                                    }
                                }
                                player.updateInventory();
                            }
                            if (xp >= 0) {
                                player.giveExp(xp);
                                Sound s = Sound.valueOf(VersionUtils.getMCVersion() > 8 ? "ENTITY_EXPERIENCE_ORB_PICKUP" : "ORB_PICKUP");
                                player.playSound(player.getLocation(), s, 1.0F, 1.0F);
                            }
                        }

                        this.nextWithdraw = Instant.now().plusMillis(ConfigManager.getLong("withdraw_cooldown"));
                        player.playSound(player.getLocation(), ConfigManager.getSound("sounds.withdraw"), 1f, 1f);
                        update();
                    }

                }));

                totalWorth += collected.getTotalWorth();
            } else {
                content.setClickable(slot, new Clickable(null));
            }
        }

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

        content.setClickable(ConfigManager.getInt("menus.items.sell_all.slot"), new Clickable(ConfigManager.getItem("menus.items.sell_all")
                .replaceCurrency("%worth%", BigDecimal.valueOf(totalWorth))
                .replaceCurrency("%avg_production%", collector.getAverageProduction())
                .replace("%mobs%", String.valueOf(collector.getTotalMobCount())).build(), (event) -> {

            collector.sellAll((Player) event.getWhoClicked());
        }));
        content.setClickable(ConfigManager.getInt("menus.items.auto_sell_slot"), new Clickable(ConfigManager.getItem("menus.items.auto_sell_" + collector.isAutoSell())
                .replaceCurrency("%avg_production%", collector.getAverageProduction())
                .build(), (event) -> {

            collector.toggleAutoSell((Player) event.getWhoClicked());
        }));
    }

    private boolean canBeRepaired(ItemStack item) {
        if (item == null || item.getItemMeta() == null || VersionUtils.getMCVersion() < 9) {
            return false;
        }
        if (!item.getItemMeta().hasEnchants() || !item.getItemMeta().hasEnchant(Enchantment.MENDING)) {
            return false;
        }

        return item.getDurability() > 0;
    }

    private boolean repair(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }

        item.setDurability((short) Math.max(0, item.getDurability() - 2));
        return item.getDurability() > 0;
    }
}
