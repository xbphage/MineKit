package com.github.xbphage.minekit.features.pvp.listener;

import com.github.xbphage.minekit.features.pvp.config.PvpConfig;
import com.github.xbphage.minekit.features.pvp.config.PvpGroupConfig;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * PvP 监听器。
 * 检测玩家攻击玩家，根据双方权限执行自定义命令。
 * 自带去重机制，避免同一攻击触发多次。
 */
@SuppressWarnings("deprecation")
public class PvpListener implements Listener {

    private static final long DEBOUNCE_MS = 200L;
    private static final String PAPI_NAME = "PlaceholderAPI";

    private final JavaPlugin plugin;
    private final PvpConfig config;
    private final Logger logger;
    private final Map<String, Long> lastHit = new HashMap<>();
    private boolean hasPapi;

    public PvpListener(JavaPlugin plugin, PvpConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.hasPapi = Bukkit.getPluginManager().getPlugin(PAPI_NAME) != null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // 去重：同一攻击者在 200ms 内对同一目标的多次触发只处理一次
        String key = attacker.getUniqueId() + ">" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastHit.get(key);
        if (last != null && now - last < DEBOUNCE_MS) {
            return;
        }
        lastHit.put(key, now);

        if (config.isCancelVanillaDamage()) {
            event.setCancelled(true);
        }

        double damage = config.isCancelVanillaDamage() ? event.getFinalDamage() : event.getDamage();
        String weaponName = getWeaponName(attacker);

        PvpGroupConfig attackerGroup = config.getGroupForPlayer(attacker);
        PvpGroupConfig victimGroup = config.getGroupForPlayer(victim);

        executeCommands(attackerGroup.getAttackerCommands(), attacker, victim, damage, weaponName, attacker);
        executeCommands(victimGroup.getVictimCommands(), attacker, victim, damage, weaponName, victim);
    }

    private void executeCommands(java.util.List<String> commands,
                                  Player attacker, Player victim,
                                  double damage, String weapon,
                                  Player context) {
        for (String cmd : commands) {
            if (cmd.isEmpty()) continue;

            String processed = cmd
                    .replace("%pvp_attacker%", attacker.getName())
                    .replace("%pvp_victim%", victim.getName())
                    .replace("%pvp_damage%", String.format("%.1f", damage))
                    .replace("%pvp_weapon%", weapon);

            if (hasPapi) {
                processed = PlaceholderAPI.setPlaceholders(context, processed);
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
        }
    }

    @SuppressWarnings("deprecation")
    private String getWeaponName(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            return "空手";
        }
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name();
    }
}
