package com.github.xiaobaphage.xbp.features.deathback.listener;

import com.github.xiaobaphage.xbp.features.deathback.DeathBackFeature;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathBackListener implements Listener {

    private final DeathBackFeature feature;
    private final Map<UUID, Location> deathLocations = new HashMap<>();

    public DeathBackListener(DeathBackFeature feature) {
        this.feature = feature;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        deathLocations.put(player.getUniqueId(), player.getLocation());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location deathLoc = deathLocations.get(player.getUniqueId());
        if (deathLoc == null) return;

        feature.sendBackPrompt(player, deathLoc);
    }

    public Location getDeathLocation(UUID uuid) {
        return deathLocations.remove(uuid);
    }
}
