package com.github.xbphage.minekit.records;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 死亡/击杀记录监听器。
 * 永远开启，记录每一次死亡。
 */
public class RecordListener implements Listener {

    private final RecordManager db;
    private final Map<UUID, UUID> lastDamager = new HashMap<>();

    public RecordListener(RecordManager db) {
        this.db = db;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        lastDamager.put(event.getEntity().getUniqueId(), event.getDamager().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimUuid = victim.getUniqueId();
        UUID killerUuid = lastDamager.remove(victimUuid);

        String killerId = null, killerName = null;
        if (killerUuid != null) {
            Player killer = victim.getServer().getPlayer(killerUuid);
            if (killer != null && killer.isOnline()) {
                killerId = killerUuid.toString();
                killerName = killer.getName();
            }
        }

        db.recordDeath(
                victimUuid.toString(),
                victim.getName(),
                event.getDeathMessage(),
                victim.getLocation(),
                killerId,
                killerName
        );
    }
}
