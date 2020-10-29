package me.bestem0r.spawnercollectors;

import me.bestem0r.spawnercollectors.events.InventoryClick;
import me.bestem0r.spawnercollectors.menus.EntityMenu;
import me.bestem0r.spawnercollectors.menus.SpawnerMenu;
import me.bestem0r.spawnercollectors.utilities.Color;
import me.bestem0r.spawnercollectors.utilities.Methods;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Collector {

    private final File file;
    private final FileConfiguration config;
    private final OfflinePlayer owner;

    private final List<CollectorEntity> collectorEntities = new ArrayList<>();

    private Inventory spawnerMenu;
    private Inventory entityMenu;

    private boolean autoSell;

    public Collector(File file) {
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
        this.owner = Bukkit.getOfflinePlayer(UUID.fromString(config.getString("owner_uuid")));

        this.autoSell = config.getBoolean("auto_sell");

        //Loads values from config
        ConfigurationSection entitySection = config.getConfigurationSection("entities");
        if (entitySection != null) {
            for (String entityKey : entitySection.getKeys(false)) {
                int entityAmount = config.getInt("entities." + entityKey);
                int spawnerAmount = config.getInt("spawners." + entityKey);
                collectorEntities.add(new CollectorEntity(EntityType.valueOf(entityKey), entityAmount, spawnerAmount));
            }
        }
    }

    /** Runs when player interacts with spawner menu */
    public void spawnerMenuInteract(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        Player player = (Player) event.getWhoClicked();
        ItemStack currentItem = event.getCurrentItem();

        if (slot == 48) {
            event.getView().close();
            openEntityMenu(player);
            return;
        }
        if (slot == 50) {
            toggleAutoSell();
            updateSpawnerMenu();
            player.playSound(player.getLocation(), Sound.valueOf(SCPlugin.getInstance().getConfig().getString("sounds.toggle_auto_sell")), 1, 1);
            return;
        }
        //Add spawner
        if (slot >= event.getView().getTopInventory().getSize() && currentItem != null) {
            if (currentItem.getType() == Material.SPAWNER) {
                EntityType entityType = typeFromSpawner(currentItem);
                if (!SCPlugin.materials.containsKey(entityType)) {
                    player.sendMessage(new Color.Builder().path("messages.not_supported").addPrefix().build());
                    return;
                }

                player.getInventory().setItem(event.getSlot(), null);
                player.playSound(player.getLocation(), Sound.valueOf(SCPlugin.getInstance().getConfig().getString("sounds.add_spawner")), 1, 1);

                for (CollectorEntity spawner : collectorEntities) {
                    if (spawner.getEntityType() == entityType) {
                        spawner.addSpawner(currentItem.getAmount());
                        updateSpawnerMenu();
                        return;
                    }
                }
                collectorEntities.add(new CollectorEntity(entityType, 0, currentItem.getAmount()));
                updateSpawnerMenu();
            }
        }
        //Withdraw spawner
        if (slot < 54) {
            if (slot >= collectorEntities.size() || slot < 0) { return; }

            CollectorEntity collected = collectorEntities.get(slot);
            if (collected == null) { return; }

            int withdrawAmount = Math.min(collected.getSpawnerAmount(), 64);

            ItemStack spawner = spawnerFromType(collected.getEntityType(), withdrawAmount);
            ItemMeta meta = spawner.getItemMeta();
            meta.setDisplayName(ChatColor.RESET + WordUtils.capitalizeFully(collected.getEntityType().name().replaceAll("_", " ")) + " Spawner");
            spawner.setItemMeta(meta);

            player.getInventory().addItem(spawner);
            collected.removeSpawners(withdrawAmount);

            if (collected.getSpawnerAmount() < 1) {
                sell(player, collected);
                collectorEntities.remove(collected);
            }

            player.playSound(player.getLocation(), Sound.valueOf(SCPlugin.getInstance().getConfig().getString("sounds.withdraw")), 1, 1);
            updateSpawnerMenu();
        }
    }

    /** Runs when player interacts with entity menu */
    public void entityMenuInteract(InventoryClickEvent event) {
        int slot = event.getRawSlot();

        Player player = (Player) event.getWhoClicked();

        if (slot == 48) {
            openSpawnerMenu(player);
            return;
        }
        if (slot == 50) {
            toggleAutoSell();
            updateEntityMenu();
            player.playSound(player.getLocation(), Sound.valueOf(SCPlugin.getInstance().getConfig().getString("sounds.toggle_auto_sell")), 1, 1);
            return;
        }
        if (slot >= collectorEntities.size() || slot < 0) {
            return;
        }
        CollectorEntity collected = collectorEntities.get(slot);
        if (collected == null) { return; }


        //Sell
        if (event.getClick() == ClickType.LEFT) {
            sell(player, collected);
        }
        //Withdraw
        if (event.getClick() == ClickType.RIGHT) {
            int withdrawAmount = Math.min(collected.getEntityAmount(), 64);
            collected.removeEntities(withdrawAmount);
            for (ItemStack itemStack : Methods.lootFromType(collected.getEntityType(), player, withdrawAmount)) {
                player.getInventory().addItem(itemStack);
            }
            player.playSound(player.getLocation(), Sound.valueOf(SCPlugin.getInstance().getConfig().getString("sounds.withdraw")), 1, 1);
        }
        updateEntityMenu();
    }

    /** Sells all entities from collected */
    private void sell(Player player, CollectorEntity collected) {
        Economy economy = SCPlugin.getEconomy();
        economy.depositPlayer(player, SCPlugin.prices.get(collected.getEntityType()) * collected.getEntityAmount());

        player.playSound(player.getLocation(), Sound.valueOf(SCPlugin.getInstance().getConfig().getString("sounds.sell")), 1, 1);
        player.sendMessage(new Color.Builder().path("messages.sell")
                .replaceWithCurrency("%worth%", String.valueOf(collected.getTotalWorth()))
                .addPrefix()
                .build());

        collected.removeEntities(collected.getEntityAmount());
    }

    /** Returns entityType based on spawner itemStack */
    private EntityType typeFromSpawner(ItemStack itemStack) {
        if (itemStack.getType() == Material.SPAWNER) {
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
    /** Returns spawner with set EntityType */
    private ItemStack spawnerFromType(EntityType entityType, int amount) {
        ItemStack itemStack = new ItemStack(Material.SPAWNER, amount);

        //Lots of more casting...
        ItemMeta itemMeta = itemStack.getItemMeta();
        BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
        BlockState blockState = blockStateMeta.getBlockState();
        CreatureSpawner spawner = (CreatureSpawner) blockState;
        spawner.setSpawnedType(entityType);
        blockStateMeta.setBlockState(blockState);

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    /** Attempts to spawn virtual mobs */
    public void attemptSpawn() {
        for (CollectorEntity collectorEntity : collectorEntities) {
            collectorEntity.attemptSpawn(autoSell, owner);
        }
        updateEntityMenuIfView();
    }

    /** Toggles auto-sell */
    private void toggleAutoSell() {
        autoSell = !autoSell;
        updateEntityMenu();
    }

    /** Returns spawner Inventory */
    public void openSpawnerMenu(Player player) {
        if (this.spawnerMenu == null) {
            this.spawnerMenu = SpawnerMenu.create(collectorEntities, autoSell);
        } else {
            this.spawnerMenu.setContents(SpawnerMenu.create(collectorEntities, autoSell).getContents());
        }
        player.openInventory(spawnerMenu);
        player.playSound(player.getLocation(), Sound.valueOf(SCPlugin.getInstance().getConfig().getString("sounds.spawners_open")), 1, 1);

        Bukkit.getPluginManager().registerEvents(new InventoryClick(this, player, InventoryClick.Menu.SPAWNER), SCPlugin.getInstance());
    }
    /** Returns entity Inventory */
    public void openEntityMenu(Player player) {
        if (this.entityMenu == null) {
            this.entityMenu = EntityMenu.create(collectorEntities, autoSell);
        } else {
            this.entityMenu.setContents(EntityMenu.create(collectorEntities, autoSell).getContents());
        }
        player.openInventory(entityMenu);
        player.playSound(player.getLocation(), Sound.valueOf(SCPlugin.getInstance().getConfig().getString("sounds.mobs_open")), 1, 1);
        Bukkit.getPluginManager().registerEvents(new InventoryClick(this, player, InventoryClick.Menu.ENTITY), SCPlugin.getInstance());
    }

    /** Updates content of spawner menu */
    private void updateSpawnerMenu() {
        this.spawnerMenu.setContents(SpawnerMenu.create(collectorEntities, autoSell).getContents());
    }

    /** Updates content of entity menu */
    private void updateEntityMenu() {
        this.entityMenu.setContents(EntityMenu.create(collectorEntities, autoSell).getContents());
    }

    /** Updates spawner menu if a player is currently viewing it */
    private void updateEntityMenuIfView() {
        if (entityMenu != null) {
            if (entityMenu.getViewers().size() > 0) {
                updateEntityMenu();
            }
        }
    }

    /** Saves collector */
    public void save() {
        config.set("spawners", "");
        config.set("entities", "");
        for (CollectorEntity spawner : collectorEntities) {
            config.set("spawners." + spawner.getEntityType().name(), spawner.getSpawnerAmount());
            config.set("entities." + spawner.getEntityType().name(), spawner.getEntityAmount());
        }
        config.set("auto_sell", autoSell);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Getters */
    public OfflinePlayer getOwner() {
        return owner;
    }
}
