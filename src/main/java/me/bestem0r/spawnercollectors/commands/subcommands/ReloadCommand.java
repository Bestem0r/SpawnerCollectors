package me.bestem0r.spawnercollectors.commands.subcommands;

import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.commands.CommandModule;
import me.bestem0r.spawnercollectors.commands.SubCommand;
import me.bestem0r.spawnercollectors.utils.ConfigManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements SubCommand {

    private final SCPlugin plugin;

    public ReloadCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        plugin.reloadValues();
        String message = ConfigManager.getMessage("messages.plugin_reloaded");
        sender.sendMessage(message);
    }

    @Override
    public String getDescription() {
        return "Reload SpawnerCollectors";
    }
}
