package me.bestem0r.spawnercollectors.commands;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utilities.Methods;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SCExecutor implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length != 1) { return false; }

            if (args[0].equalsIgnoreCase("reload")) {
                if (player.hasPermission("spawnercollectors.reload")) {
                    player.sendMessage(ChatColor.RED + "You do not have permissions for this command!");
                    return true;
                }
                SCPlugin.getInstance().reloadValues();
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
}
