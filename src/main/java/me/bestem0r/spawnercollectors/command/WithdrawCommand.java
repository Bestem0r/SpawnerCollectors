package me.bestem0r.spawnercollectors.command;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.collector.Collector;
import me.bestem0r.spawnercollectors.collector.EntityCollector;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.command.ISubCommand;
import net.bestemor.core.config.ConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WithdrawCommand implements ISubCommand {

    private final SCPlugin plugin;

    public WithdrawCommand(SCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> getCompletion(String[] args) {
        switch (args.length) {
            case 2:
                return Stream.of(EntityType.values())
                        .filter((entityType) -> entityType.name().startsWith(args[1].toUpperCase(Locale.ROOT)))
                        .map(Enum::toString).map(String::toLowerCase).collect(Collectors.toList());
            case 3:
                return Collections.singletonList("<amount>");
        }
        return null;
    }

    @Override
    public void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return;
        }
        Player player = (Player) sender;
        if (args.length != 3) {
            player.sendMessage("Usage: /spawnercollectors withdraw <entity_type> <amount>");
            return;
        }
        if (!isEntityType(args[1])) {
            player.sendMessage("Invalid entity type: " + args[1]);
            return;
        }
        EntityType entityType = EntityType.valueOf(args[1].toUpperCase(Locale.ROOT));

        if (!isNumber(args[2])) {
            player.sendMessage("Invalid amount: " + args[2]);
            return;
        }
        long amount = Long.parseLong(args[2]);
        if (amount > 100 || amount < 0) {
            player.sendMessage("You can only withdraw up to 100 entities at a time!");
            return;
        }

        boolean morePermissions = ConfigManager.getBoolean("more_permissions");
        if (morePermissions && !player.hasPermission("spawnercollectors.withdraw.mob")) {
            player.sendMessage(ConfigManager.getMessage("messages.no_permission_withdraw_mob"));
            return;
        }
        CustomEntityType type = new CustomEntityType(entityType);

        Collector collector = SpawnerUtils.getCollector(plugin, player);
        EntityCollector entityCollector = collector.getEntityCollector(type);
        if (entityCollector == null) {
            player.sendMessage("You don't have any " + type.name() + " spawners in your collector!");
            return;
        }
        entityCollector.withdraw(player, amount);

    }

    @Override
    public String getDescription() {
        return "Withdraw entities from your collector";
    }

    @Override
    public String getUsage() {
        return "<entity_type> <amount>";
    }

    private boolean isEntityType(String entityType) {
        return Arrays.stream(EntityType.values()).anyMatch(type -> type.name().equalsIgnoreCase(entityType));
    }

    private boolean isNumber(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
