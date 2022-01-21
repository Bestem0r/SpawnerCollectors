package me.bestem0r.spawnercollectors.events;

import com.cryptomorin.xseries.XMaterial;
import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.ConfigManager;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockEvent implements Listener {

    private final SCPlugin plugin;
    private final XMaterial spawner = XMaterial.SPAWNER;

    public BlockEvent(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        XMaterial mat = XMaterial.matchXMaterial(event.getBlock().getType());
        if (this.plugin.isDisablePlace() && mat == spawner && !event.getPlayer().hasPermission("spawnercollectors.bypass_place")) {
            event.getPlayer().sendMessage(ConfigManager.getMessage("messages.no_permission_place_spawner"));
            event.setCancelled(true);
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {

        XMaterial mat = XMaterial.matchXMaterial(event.getBlock().getType());
        if (mat == spawner && plugin.getConfig().getBoolean("enable_silktouch")) {

            ItemStack i = event.getPlayer().getInventory().getItemInHand();

            if (i.getType() != Material.AIR && i.hasItemMeta() && i.getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {

                if (event.getBlock().getState() instanceof CreatureSpawner) {
                    EntityType type = ((CreatureSpawner) event.getBlock().getState()).getSpawnedType();
                    ItemStack spawner = SpawnerUtils.spawnerFromType(new CustomEntityType(type), 1);
                    event.getBlock().getLocation().getWorld().dropItem(event.getBlock().getLocation(), spawner);
                }
            }
        }
    }
}
