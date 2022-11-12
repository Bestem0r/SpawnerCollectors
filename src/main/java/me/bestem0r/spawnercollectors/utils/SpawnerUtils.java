package me.bestem0r.spawnercollectors.utils;

import com.dnyferguson.mineablespawners.MineableSpawners;
import de.dustplanet.util.SilkUtil;
import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

public class SpawnerUtils {

    private SpawnerUtils() {}

    /** Returns collector based on Player */
    public static Collector getCollector(SCPlugin plugin, Player player) {
        return plugin.collectors.get(player.getUniqueId());
    }

    /** Returns spawner with set EntityType */
    public static ItemStack spawnerFromType(CustomEntityType type, int amount) {

        ItemStack i;

        if (Bukkit.getPluginManager().isPluginEnabled("MineableSpawners")) {
            i = MineableSpawners.getApi().getSpawnerFromEntityType(type.getEntityType());
        } else if (Bukkit.getPluginManager().isPluginEnabled("SilkSpawners")) {
            i = SilkUtil.hookIntoSilkSpanwers().newSpawnerItem(type.name(), "", amount, false);
        } else {
            i = new ItemStack(Material.valueOf(VersionUtils.getMCVersion() < 13 ? "MOB_SPAWNER" : "SPAWNER"), amount);

            //Lots of casting...
            ItemMeta itemMeta = i.getItemMeta();
            BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
            BlockState blockState = blockStateMeta.getBlockState();

            CreatureSpawner spawner = (CreatureSpawner) blockState;
            spawner.setSpawnedType(type.getEntityType());

            blockStateMeta.setBlockState(blockState);
            i.setItemMeta(itemMeta);
        }
        i.setAmount(amount);
        ItemMeta meta = i.getItemMeta();

        String entityName = WordUtils.capitalizeFully(type.name().replaceAll("_", " "));
        meta.setDisplayName(ConfigManager.getString("spawner_withdraw_name").replace("%entity%", entityName));

        i.setItemMeta(meta);
        return i;
    }

    /** Plays sound for Player */
    public static void playSound(Player player, String soundPath) {
        Sound sound = ConfigManager.getSound(soundPath);
        player.playSound(player.getLocation(), sound, 1 ,1);
    }

    /** Returns CustomEntityType from spawner itemStack */
    public static CustomEntityType typeFromSpawner(ItemStack itemStack) {

        if (itemStack.getType().name().equals(VersionUtils.getMCVersion() < 13 ? "MOB_SPAWNER" : "SPAWNER")) {

            if (Bukkit.getPluginManager().isPluginEnabled("MineableSpawners")) {
                EntityType type = MineableSpawners.getApi().getEntityTypeFromItemStack(itemStack);
                if (type != null) {
                    return new CustomEntityType(type);
                }
            }
            if (Bukkit.getPluginManager().isPluginEnabled("SilkSpawners")) {
                String name = SilkUtil.hookIntoSilkSpanwers().getStoredSpawnerItemEntityID(itemStack);
                if (name != null) {
                    return new CustomEntityType(name);
                }
            }

            //Lots of casting...
            ItemMeta itemMeta = itemStack.getItemMeta();
            BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
            BlockState blockState = blockStateMeta.getBlockState();

            CreatureSpawner spawner = (CreatureSpawner) blockState;
            return new CustomEntityType(spawner.getSpawnedType());
        } else {
            return null;
        }
    }

    public static boolean hasAvailableSlot(Player player){
        Inventory inv = player.getInventory();
        for (int i = 0; i < 35 && i < inv.getContents().length; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) {
                return true;
            }
        }
        return false;
    }

    public static int getMaxMobs(OfflinePlayer player, CustomEntityType type) {
        if (!player.isOnline()) {
            return -1;
        }
        return player.getPlayer().getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .filter((s) -> s.startsWith("spawnercollectors.mob." + type.name().toLowerCase() + "."))
                .filter((s) -> s.length() > 22 + type.name().length() + 1)
                .map((s) -> s.substring(22 + type.name().length() + 1))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(-1);
    }
}
