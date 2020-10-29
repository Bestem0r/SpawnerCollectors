package me.bestem0r.spawnercollectors.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class SCCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        List<String> completer = new ArrayList<>();

        if (args.length == 0 || (args.length == 1 && args[0].length() == 0)) {
            completer.add("spawners");
            completer.add("mobs");
            completer.add("reload");
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
            }
            return completer;
        }
        return completer;
    }
}
