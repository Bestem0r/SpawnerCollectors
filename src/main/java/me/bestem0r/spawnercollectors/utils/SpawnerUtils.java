package me.bestem0r.spawnercollectors.utils;

import com.dnyferguson.mineablespawners.MineableSpawners;
import de.corneliusmay.silkspawners.plugin.SilkSpawners;
import de.corneliusmay.silkspawners.plugin.spawner.Spawner;
import de.dustplanet.util.SilkUtil;
import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Base64;

public class SpawnerUtils {

    private SpawnerUtils() {}

    /** Returns collector based on Player */
    public static Collector getCollector(SCPlugin plugin, Player player) {
        return plugin.getCollectorManager().getCollector(player.getUniqueId().toString());
    }

    /** Returns spawner with set EntityType */
    public static ItemStack spawnerFromType(CustomEntityType type, int amount, JavaPlugin plugin) {

        ItemStack i;

        if (Bukkit.getPluginManager().isPluginEnabled("MineableSpawners")) {
            i = MineableSpawners.getApi().getSpawnerFromEntityType(type.getEntityType());
        } else if (Bukkit.getPluginManager().isPluginEnabled("SilkSpawners")) {
            i = SilkUtil.hookIntoSilkSpanwers().newSpawnerItem(type.name(), "", amount, false);
        } else if (Bukkit.getPluginManager().isPluginEnabled("SilkSpawners_v2")) {
            SilkSpawners silkSpawnersPlugin = (SilkSpawners) Bukkit.getPluginManager().getPlugin("SilkSpawners_v2");
            Spawner spawner = new Spawner(silkSpawnersPlugin, type.getEntityType());
            i = spawner.getItemStack();
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

            if (VersionUtils.getMCVersion() > 13) {
                NamespacedKey key = new NamespacedKey(plugin, "spawnercollectors-spawner-type");

                ItemMeta meta = i.getItemMeta();
                meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, type.name());
                i.setItemMeta(meta);
            }
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
        Sound sound = ConfigManager.getSound("sounds." + soundPath);
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
            if (Bukkit.getPluginManager().isPluginEnabled("SilkSpawners_v2")) {
                SilkSpawners silkSpawnersPlugin = (SilkSpawners) Bukkit.getPluginManager().getPlugin("SilkSpawners_v2");
                Spawner spawner = new Spawner(silkSpawnersPlugin, itemStack);
                return new CustomEntityType(spawner.getEntityType());
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
        if (player == null || !player.isOnline()) {
            return -1;
        }
        return player.getPlayer().getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .filter((s) -> s.startsWith("spawnercollectors.mob." + type.name().toLowerCase() + "."))
                .map((s) -> s.replace("spawnercollectors.mob." + type.name().toLowerCase() + ".", ""))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(-1);
    }

    public static Location locationFromBase64(String base64) {
        String decoded = new String(Base64.getDecoder().decode(base64));
        String[] split = decoded.split("%");
        String worldName = split[0];

        int x = Integer.parseInt(split[1]);
        int y = Integer.parseInt(split[2]);
        int z = Integer.parseInt(split[3]);
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    /** Returns encoded base64 string */
    public static String locationToBase64(Location location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        String id = location.getWorld().getName() + "%" + x + "%" + y + "%" + z;
        return Base64.getEncoder().encodeToString(id.getBytes());
    }

    public static int getMaxSpawners(OfflinePlayer player, CustomEntityType type) {
        if (player == null || !player.isOnline()) {
            return 0;
        }
        return player.getPlayer().getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .filter((s) -> s.startsWith("spawnercollectors.spawner." + type.name().toLowerCase() + "."))
                .map((s) -> s.replace("spawnercollectors.spawner." + type.name().toLowerCase() + ".", ""))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
    }
}
