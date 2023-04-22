package me.bestem0r.spawnercollectors.listener;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
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

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        Material mat = event.getBlock().getType();
        if (this.plugin.isDisablePlace() && mat == spawner && !event.getPlayer().hasPermission("spawnercollectors.bypass_place")) {
            event.getPlayer().sendMessage(ConfigManager.getMessage("messages.no_permission_place_spawner"));
            event.setCancelled(true);
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
            }
        }
    }

    @EventHandler
    public void onInteractSpawner(PlayerInteractEvent event) {
        if (ConfigManager.getBoolean("right_click_spawner_menu")) {

            boolean spawnerInInventory = event.getAction() == Action.RIGHT_CLICK_AIR && event.getPlayer().getItemInHand().getType().name().contains("SPAWNER");
            boolean spawnerClick = event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock().getType().name().contains("SPAWNER");
            if (spawnerInInventory || spawnerClick) {
                SpawnerUtils.getCollector(plugin, event.getPlayer()).openSpawnerMenu(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {

        Material mat = event.getBlock().getType();
        if (mat == spawner && plugin.getConfig().getBoolean("enable_silktouch") &&
                !Bukkit.getPluginManager().isPluginEnabled("SilkSpawners") &&
                !Bukkit.getPluginManager().isPluginEnabled("SilkSpawners_v2") &&
                !Bukkit.getPluginManager().isPluginEnabled("MineableSpawners")) {

            ItemStack i = event.getPlayer().getInventory().getItemInHand();

            if (i.getType() != Material.AIR && i.hasItemMeta() && i.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {

                if (event.getBlock().getState() instanceof CreatureSpawner) {
                    EntityType type = ((CreatureSpawner) event.getBlock().getState()).getSpawnedType();
                    ItemStack spawner = SpawnerUtils.spawnerFromType(new CustomEntityType(type), 1, plugin);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        event.getBlock().getLocation().getWorld().dropItem(event.getBlock().getLocation(), spawner);
                    }, 2);
                    event.setExpToDrop(0);
                }
            }
        }
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        if (ConfigManager.getBoolean("disable_spawner_spawn")) {
            event.setCancelled(true);
        }
    }
}
