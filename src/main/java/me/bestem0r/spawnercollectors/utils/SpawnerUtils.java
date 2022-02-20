package me.bestem0r.spawnercollectors.utils;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.dnyferguson.mineablespawners.MineableSpawners;
import de.dustplanet.util.SilkUtil;
import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            i = SilkUtil.hookIntoSilkSpanwers().newSpawnerItem(type.getEntityType().name(), "", amount, false);
        } else {
            i = new ItemStack(XMaterial.SPAWNER.parseMaterial(), amount);

            //Lots of casting...
            ItemMeta itemMeta = i.getItemMeta();
            BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
            BlockState blockState = blockStateMeta.getBlockState();

            CreatureSpawner spawner = (CreatureSpawner) blockState;
            spawner.setSpawnedType(type.getEntityType());

            blockStateMeta.setBlockState(blockState);
            i.setItemMeta(itemMeta);
        }
        ItemMeta meta = i.getItemMeta();

        String entityName = WordUtils.capitalizeFully(type.name().replaceAll("_", " "));
        meta.setDisplayName(ConfigManager.getString("spawner_withdraw_name").replace("%entity%", entityName));

        i.setItemMeta(meta);
        return i;
    }

    /** Plays sound for Player */
    public static void playSound(SCPlugin plugin, Player player, String soundPath) {
        FileConfiguration config = plugin.getConfig();
        XSound xSound = XSound.matchXSound(config.getString("sounds." + soundPath)).orElse(XSound.UI_BUTTON_CLICK);
        player.playSound(player.getLocation(), xSound.parseSound(), 1, 1);
    }

    /** Returns CustomEntityType from spawner itemStack */
    public static CustomEntityType typeFromSpawner(ItemStack itemStack) {

        if (itemStack.getType() == XMaterial.SPAWNER.parseMaterial()) {

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

    public static int parseMCVersion() {
        String version = Bukkit.getVersion();
        Matcher matcher = Pattern.compile("MC: \\d\\.(\\d+)").matcher(version);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            throw new IllegalArgumentException("Failed to parse server version from: " + version);
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
}
