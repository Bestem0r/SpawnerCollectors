package me.bestem0r.spawnercollectors.command;

import me.bestem0r.spawnercollectors.SCPlugin;
import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements ISubCommand {

    private final SCPlugin plugin;

    public ReloadCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
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

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public boolean requirePermission() {
        return true;
    }
}
