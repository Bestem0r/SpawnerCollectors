package me.bestem0r.spawnercollectors.commands.subcommands;

import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.commands.CommandModule;
import me.bestem0r.spawnercollectors.commands.SubCommand;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnersCommand implements SubCommand {

    private final SCPlugin plugin;

    public SpawnersCommand(SCPlugin plugin) {
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
            SpawnerUtils.getCollector(plugin, player).openSpawnerMenu(player);
        }
    }

    @Override
    public String getDescription() {
        return "Open your spawner storage";
    }
}
