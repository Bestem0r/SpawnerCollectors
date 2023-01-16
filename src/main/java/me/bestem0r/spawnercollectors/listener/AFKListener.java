package me.bestem0r.spawnercollectors.listener;

import me.bestem0r.spawnercollectors.SCPlugin;
import net.bestemor.core.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Instant;
import java.util.*;

public class AFKListener implements Listener {

    private final SCPlugin plugin;

    private final boolean afkCheck;
    private final int time;

    private final Map<UUID, Instant> lastMove = new HashMap<>();
    private final List<UUID> afkPlayers = new ArrayList<>();

    public AFKListener(SCPlugin plugin) {
        this.plugin = plugin;

        this.afkCheck = plugin.getConfig().getBoolean("afk.enable");
        this.time = plugin.getConfig().getInt("afk.time");

        if (afkCheck) {
            runChecker();
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        if (afkCheck) {
            Player player = event.getPlayer();

            if (afkPlayers.contains(player.getUniqueId())) {
                String message = ConfigManager.getMessage("messages.no_longer_afk");
                if (!message.equals("")) {
                    player.sendMessage(message);
                }
                afkPlayers.remove(player.getUniqueId());
            }

            lastMove.put(player.getUniqueId(), Instant.now());

        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (afkCheck) {
            lastMove.remove(event.getPlayer().getUniqueId());
            afkPlayers.remove(event.getPlayer().getUniqueId());
        }
    }

    private void runChecker() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (UUID uuid : lastMove.keySet()) {
                if (!afkPlayers.contains(uuid) && lastMove.get(uuid).plusSeconds(time).isBefore(Instant.now())) {
                    afkPlayers.add(uuid);
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        player.sendMessage(ConfigManager.getMessage("messages.afk"));
                    }
                }
            }
        }, 20L, 20L);
    }

    public boolean isAFK(Player player) {
        return afkPlayers.contains(player.getUniqueId());
    }

    public boolean isAfkCheck() {
        return afkCheck;
    }
}
