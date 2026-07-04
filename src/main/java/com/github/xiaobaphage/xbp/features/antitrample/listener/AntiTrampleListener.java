package com.github.xiaobaphage.xbp.features.antitrample.listener;

import com.github.xiaobaphage.xbp.features.antitrample.config.AntiTrampleConfig;
import com.github.xiaobaphage.xbp.features.antitrample.config.AntiTrampleGroupConfig;
import com.github.xiaobaphage.xbp.features.antitrample.config.AntiTrampleGroupConfig.AntiTrampleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

/**
 * 农田防踩踏监听器。
 * 阻止耕地退化，对踩踏者按权限组扣血 + 施加效果。
 */
public class AntiTrampleListener implements Listener {

    private final AntiTrampleConfig config;

    public AntiTrampleListener(AntiTrampleConfig config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() != Material.FARMLAND) return;

        // 阻止耕地退化
        event.setCancelled(true);

        // 查找站在此耕地上的玩家
        Block block = event.getBlock();
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);
        Collection<Player> players = loc.getWorld().getNearbyPlayers(loc, 1.5, 2.0, 1.5);

        for (Player player : players) {
            // 检查玩家是否正好站在耕地上方（含跳跃/下落时）
            Block feetBlock = player.getLocation().getBlock();
            Block belowBlock = feetBlock.getRelative(0, -1, 0);
            if (!feetBlock.equals(block) && !belowBlock.equals(block)) continue;

            punishPlayer(player);
            break; // 只惩罚站得最近的一个
        }
    }

    private void punishPlayer(Player player) {
        AntiTrampleGroupConfig group = config.getGroupForPlayer(player);
        if (group == null) return;

        // 扣血
        if (group.getDamage() > 0) {
            double newHealth = Math.max(0.5, player.getHealth() - group.getDamage());
            player.setHealth(newHealth);
        }

        // 施加效果
        for (AntiTrampleEffect ef : group.getEffects()) {
            PotionEffectType type = PotionEffectType.getByName(ef.getType());
            if (type == null) continue;
            player.addPotionEffect(
                    new PotionEffect(type, ef.getDurationTicks(), ef.getAmplifier()),
                    true
            );
        }
    }
}
