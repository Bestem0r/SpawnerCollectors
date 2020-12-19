package me.bestem0r.spawnercollectors.commands.subcommands;

import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.commands.CommandModule;
import me.bestem0r.spawnercollectors.commands.SubCommand;
import me.bestem0r.spawnercollectors.utils.ColorBuilder;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements SubCommand {

    private final SCPlugin plugin;
    private CommandModule module;

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
        String message = new ColorBuilder(plugin).path("messages.plugin_reloaded").addPrefix().build();
        module.commandOutput(sender, message);
    }

    @Override
    public void setModule(CommandModule module) {
        this.module = module;
    }

    @Override
    public String getDescription() {
        return "Reload SpawnerCollectors";
    }
}
