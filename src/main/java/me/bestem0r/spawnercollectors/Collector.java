package me.bestem0r.spawnercollectors;

import com.cryptomorin.xseries.XMaterial;
import me.bestem0r.spawnercollectors.events.InventoryClick;
import me.bestem0r.spawnercollectors.menus.EntityMenu;
import me.bestem0r.spawnercollectors.menus.SpawnerMenu;
import me.bestem0r.spawnercollectors.utils.ColorBuilder;
import me.bestem0r.spawnercollectors.utils.Database;
import me.bestem0r.spawnercollectors.utils.Methods;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class Collector {

    private File file;
    private FileConfiguration config;
    private final Player owner;

    private final List<EntityCollector> collectorEntities = new ArrayList<>();

    private Inventory spawnerMenu;
    private Inventory entityMenu;
    
    private final SCPlugin plugin;
    private Connection connection;

    private boolean autoSell;

    public Collector(SCPlugin plugin, Player player) {
        this.plugin = plugin;
        this.owner = player;

        switch (plugin.getStoreMethod()) {
            case MYSQL:
                loadMYSQL();
                break;
            case YAML:
                loadYAML();
        }
    }

    private void loadYAML() {
        this.file = new File(plugin.getDataFolder() + "/collectors/" + owner.getUniqueId() + ".yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.autoSell = config.getBoolean("auto_sell");

        ConfigurationSection entitySection = config.getConfigurationSection("entities");
        if (entitySection != null) {
            for (String entityKey : entitySection.getKeys(false)) {
                int entityAmount = config.getInt("entities." + entityKey);
                int spawnerAmount = config.getInt("spawners." + entityKey);
                EntityType entityType = EntityType.valueOf(entityKey);
                collectorEntities.add(new EntityCollector(plugin, entityType, entityAmount, spawnerAmount));
            }
        }
    }
    private void loadMYSQL() {
        this.connection = Database.getDataBaseConnection();
        try {
            String playerQuery = "SELECT * FROM player_data WHERE owner_uuid = '" + owner.getUniqueId().toString() + "'";
            PreparedStatement preparedPlayerStatement = connection.prepareStatement(playerQuery);

            ResultSet playerResult = preparedPlayerStatement.executeQuery();
            while (playerResult.next()) {
                this.autoSell = playerResult.getBoolean("auto_sell");
            }

            String entityQuery = "SELECT * FROM entity_data WHERE owner_uuid = '" + owner.getUniqueId().toString() + "'";
            PreparedStatement preparedEntityStatement = connection.prepareStatement(entityQuery);

            ResultSet entitySet = preparedEntityStatement.executeQuery();
            while (entitySet.next()) {
                int entityAmount = entitySet.getInt("entity_amount");
                int spawnerAmount = entitySet.getInt("spawner_amount");
                EntityType entityType = EntityType.valueOf(entitySet.getString("entity_type"));
                collectorEntities.add(new EntityCollector(plugin, entityType, entityAmount, spawnerAmount));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Runs when player interacts with spawner menu */
    public void spawnerMenuInteract(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        if (slot == 48) {
            if (plugin.isMorePermissions() && !player.hasPermission("spawnercollectors.command.mobs")) {
                player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_command").addPrefix().build());
            } else {
                openEntityMenu(player);
            }
            return;
        }
        if (slot == 49) {
            sellAll(player);
            return;
        }
        if (slot == 50) {
            toggleAutoSell(player);
            return;
        }
        //Add spawner
        if (slot >= event.getView().getTopInventory().getSize() && currentItem != null) {
            if (currentItem.getType() == XMaterial.SPAWNER.parseMaterial()) {
                EntityType entityType = typeFromSpawner(currentItem);
                if (addSpawner(player, entityType, currentItem.getAmount())) {
                    player.getInventory().setItem(event.getSlot(), null);
                }
            }
        }
        //Withdraw spawner
        if (slot < 54) {
            if (slot >= collectorEntities.size() || slot < 0) { return; }
            boolean morePermissions = plugin.isMorePermissions();
            if (morePermissions && !player.hasPermission("spawnercollectors.withdraw.spawner")) {
                player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_withdraw_spawner").addPrefix().build());
                return;
            }

            EntityCollector collected = collectorEntities.get(slot);
            if (collected == null) { return; }

            int withdrawAmount = Math.min(collected.getSpawnerAmount(), 64);

            ItemStack spawner = Methods.spawnerFromType(plugin, collected.getEntityType(), withdrawAmount);

            Map<Integer, ItemStack> drop = player.getInventory().addItem(spawner);
            for (int i : drop.keySet()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop.get(i));
            }
            collected.removeSpawners(withdrawAmount);

            if (collected.getSpawnerAmount() < 1) {
                sell(player, collected);
                collectorEntities.remove(collected);
            }

            Methods.playSound(plugin, player, "withdraw");
            plugin.log.add(ChatColor.stripColor(new Date().toString() + ": " + player.getName() + " withdrew " + withdrawAmount + " " + collected.getEntityType() + " Spawner"));
            updateSpawnerMenu();
        }
    }

    /** Adds spawner */
    public boolean addSpawner(Player player, EntityType entityType, int amount) {
        if (!this.plugin.materials.containsKey(entityType) && player != null) {
            player.sendMessage((new ColorBuilder(this.plugin)).path("messages.not_supported").addPrefix().build());
            return false;
        }
        //Check if owner has permission for mob
        if (player != null && this.plugin.isMorePermissions() && !this.owner.isOp() && this.owner.getEffectivePermissions().stream()
                .noneMatch((s) -> s.getPermission().startsWith("spawnercollectors.spawner." + entityType.name().toLowerCase()))) {

            player.sendMessage((new ColorBuilder(this.plugin)).path("messages.no_permission_mob").addPrefix().build());
            return false;
        }
        //Check if amount is exceeding global max limit
        if (player != null && this.plugin.getMaxSpawners() > 0 && this.plugin.getMaxSpawners() < amount && !this.owner.hasPermission("spawnercollectors.bypass_limit")) {
            player.sendMessage((new ColorBuilder(this.plugin)).path("messages.reached_max_spawners").replace("%max%", String.valueOf(this.plugin.getMaxSpawners())).addPrefix().build());
            return false;
        }

        int max = 0;
        if (player != null) {
            max = this.owner.getEffectivePermissions().stream()
                    .filter((s) -> s.getPermission().startsWith("spawnercollectors.spawner." + entityType.name().toLowerCase()))
                    .filter((s) -> s.getPermission().length() > 26 + entityType.name().length() + 1)
                    .map((s) -> s.getPermission().substring(26 + entityType.name().length() + 1))
                    .mapToInt(Integer::parseInt)
                    .max()
                    .orElse(0);
        }

        //Check if amount is exceeding per-mob permission limit
        if (player != null && this.plugin.isMorePermissions() && !owner.isOp() && max != 0 && max < amount) {
            player.sendMessage((new ColorBuilder(this.plugin)).path("messages.reached_max_spawners").replace("%max%", String.valueOf(max)).addPrefix().build());
            return false;
        }

        Optional<EntityCollector> optionalCollector = this.collectorEntities.stream().filter((c) -> c.getEntityType() == entityType).findAny();

        if (optionalCollector.isPresent()) {

            EntityCollector collector = optionalCollector.get();
            //Check if new amount will exceed global max limit
            if (player != null && this.plugin.getMaxSpawners() > 0 && this.plugin.getMaxSpawners() < amount + collector.getSpawnerAmount() && !this.owner.hasPermission("spawnercollectors.bypass_limit")) {
                player.sendMessage((new ColorBuilder(this.plugin)).path("messages.reached_max_spawners").replace("%max%", String.valueOf(this.plugin.getMaxSpawners())).addPrefix().build());
                return false;
            }
            //Check if new amount will exceed per-mob permission limit
            if (player != null && this.plugin.isMorePermissions() && !owner.isOp() && max != 0 && max < amount + collector.getSpawnerAmount()) {
                player.sendMessage((new ColorBuilder(this.plugin)).path("messages.reached_max_spawners").replace("%max%", String.valueOf(max)).addPrefix().build());
                return false;
            }

            collector.addSpawner(amount);
        } else {
            this.collectorEntities.add(new EntityCollector(this.plugin, entityType, 0, amount));
        }

        if (player != null) {
            Methods.playSound(this.plugin, player, "add_spawner");
        }

        String spawnerName = ChatColor.RESET + WordUtils.capitalizeFully(entityType.name().replaceAll("_", " ")) + " Spawner";
        String giverName = player == null ? "Console" : player.getName();
        this.plugin.log.add(ChatColor.stripColor((new Date()).toString() + ": " + giverName + " added " + amount + " " + spawnerName + " to " + this.owner.getName() + "'s collector!"));
        this.updateSpawnerMenuIfView();
        this.updateEntityMenuIfView();
        return true;
    }

    /** Runs when player interacts with entity menu */
    public void entityMenuInteract(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        Player player = (Player) event.getWhoClicked();

        if (slot == 48) {
            if (plugin.isMorePermissions() && !player.hasPermission("spawnercollectors.command.spawners")) {
                player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_command").addPrefix().build());
            } else {
                openSpawnerMenu(player);
            }
            return;
        }
        if (slot == 49) {
            sellAll(player);
            return;
        }
        if (slot == 50) {
            toggleAutoSell(player);
            return;
        }
        if (slot >= collectorEntities.size() || slot < 0) {
            return;
        }
        EntityCollector collected = collectorEntities.get(slot);
        if (collected == null) { return; }


        //Sell
        if (event.getClick() == ClickType.LEFT) {
            sell(player, collected);
        }
        //Withdraw
        if (event.getClick() == ClickType.RIGHT) {

            boolean morePermissions = plugin.isMorePermissions();
            if (morePermissions && !player.hasPermission("spawnercollectors.withdraw.mob")) {
                player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_withdraw_mob").addPrefix().build());
                return;
            }
            int withdrawAmount = Math.min(collected.getEntityAmount(), 64);
            if (XMaterial.isNewVersion() || plugin.getCustomLoot().containsKey(collected.getEntityType())) {
                collected.removeEntities(withdrawAmount);
            }

            for (ItemStack itemStack : Methods.lootFromType(plugin, collected.getEntityType(), player, withdrawAmount)) {
                Map<Integer, ItemStack> drop = player.getInventory().addItem(itemStack);
                for (int i : drop.keySet()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop.get(i));
                }
            }
            if (this.plugin.doGiveXP() && (!this.plugin.isMorePermissions() || player.hasPermission("spawnercollectors.receive_xp"))) {

                for (EntityExperience e : EntityExperience.values()) {
                    if (e.name().equals(collected.getEntityType().name())) {
                        player.giveExp(e.getRandomAmount(withdrawAmount));
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                        break;
                    }
                }
            }
            Methods.playSound(plugin, player, "withdraw");
            updateEntityMenu();
        }
    }

    /** Sells every collected entity in every collected */
    private void sellAll(Player player) {
        boolean morePermissions = plugin.isMorePermissions();
        if (morePermissions && !player.hasPermission("spawnercollectors.sell")) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_sell").addPrefix().build());
            return;
        }

        Economy economy = plugin.getEconomy();
        double total = 0;
        for (EntityCollector collected : collectorEntities) {
            total += collected.getTotalWorth();
            collected.removeEntities(collected.getEntityAmount());
        }
        economy.depositPlayer(player, total);

        Methods.playSound(plugin, player, "sell");
        player.sendMessage(new ColorBuilder(plugin).path("messages.sell_all")
                .replaceWithCurrency("%worth%", String.valueOf(total))
                .addPrefix().build());

        updateSpawnerMenuIfView();
        updateEntityMenuIfView();
    }

    /** Sells all entities from specified collected */
    private void sell(Player player, EntityCollector collected) {
        boolean morePermissions = plugin.isMorePermissions();
        if (morePermissions && !player.hasPermission("spawnercollectors.sell")) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_sell").addPrefix().build());
            return;
        }
        Economy economy = plugin.getEconomy();
        economy.depositPlayer(player, collected.getTotalWorth());

        Methods.playSound(plugin, player, "sell");
        player.sendMessage(new ColorBuilder(plugin).path("messages.sell")
                .replaceWithCurrency("%worth%", String.valueOf(collected.getTotalWorth()))
                .addPrefix()
                .build());

        collected.removeEntities(collected.getEntityAmount());
        updateEntityMenuIfView();
        updateSpawnerMenuIfView();

    }

    /** Returns entityType based on spawner itemStack */
    private EntityType typeFromSpawner(ItemStack itemStack) {
        if (itemStack.getType() == XMaterial.SPAWNER.parseMaterial()) {
            //Lots of casting...
            ItemMeta itemMeta = itemStack.getItemMeta();
            BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
            BlockState blockState = blockStateMeta.getBlockState();
            CreatureSpawner spawner = (CreatureSpawner) blockState;
            return spawner.getSpawnedType();
        } else {
            return null;
        }
    }

    /** Attempts to spawn virtual mobs */
    public void attemptSpawn() {
        for (EntityCollector entityCollector : collectorEntities) {
            entityCollector.attemptSpawn(autoSell, owner);
        }
        updateSpawnerMenuIfView();
        updateEntityMenuIfView();
    }

    /** Toggles auto-sell */
    private void toggleAutoSell(Player player) {
        boolean morePermissions = plugin.isMorePermissions();
        if (morePermissions && !player.hasPermission("spawnercollectors.auto_sell")) {
            player.sendMessage(new ColorBuilder(plugin).path("messages.no_permission_auto-sell").addPrefix().build());
            return;
        }

        autoSell = !autoSell;

        updateEntityMenuIfView();
        updateSpawnerMenuIfView();
        Methods.playSound(plugin, player, "toggle_auto_sell");
    }

    /** Returns spawner Inventory */
    public void openSpawnerMenu(Player player) {
        if (this.spawnerMenu == null) {
            this.spawnerMenu = SpawnerMenu.create(plugin, collectorEntities, autoSell);
        } else {
            updateSpawnerMenu();
        }
        player.openInventory(spawnerMenu);
        Methods.playSound(plugin, player, "spawners_open");

        Bukkit.getPluginManager().registerEvents(new InventoryClick(this, player, InventoryClick.Menu.SPAWNER), plugin);
    }
    /** Returns entity Inventory */
    public void openEntityMenu(Player player) {
        if (this.entityMenu == null) {
            this.entityMenu = EntityMenu.create(plugin, collectorEntities, autoSell);
        } else {
            updateEntityMenu();
        }
        player.openInventory(entityMenu);
        Methods.playSound(plugin, player, "mobs_open");
        Bukkit.getPluginManager().registerEvents(new InventoryClick(this, player, InventoryClick.Menu.ENTITY), plugin);
    }

    /** Updates content of spawner menu */
    private void updateSpawnerMenu() {
        if (spawnerMenu == null) { return; }
        this.spawnerMenu.setContents(SpawnerMenu.create(plugin, collectorEntities, autoSell).getContents());
    }

    /** Updates spawner menu if a player is currently viewing it */
    private void updateSpawnerMenuIfView() {
        if (spawnerMenu != null) {
            if (spawnerMenu.getViewers().size() > 0) {
                updateSpawnerMenu();
            }
        }
    }

    /** Updates content of entity menu */
    private void updateEntityMenu() {
        if (entityMenu == null) { return; }
        this.entityMenu.setContents(EntityMenu.create(plugin, collectorEntities, autoSell).getContents());
    }

    /** Updates entity menu if a player is currently viewing it */
    private void updateEntityMenuIfView() {
        if (entityMenu != null) {
            if (entityMenu.getViewers().size() > 0) {
                updateEntityMenu();
            }
        }
    }

    /** Saves collector */
    public void save() {
        switch (plugin.getStoreMethod()) {
            case YAML:
                config.set("spawners", "");
                config.set("entities", "");
                for (EntityCollector spawner : collectorEntities) {
                    config.set("spawners." + spawner.getEntityType().name(), spawner.getSpawnerAmount());
                    config.set("entities." + spawner.getEntityType().name(), spawner.getEntityAmount());
                }
                config.set("auto_sell", autoSell);
                try {
                    config.save(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case MYSQL:
                try {
                    this.connection = Database.getDataBaseConnection();
                    String update = "replace into player_data (owner_uuid, auto_sell) values ('" + owner.getUniqueId().toString() + "', " + autoSell + ")";
                    PreparedStatement updateStatement = connection.prepareStatement(update);
                    updateStatement.execute();

                    String delete = "delete from entity_data where owner_uuid = '" + owner.getUniqueId().toString() + "'";
                    PreparedStatement deleteStatement = connection.prepareStatement(delete);
                    deleteStatement.execute();

                    for (EntityCollector spawner : collectorEntities) {
                        String insert = " insert into entity_data (owner_uuid, entity_type, spawner_amount, entity_amount)"
                                + " values (?, ?, ?, ?)";

                        PreparedStatement insertStatement = connection.prepareStatement(insert);
                        insertStatement.setString(1, owner.getUniqueId().toString());
                        insertStatement.setString(2, spawner.getEntityType().name());
                        insertStatement.setInt(3, spawner.getSpawnerAmount());
                        insertStatement.setInt(4, spawner.getEntityAmount());

                        insertStatement.execute();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                throw new IllegalStateException("Invalid data storage method!");
        }
    }

    public OfflinePlayer getOwner() {
        return owner;
    }
}
