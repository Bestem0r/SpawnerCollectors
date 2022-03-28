package me.bestem0r.spawnercollectors.command;

import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.command.ISubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SpawnersCommand implements ISubCommand {

    private final SCPlugin plugin;

    public SpawnersCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
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

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }
}
