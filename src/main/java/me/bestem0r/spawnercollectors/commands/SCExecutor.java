package me.bestem0r.spawnercollectors.commands;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utilities.Color;
import me.bestem0r.spawnercollectors.utilities.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class SCExecutor implements CommandExecutor {

    private final SCPlugin plugin;

    public SCExecutor(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (args.length == 0) {
            return false;
        }
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (player != null && !player.hasPermission("spawnercollectors.reload")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for this command!");
                return true;
            }
            plugin.reloadValues();
            sendMessage(player, new Color.Builder(plugin).path("messages.plugin_reloaded").addPrefix().build());
            return true;
        }
        if (args[0].equalsIgnoreCase("givespawner")) {
            if (player != null && !player.hasPermission("spawnercollectors.givespawner")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for this command!");
                return true;
            }
            if (args.length > 4) {
                sendMessage(player, ChatColor.RED + "Please specify player and entity type!");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendMessage(player, ChatColor.RED + "Could not find player: " + args[1]);
                return true;
            }
            if (!isEntity(args[2])) {
                sendMessage(player, ChatColor.RED + "Could not find valid entity type: " + args[2]);
                return true;
            }
            EntityType entityType = EntityType.valueOf(args[2]);
            int amount = 1;
            if (args.length == 4 && canConvert(args[3])) {
                amount = Integer.parseInt(args[3]);
            }
            Collector collector = Methods.getCollector(plugin, target);
            if (collector.addSpawner(player, entityType, amount)) {
                sendMessage(player, new Color.Builder(plugin).path("messages.give_spawner").addPrefix().build());
            }
            return true;
        }

        if (sender instanceof Player) {

            if (!player.hasPermission("spawnercollectors.use")) {
                player.sendMessage(ChatColor.RED + "You do not have permissions for this command!");
                return true;
            }

            switch (args[0]) {
                case "spawners":
                    Methods.getCollector(plugin, player).openSpawnerMenu(player);
                    break;
                case "mobs":
                    Methods.getCollector(plugin, player).openEntityMenu(player);
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

    private void sendMessage(Player player, String message) {
        if (player == null) {
            Bukkit.getLogger().info(message);
        } else {
            player.sendMessage(message);
        }
    }
}
