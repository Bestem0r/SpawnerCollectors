package me.bestem0r.spawnercollectors.collector;

import me.bestem0r.spawnercollectors.CustomEntityType;
import me.bestem0r.spawnercollectors.SCPlugin;
import me.bestem0r.spawnercollectors.utils.SpawnerUtils;
import net.bestemor.core.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.bestem0r.spawnercollectors.DataStoreMethod.YAML;

public class CollectorManager {

    private final SCPlugin plugin;

    private final Map<String, Collector> collectors = new ConcurrentHashMap<>();


    private final Map<UUID, Double> earned = new HashMap<>();

    private final File placedFile;
    private final FileConfiguration placedConfig;

    private Runnable spawnerThread;
    private Runnable messageThread;

    private long autoSave;
    private Instant nextAutoSave = Instant.now();

    public CollectorManager(SCPlugin plugin) {
        this.plugin = plugin;

        this.placedFile = new File(plugin.getDataFolder(), "placed_collectors.yml");
        this.placedConfig = YamlConfiguration.loadConfiguration(placedFile);
    }

    public void load() {
        collectors.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String uuid = player.getUniqueId().toString();
            collectors.put(uuid, new Collector(plugin, uuid, player));
        }

        if (plugin.getConfig().getLong("auto_save") > 0) {
            this.autoSave = plugin.getConfig().getLong("auto_save");
            startAutoSave();
        }

        for (String uuid : placedConfig.getKeys(false)) {
            try {
                Location location = SpawnerUtils.locationFromBase64(uuid);
                String ownerUUID = placedConfig.getString(uuid + ".owner");
                assert ownerUUID != null;
                OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerUUID));
                collectors.put(uuid, new PlacedCollector(plugin, location, owner));
            } catch (Exception e) {
                Bukkit.getLogger().severe("Unable to load placed collector at " + uuid + "!");
                e.printStackTrace();
            }
        }
    }

    public void load(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        collectors.put(uuid, new Collector(plugin, uuid, player));
    }

    public void unload(OfflinePlayer player) {
        String uuid = player.getUniqueId().toString();
        Collector collector = collectors.remove(uuid);
        if (collector != null) {
            collector.saveAsync();
        }
    }

    public void loadAllExistingYAML() {
        if (plugin.getStoreMethod() == YAML) {
            collectors.values().forEach(Collector::saveSync);
            for (File file : new File(plugin.getDataFolder() + "/collectors/").listFiles()) {
                String uuid = file.getName().split("\\.")[0];
                if (!collectors.containsKey(uuid)) {
                    collectors.put(uuid, new Collector(plugin, uuid, Bukkit.getOfflinePlayer(UUID.fromString(uuid))));
                }
            }
        }
    }

    public void saveAll() {
        collectors.values().forEach(Collector::saveSync);
    }

    private void startAutoSave() {
        long intervalTicks = 200L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (Instant.now().isAfter(nextAutoSave)) {

                int numberOfCollectors = collectors.size();
                int secondsPerCollector = (int) Math.floor((double) autoSave / numberOfCollectors);
                int millisecondsPerCollector = (int) Math.floor((double) (autoSave % numberOfCollectors) * 1000 / numberOfCollectors);

                List<Collector> toSave = new ArrayList<>(collectors.values());
                Collections.shuffle(toSave);
                for (int i = 0; i < numberOfCollectors; i++) {
                    toSave.get(i).setNextAutoSave(Instant.now()
                            .plusSeconds(secondsPerCollector)
                            .plusMillis(millisecondsPerCollector));
                }

                nextAutoSave = Instant.now().plusSeconds(autoSave);
            }

            for (Collector collector : collectors.values()) {
                if (collector.getNextAutoSave().isBefore(Instant.now())) {
                    collector.saveAsync();
                }
            }

        }, intervalTicks, intervalTicks);
    }

    /** Starts async thread to replicate spawner mechanics */
    public void startSpawners() {
        long timer = 20L * plugin.getConfig().getInt("spawn_interval");

        if (spawnerThread != null) {
            Bukkit.getScheduler().cancelTask(spawnerThread.hashCode());
        }

        this.spawnerThread = () -> {
            try {
                for (Collector collector : collectors.values()) {
                    collector.attemptSpawn();
                }
            } catch (ConcurrentModificationException e) {
                Bukkit.getLogger().severe("[SpawnerCollectors] ConcurrentModificationException in spawner thread!");
            }

        };

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this.spawnerThread, timer, timer);
    }
    /** Async message thread for earned money by auto-sell */
    public void startMessages() {
        int minutes = plugin.getConfig().getInt("notify_interval");
        if (messageThread != null) {
            Bukkit.getScheduler().cancelTask(messageThread.hashCode());
        }
        if (minutes > 0) {
            this.messageThread = () -> {
                earned.remove(null);
                for (UUID uuid : earned.keySet()) {

                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {

                        double playerEarned = Math.round(earned.get(uuid) * 100.0) / 100.0;
                        player.sendMessage(ConfigManager.getCurrencyBuilder("messages.earned_notify")
                                .replaceCurrency("%worth%", BigDecimal.valueOf(playerEarned))
                                .replace("%time%", String.valueOf(minutes))
                                .addPrefix()
                                .build());
                        SpawnerUtils.playSound(player, "notification");
                    }
                }
                earned.clear();
            };
            long time = (long) minutes * 20 * 60;
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, messageThread, time, time);
        }
    }


    /** Earned message methods */
    public void addEarned(OfflinePlayer player, double amount) {
        if (earned.containsKey(player.getUniqueId()) && earned.get(player.getUniqueId()) != null) {
            earned.replace(player.getUniqueId(), earned.get(player.getUniqueId()) + amount);
        } else {
            earned.put(player.getUniqueId(), amount);
        }
    }

    public boolean addPlacedCollector(Location location, Player owner, EntityType type) {
        String uuid = SpawnerUtils.locationToBase64(location);
        PlacedCollector collector = new PlacedCollector(plugin, location, owner);
        collector.loaded = true;
        if (collector.addSpawner(null, new CustomEntityType(type), 1)) {

            collectors.put(uuid, collector);

            placedConfig.set(uuid + ".owner", owner.getUniqueId().toString());
            savePlacedFile();

            return true;
        } else {
            return false;
        }
    }

    public void removePlacedCollector(Location location, Player player) {
        String uuid = SpawnerUtils.locationToBase64(location);
        Collector collector = collectors.remove(uuid);
        if (collector == null) {
            return;
        }
        collector.sellAll(player);
        collector.delete();
        placedConfig.set(uuid, null);

        savePlacedFile();
    }

    private void savePlacedFile() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                placedConfig.save(placedFile);
            } catch (Exception e) {
                Bukkit.getLogger().severe("Unable to save placed collectors file!");
                e.printStackTrace();
            }
        });
    }

    public void openPlacedCollector(Location location, Player player) {
        String uuid = SpawnerUtils.locationToBase64(location);
        Collector collector = collectors.get(uuid);
        if (collector != null) {
            boolean spawnerBreak = ConfigManager.getBoolean("enable_spawner_breaking");
            if (!spawnerBreak && !collector.getOwner().getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(ConfigManager.getCurrencyBuilder("messages.not_owner").addPrefix().build());
                return;
            }
            collector.openEntityMenu(player);
        }
    }

    public Collector getCollector(String string) {
        return collectors.get(string);
    }
}
