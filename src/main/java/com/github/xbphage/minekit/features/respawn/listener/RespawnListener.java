package com.github.xbphage.minekit.features.respawn.listener;

import com.github.xbphage.minekit.features.respawn.config.EffectData;
import com.github.xbphage.minekit.features.respawn.config.RespawnConfig;
import com.github.xbphage.minekit.features.respawn.config.RespawnConfig.PenaltyResult;
import com.github.xbphage.minekit.records.RecordManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.logging.Logger;

public class RespawnListener implements Listener {

    private final JavaPlugin plugin;
    private final RespawnConfig config;
    private final RecordManager records;
    private final Logger logger;

    public RespawnListener(JavaPlugin plugin, RespawnConfig config, RecordManager records) {
        this.plugin = plugin;
        this.config = config;
        this.records = records;
        this.logger = plugin.getLogger();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            String uuid = player.getUniqueId().toString();
            int deathsHour = records.getDeathsLastHour(uuid);
            int redemptionAmount = records.getRedemptionAmount(uuid);
            int effective = Math.max(0, deathsHour - redemptionAmount);

            PenaltyResult penalty = config.getPenalty(player, effective);

            // 设置血量
            double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            player.setHealth(Math.min(penalty.getHealth(), maxHealth));

            // 设置饱食度
            player.setFoodLevel(penalty.getFood());

            // 施加效果
            for (EffectData ef : penalty.getEffects()) {
                PotionEffectType type = PotionEffectType.getByName(ef.getType());
                if (type == null) {
                    logger.warning("[Respawn] 未知效果: " + ef.getType());
                    continue;
                }
                player.removePotionEffect(type);
                player.addPotionEffect(new PotionEffect(type, ef.getDurationTicks(), ef.getAmplifier()));
            }

            // 黄色消息（在死亡回溯提示之后显示）
            if (penalty.getMessage() != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage(color("&e" + penalty.getMessage()));
                    }
                }, 2L);
            }
        }, 1L);
    }

    private static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
