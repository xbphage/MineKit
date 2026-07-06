package com.github.xbphage.minekit.features.killstats.listener;

import com.github.xbphage.minekit.features.killstats.database.KillStatsDatabase;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 监听玩家击杀事件，记录到数据库。
 * 使用 damager 缓存处理击杀归属（避免 event.getEntity().getKiller() 不准的问题）。
 */
public class KillStatsListener implements Listener {

    private final KillStatsDatabase db;
    private final Map<UUID, UUID> lastDamager = new HashMap<>();

    public KillStatsListener(KillStatsDatabase db) {
        this.db = db;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) return;
        lastDamager.put(event.getEntity().getUniqueId(), event.getDamager().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimUuid = victim.getUniqueId();
        UUID killerUuid = lastDamager.remove(victimUuid);

        if (killerUuid == null) return;
        Player killer = victim.getServer().getPlayer(killerUuid);
        if (killer == null || !killer.isOnline()) return;

        db.recordKill(killerUuid, killer.getName(), victimUuid, victim.getName());
    }
}
