package me.bestem0r.spawnercollectors.commands;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utilities.Color;
import me.bestem0r.spawnercollectors.utilities.Methods;
import org.apache.commons.lang.enums.EnumUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SCExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("spawnercollectors.reload")) {
                    player.sendMessage(ChatColor.RED + "You do not have permissions for this command!");
                    return true;
                }
                SCPlugin.getInstance().reloadValues();
                player.sendMessage(new Color.Builder().path("messages.plugin_reloaded").addPrefix().build());
                return true;
            }
            if (args[0].equalsIgnoreCase("givespawner")) {
                if (!player.hasPermission("spawnercollectors.givespawner")) {
                    player.sendMessage(ChatColor.RED + "You do not have permissions for this command!");
                    return true;
                }
                if (args.length > 4) {
                    player.sendMessage(ChatColor.RED + "Please specify player and entity type!");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Could not find player: " + args[1]);
                    return true;
                }
                if (!isEntity(args[2])) {
                    player.sendMessage(ChatColor.RED + "Could not find valid entity type: " + args[2]);
                    return true;
                }
                EntityType entityType = EntityType.valueOf(args[2]);
                int amount = 1;
                if (args.length == 4 && canConvert(args[3])) {
                    amount = Integer.parseInt(args[3]);
                }
                Collector collector = Methods.getCollector(target);
                collector.addSpawner(player, entityType, amount);
                player.sendMessage(new Color.Builder().path("messages.give_spawner").addPrefix().build());
                return true;
            }

            if (!player.hasPermission("spawnercollectors.use")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for this command!");
                return true;
            }

            switch (args[0]) {
                case "spawners":
                    Methods.getCollector(player).openSpawnerMenu(player);
                    break;
                case "mobs":
                    Methods.getCollector(player).openEntityMenu(player);
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Unknown sub-command: " + args[0]);
            }
            return true;
        }
        return false;
    }

    private boolean isEntity(String entityTest) {
        for (EntityType entityType : EntityType.values()) {
            if (entityType.name().equals(entityTest)) {
                return true;
            }
        }
        return false;
    }

    private Boolean canConvert(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
