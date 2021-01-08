package me.bestem0r.spawnercollectors.commands.subcommands;

import me.bestem0r.spawnercollectors.Collector;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.commands.CommandModule;
import me.bestem0r.spawnercollectors.commands.SubCommand;
import me.bestem0r.spawnercollectors.utils.ColorBuilder;
import me.bestem0r.spawnercollectors.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GiveSpawnerCommand implements SubCommand {
    private CommandModule module;
    private final SCPlugin plugin;

    public GiveSpawnerCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> getCompletion(int index, String[] args) {
        switch(index) {
            case 0:
                return Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).collect(Collectors.toList());
            case 1:
                return Arrays.asList("hand", "gui");
            case 2:
                return Stream.of(EntityType.values()).filter((entityType) -> {
                    return entityType.name().startsWith(args[2]);
                }).map(Enum::toString).collect(Collectors.toList());
            default:
                return new ArrayList<>();
        }
    }

    public void run(CommandSender sender, String[] args) {
        if (args.length != 4 && args.length != 5) {
            this.module.commandOutput(sender, ChatColor.RED + "Please specify player and entity type!");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            this.module.commandOutput(sender, ChatColor.RED + "Could not find player: " + args[1]);
            return;
        }
        if (!args[2].equals("hand") && !args[2].equals("gui")) {
            this.module.commandOutput(sender, ChatColor.RED + "Please specify where to give the spawners!");
            return;
        }
        if (!this.isEntity(args[3])) {
            this.module.commandOutput(sender, ChatColor.RED + "Could not find valid entity type: " + args[2]);
            return;
        }

        EntityType entityType = EntityType.valueOf(args[3]);
        int amount = 1;
        if (args.length == 5 && this.canConvert(args[4])) {
            amount = Integer.parseInt(args[4]);
        }

        switch(args[2]) {
            case "hand":
                target.getInventory().addItem(Methods.spawnerFromType(this.plugin, entityType, amount));
                target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 1);
                break;
            case "gui":
                Player player = null;
                if (sender instanceof Player) {
                    player = (Player)sender;
                }

                Collector collector = Methods.getCollector(this.plugin, target);
                if (collector.addSpawner(player, entityType, amount)) {
                    this.module.commandOutput(sender, (new ColorBuilder(this.plugin)).path("messages.give_spawner").addPrefix().build());
                }
        }
    }

    private boolean isEntity(String entityTest) {
        EntityType[] var2 = EntityType.values();

        for (EntityType entityType : var2) {
            if (entityType.name().equals(entityTest)) {
                return true;
            }
        }

        return false;
    }

    private boolean canConvert(String string) {
        for(int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }

        return true;
    }

    public void setModule(CommandModule module) {
        this.module = module;
    }

    public String getDescription() {
        return "Give spawners to players";
    }
}
