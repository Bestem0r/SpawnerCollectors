package me.bestem0r.spawnercollectors.commands.subcommands;

import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.DataStoreMethod;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.commands.CommandModule;
import me.bestem0r.spawnercollectors.commands.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MigrateCommand implements SubCommand {

    private final SCPlugin plugin;

    public MigrateCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getStoreMethod() == DataStoreMethod.YAML) {
                plugin.saveAll();
                plugin.loadAll();
                plugin.setStoreMethod(DataStoreMethod.MYSQL);
                plugin.saveAll();
                plugin.collectors.clear();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    plugin.collectors.add(new Collector(plugin, player.getUniqueId()));
                }
                sender.sendMessage("§aSuccessfully migrated from YAML to MySQL!");
            }
        });
    }

    @Override
    public String getDescription() {
        return "Migrate between storage methods";
    }
}
