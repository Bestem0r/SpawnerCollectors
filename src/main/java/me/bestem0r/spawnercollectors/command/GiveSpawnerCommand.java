package me.bestem0r.spawnercollectors.command;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import net.bestemor.core.config.VersionUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GiveSpawnerCommand implements ISubCommand {

    private final SCPlugin plugin;

    public GiveSpawnerCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> getCompletion(String[] args) {
        switch(args.length) {
            case 2:
                return Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).collect(Collectors.toList());
            case 3:
                return Arrays.asList("hand", "gui");
            case 4:
                return Stream.of(EntityType.values()).filter((entityType) -> {
                    return entityType.name().startsWith(args[3].toUpperCase(Locale.ROOT));
                }).map(Enum::toString).map(String::toLowerCase).collect(Collectors.toList());
            default:
                return new ArrayList<>();
        }
    }

    public void run(CommandSender sender, String[] args) {
        if (args.length != 4 && args.length != 5) {
            sender.sendMessage(ChatColor.RED + "Please specify player and entity type!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Could not find player: " + args[1]);
            return;
        }
        if (!args[2].equals("hand") && !args[2].equals("gui")) {
            sender.sendMessage(ChatColor.RED + "Please specify where to give the spawners!");
            return;
        }
        if (!this.isEntity(args[3])) {
            sender.sendMessage(ChatColor.RED + "Could not find valid entity type: " + args[3]);
            return;
        }

        CustomEntityType entityType = new CustomEntityType(args[3]);
        int amount = 1;
        if (args.length == 5 && this.canConvert(args[4])) {
            amount = Integer.parseInt(args[4]);
        }

        switch(args[2]) {
            case "hand":
                target.getInventory().addItem(SpawnerUtils.spawnerFromType(entityType, amount, plugin));
                target.playSound(target.getLocation(), Sound.valueOf(VersionUtils.getMCVersion() > 8 ? "ENTITY_ITEM_PICKUP" : "ITEM_PICKUP"), 1, 1);
                break;
            case "gui":
                Player player = null;
                if (sender instanceof Player) {
                    player = (Player)sender;
                }

                Collector collector = SpawnerUtils.getCollector(this.plugin, target);
                if (collector.addSpawner(player, entityType, amount)) {
                    sender.sendMessage(ConfigManager.getMessage("messages.give_spawner"));
                }
        }
    }

    private boolean isEntity(String entityTest) {
        for (EntityType entityType : EntityType.values()) {
            if (entityType.name().equals(entityTest.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean canConvert(String string) {
        for(int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }

        return true;
    }

    public String getDescription() {
        return "Give spawners to players";
    }

    @Override
    public String getUsage() {
        return "<player> <hand/gui> <type> [amount]";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }
}
