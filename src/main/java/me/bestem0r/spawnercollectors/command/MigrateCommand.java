package me.bestem0r.spawnercollectors.command;

import me.bestem0r.spawnercollectors.DataStoreMethod;
import me.bestem0r.spawnercollectors.SCPlugin;
import net.bestemor.core.command.ISubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MigrateCommand implements ISubCommand {

    private final SCPlugin plugin;

    public MigrateCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        return new ArrayList<>();
    }

    @Override
    public void run(CommandSender sender, String[] args) {

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getStoreMethod() == DataStoreMethod.YAML) {
                plugin.getCollectorManager().loadAllExistingYAML();
                plugin.setStoreMethod(DataStoreMethod.MYSQL);
                plugin.getCollectorManager().saveAll();
                plugin.getCollectorManager().load();
                sender.sendMessage("§aSuccessfully migrated from YAML to MySQL!");
            }
        });
    }

    @Override
    public String getDescription() {
        return "Migrate from YAML to MySQL";
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
