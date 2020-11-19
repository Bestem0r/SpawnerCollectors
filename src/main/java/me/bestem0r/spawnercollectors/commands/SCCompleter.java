package me.bestem0r.spawnercollectors.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SCCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        List<String> completer = new ArrayList<>();

        if (args.length == 0 || (args.length == 1 && args[0].length() == 0)) {
            completer.add("spawners");
            completer.add("mobs");
            completer.add("reload");
            completer.add("givespawner");
            return completer;
        }
        if (args.length == 1) {
            switch (args[0].charAt(0)) {
                case 's':
                    completer.add("spawners");
                    break;
                case 'm':
                    completer.add("mobs");
                    break;
                case 'r':
                    completer.add("reload");
                    break;
                case 'g':
                    completer.add("givespawner");
            }
            return completer;
        }
        if (args[0].equalsIgnoreCase("givespawner")) {
            switch (args.length) {
                case 2:
                    Bukkit.getOnlinePlayers().forEach((player -> completer.add(player.getName())));
                    return completer;
                case 3:
                    completer.addAll(Stream.of(EntityType.values())
                            .filter(entityType -> entityType.name().startsWith(args[2]))
                            .map(EntityType::toString)
                            .collect(Collectors.toList()));
                    return completer;
            }
        }

        return completer;
    }
}
