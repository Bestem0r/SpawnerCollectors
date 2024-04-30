package me.bestem0r.spawnercollectors.listener;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class BlockListener implements Listener {

    private final SCPlugin plugin;
    private final Material spawner = Material.valueOf(VersionUtils.getMCVersion() < 13 ? "MOB_SPAWNER" : "SPAWNER");

    public BlockListener(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {

        Material mat = event.getBlock().getType();
        if (plugin.isDisablePlace() && mat == spawner && !event.getPlayer().hasPermission("spawnercollectors.bypass_place")) {
            event.getPlayer().sendMessage(ConfigManager.getMessage("messages.no_permission_place_spawner"));
            event.setCancelled(true);
            return;
        }

        if (event.isCancelled()) {
            return;
        }

        if (mat == spawner && VersionUtils.getMCVersion() > 13) {
            PersistentDataContainer container = event.getItemInHand().getItemMeta().getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(plugin, "spawnercollectors-spawner-type");

            if (container.has(key, PersistentDataType.STRING)) {
                EntityType type = EntityType.valueOf(container.get(key, PersistentDataType.STRING));
                CreatureSpawner spawner = (CreatureSpawner) event.getBlock().getState();
                spawner.setSpawnedType(type);
                spawner.update();

                if (!ConfigManager.getBoolean("enable_enhanced_placed_spawners")) {
                    return;
                }
                event.setCancelled(!plugin.getCollectorManager().addPlacedCollector(event.getBlock().getLocation(), event.getPlayer(), type));
            }
        }
    }

    @EventHandler
    public void onInteractSpawner(PlayerInteractEvent event) {
        if (ConfigManager.getBoolean("right_click_spawner_menu")) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_AIR) { return; }

            Player player = event.getPlayer();
            boolean bypass = player.hasPermission("spawnercollectors.bypass_place");
            boolean isSpawner = event.getPlayer().getItemInHand().getType().name().contains("SPAWNER");

            isSpawner = isSpawner && ((plugin.isDisablePlace() && !bypass) || event.getAction() == Action.RIGHT_CLICK_AIR);
            if (isSpawner) {
                SpawnerUtils.getCollector(plugin, event.getPlayer()).openSpawnerMenu(event.getPlayer());
                event.setCancelled(true);
                return;
            }
        }

        if (ConfigManager.getBoolean("enable_enhanced_placed_spawners") && event.getClickedBlock() != null) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType() == spawner) {
                plugin.getCollectorManager().openPlacedCollector(event.getClickedBlock().getLocation(), event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {

        Material mat = event.getBlock().getType();
        Player player = event.getPlayer();

        if (mat == spawner && plugin.getConfig().getBoolean("enable_silktouch") &&
                !Bukkit.getPluginManager().isPluginEnabled("SilkSpawners") &&
                !Bukkit.getPluginManager().isPluginEnabled("SilkSpawners_v2") &&
                !Bukkit.getPluginManager().isPluginEnabled("MineableSpawners")) {

            ItemStack i = event.getPlayer().getInventory().getItemInHand();

            if (i.getType() != Material.AIR && i.hasItemMeta() && i.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {

                if (event.getBlock().getState() instanceof CreatureSpawner) {
                    EntityType type = ((CreatureSpawner) event.getBlock().getState()).getSpawnedType();
                    ItemStack spawner = SpawnerUtils.spawnerFromType(new CustomEntityType(type), 1, plugin);

                    int amount = 1;
                    String uuid = SpawnerUtils.locationToBase64(event.getBlock().getLocation());
                    Collector collector = plugin.getCollectorManager().getCollector(uuid);
                    boolean spawnerBreak = ConfigManager.getBoolean("enable_spawner_breaking");

                    if (!spawnerBreak && collector != null && !collector.getOwner().getUniqueId().equals(player.getUniqueId())) {
                        player.sendMessage(ConfigManager.getMessage("messages.not_owner"));
                        event.setCancelled(true);
                        return;
                    }
                    if (collector != null && !collector.getCollectorEntities().isEmpty()) {
                        amount = collector.getCollectorEntities().get(0).getSpawnerAmount();
                    }

                    final int finalAmount = amount;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        int toRemove = finalAmount;

                        while (toRemove > 0) {
                            ItemStack drop = spawner.clone();
                            drop.setAmount(Math.min(toRemove, 64));
                            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), drop);
                            toRemove -= 64;
                        }

                    }, 2);
                    event.setExpToDrop(0);
                }
            }
        }

        if (mat == spawner && ConfigManager.getBoolean("enable_enhanced_placed_spawners")) {
            plugin.getCollectorManager().removePlacedCollector(event.getBlock().getLocation(), event.getPlayer());
        }
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        if (ConfigManager.getBoolean("disable_spawner_spawn")) {
            event.setCancelled(true);
        }
        if (!ConfigManager.getBoolean("enable_enhanced_placed_spawners")) {
            return;
        }
        String uuid = SpawnerUtils.locationToBase64(event.getSpawner().getLocation());
        if (plugin.getCollectorManager().getCollector(uuid) != null) {
            event.setCancelled(true);
        }
    }
}
