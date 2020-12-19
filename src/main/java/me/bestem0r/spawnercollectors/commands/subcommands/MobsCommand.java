package me.bestem0r.spawnercollectors.commands.subcommands;

import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.commands.CommandModule;
import me.bestem0r.spawnercollectors.commands.SubCommand;
import me.bestem0r.spawnercollectors.utils.Methods;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MobsCommand implements SubCommand {

    private final SCPlugin plugin;

    public MobsCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Methods.getCollector(plugin, player).openEntityMenu(player);
        }
    }

    @Override
    public void setModule(CommandModule module) {

    }

    @Override
    public String getDescription() {
        return "Open your mob storage";
    }
}
