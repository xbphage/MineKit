package com.github.xiaobaphage.xbp.features.respawn.listener;

import com.github.xiaobaphage.xbp.features.respawn.config.EffectData;
import com.github.xiaobaphage.xbp.features.respawn.config.GroupConfig;
import com.github.xiaobaphage.xbp.features.respawn.config.RespawnConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.logging.Logger;

/**
 * 监听玩家复活事件，应用配置的血量、饱食度、药水效果。
 */
public class RespawnListener implements Listener {

    private final JavaPlugin plugin;
    private final RespawnConfig config;
    private final Logger logger;

    public RespawnListener(JavaPlugin plugin, RespawnConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // 延迟 1 tick，确保玩家完全重生
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            GroupConfig group = config.getGroupForPlayer(player);
            if (group == null) return;

            // 1. 设置血量（不超过 MaxHealth）
            player.setHealth(Math.min(group.getHealth(), player.getMaxHealth()));

            // 2. 设置饱食度
            player.setFoodLevel(group.getFood());

            // 3. 施加药水效果（不清除已有效果，同类型覆盖）
            for (EffectData ef : group.getEffects()) {
                PotionEffectType type = PotionEffectType.getByName(ef.getType());
                if (type == null) {
                    logger.warning("[Respawn] 未知效果类型: " + ef.getType());
                    continue;
                }
                player.addPotionEffect(
                        new PotionEffect(type, ef.getDurationTicks(), ef.getAmplifier()),
                        true // 强制覆盖已有效果
                );
            }
        }, 1L);
    }
}
