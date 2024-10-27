package me.bestem0r.spawnercollectors.listener;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.events.CMIAfkEnterEvent;
import com.Zrips.CMI.events.CMIAfkLeaveEvent;
import me.bestem0r.spawnercollectors.SCPlugin;
import net.bestemor.core.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

        if (!afkCheck) {
            return;
        }

        if (ConfigManager.getBoolean("afk.cmi") && Bukkit.getPluginManager().isPluginEnabled("CMI")) {
            Bukkit.getPluginManager().registerEvents(new CMIListener(), plugin);
        } else {
            runChecker();
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        if (event.getTo() == null) {
            return;
        }

        if (afkCheck && !Bukkit.getPluginManager().isPluginEnabled("CMI")) {
            Player player = event.getPlayer();

            Location to = event.getTo();
            if (to.getDirection().equals(event.getFrom().getDirection())) {
                return;
            }

            // Calculate velocity
            double velocity = Math.sqrt(Math.pow(to.getX() - event.getFrom().getX(), 2) + Math.pow(to.getZ() - event.getFrom().getZ(), 2));
            if (velocity < 0.15) {
                return;
            }

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
        if (Bukkit.getPluginManager().isPluginEnabled("CMI")) {
            CMIUser user = CMI.getInstance().getPlayerManager().getUser(player);
            return user.isAfk();
        }
        return afkPlayers.contains(player.getUniqueId());
    }

    public boolean isAfkCheck() {
        return afkCheck;
    }


    private static class CMIListener implements Listener {

        @EventHandler
        public void onAFKEnter(CMIAfkEnterEvent event) {
            event.getPlayer().sendMessage(ConfigManager.getMessage("messages.afk"));
        }

        @EventHandler
        public void onAFKLeave(CMIAfkLeaveEvent event) {
            String message = ConfigManager.getMessage("messages.no_longer_afk");
            if (!message.equals("")) {
                event.getPlayer().sendMessage(message);
            }
        }
    }
}
