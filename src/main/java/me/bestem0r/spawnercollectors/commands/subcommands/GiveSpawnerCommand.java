package me.bestem0r.spawnercollectors.commands.subcommands;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.commands.CommandModule;
import me.bestem0r.spawnercollectors.commands.SubCommand;
import me.bestem0r.spawnercollectors.utils.ColorBuilder;
import me.bestem0r.spawnercollectors.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GiveSpawnerCommand implements SubCommand {

    private CommandModule module;
    private final SCPlugin plugin;

    public GiveSpawnerCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(int index, String[] args) {
        switch (index) {
            case 0:
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            case 1:
                return Stream.of(EntityType.values())
                        .filter(entityType -> entityType.name().startsWith(args[1]))
                        .map(EntityType::toString)
                        .collect(Collectors.toList());
            default:
                return new ArrayList<>();
        }
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (args.length > 4) {
            module.commandOutput(sender, ChatColor.RED + "Please specify player and entity type!");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            module.commandOutput(sender, ChatColor.RED + "Could not find player: " + args[1]);
            return;
        }
        if (!isEntity(args[2])) {
            module.commandOutput(sender, ChatColor.RED + "Could not find valid entity type: " + args[2]);
            return;
        }
        EntityType entityType = EntityType.valueOf(args[2]);
        int amount = 1;
        if (args.length == 4 && canConvert(args[3])) {
            amount = Integer.parseInt(args[3]);
        }
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        Collector collector = Methods.getCollector(plugin, target);
        if (collector.addSpawner(player, entityType, amount)) {
            module.commandOutput(sender, new ColorBuilder(plugin).path("messages.give_spawner").addPrefix().build());
        }
    }

    private boolean isEntity(String entityTest) {
        for (EntityType entityType : EntityType.values()) {
            if (entityType.name().equals(entityTest)) {
                return true;
            }
        }
        return false;
    }

    private boolean canConvert(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setModule(CommandModule module) {
        this.module = module;
    }

    @Override
    public String getDescription() {
        return "Give spawners to players";
    }
}
