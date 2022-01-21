package me.bestem0r.spawnercollectors.menus;

import com.cryptomorin.xseries.XSound;
import me.bestem0r.spawnercollectors.EntityExperience;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.collector.EntityCollector;
import me.bestem0r.spawnercollectors.loot.LootManager;
import me.bestem0r.spawnercollectors.utils.ConfigManager;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

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
    protected void create(Inventory inventory) {
        inventory.clear();

        fillBottom(ConfigManager.getItem("menus.items.filler").build());
        inventory.setItem(ConfigManager.getInt("menus.items.spawners.slot"), ConfigManager.getItem("menus.items.spawners").build());

        update(inventory);
    }

    @Override
    protected void update(Inventory inventory) {

        double totalWorth = 0;

        for (int i = 0; i < 45; i++) {
            if (i < collector.getCollectorEntities().size()) {
                inventory.setItem(i, collector.getCollectorEntities().get(i).getEntityItem());
                totalWorth += collector.getCollectorEntities().get(i).getTotalWorth();
            } else {
                inventory.setItem(i, null);
            }
        }

        inventory.setItem(ConfigManager.getInt("menus.items.sell_all.slot"), ConfigManager.getItem("menus.items.sell_all")
                .replaceCurrency("%worth%", BigDecimal.valueOf(totalWorth))
                .replace("%mobs%", String.valueOf(collector.getTotalMobCount())).build());
        inventory.setItem(ConfigManager.getInt("menus.items.auto_sell_slot"), ConfigManager.getItem("menus.items.auto_sell_" + collector.isAutoSell()).build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        if (slot == ConfigManager.getInt("menus.items.spawners.slot")) {
            if (ConfigManager.getBoolean("more_permissions") && !player.hasPermission("spawnercollectors.command.spawners")) {
                player.sendMessage(ConfigManager.getMessage("messages.no_permission_command"));
            } else {
                collector.openSpawnerMenu(player);
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
        if (slot >= collector.getCollectorEntities().size() || slot < 0) {
            return;
        }
        EntityCollector collected = collector.getCollectorEntities().get(slot);
        if (collected == null) { return; }


        //Sell
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
            //Bukkit.getLogger().info("Cancel? " + plugin.getConfig().getBoolean("cancel_overflowing_items"));
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

                if (ConfigManager.getBoolean(("custom_loot_tables.enable")) && lootManager.getCustomXP().containsKey(collected.getEntityType().name())) {
                    player.giveExp((int) (lootManager.getCustomXP().get(collected.getEntityType().name()) * withdrawAmount));
                    player.playSound(player.getLocation(), XSound.ENTITY_EXPERIENCE_ORB_PICKUP.parseSound(), 1.0F, 1.0F);
                } else {
                    for (EntityExperience e : EntityExperience.values()) {
                        if (e.name().equals(collected.getEntityType().name())) {
                            player.giveExp(e.getRandomAmount(withdrawAmount));
                            player.playSound(player.getLocation(), XSound.ENTITY_EXPERIENCE_ORB_PICKUP.parseSound(), 1.0F, 1.0F);
                            break;
                        }
                    }
                }
            }

            this.nextWithdraw = Instant.now().plusMillis(ConfigManager.getLong("withdraw_cooldown"));
            player.playSound(player.getLocation(), ConfigManager.getSound("sounds.withdraw"), 1f, 1f);
            update();
        }
    }
}
