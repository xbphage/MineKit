package com.github.xiaobaphage.xbp.features.pvp.listener;

import com.github.xiaobaphage.xbp.features.pvp.config.PvpConfig;
import com.github.xiaobaphage.xbp.features.pvp.config.PvpGroupConfig;
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

import java.util.logging.Logger;

/**
 * PvP 监听器。
 * 检测玩家攻击玩家，根据双方权限执行自定义命令。
 */
public class PvpListener implements Listener {

    private static final String PAPI_NAME = "PlaceholderAPI";

    private final JavaPlugin plugin;
    private final PvpConfig config;
    private final Logger logger;
    private boolean hasPapi;

    public PvpListener(JavaPlugin plugin, PvpConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.hasPapi = Bukkit.getPluginManager().getPlugin(PAPI_NAME) != null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        // 只处理玩家→玩家
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // 根据配置决定是否取消原版伤害
        if (config.isCancelVanillaDamage()) {
            event.setCancelled(true);
        }

        // 如果取消了伤害，用 finalDamage；否则用实际造成的伤害
        double damage = config.isCancelVanillaDamage() ? event.getFinalDamage() : event.getDamage();
        String weaponName = getWeaponName(attacker);

        // 获取双方权限组
        PvpGroupConfig attackerGroup = config.getGroupForPlayer(attacker);
        PvpGroupConfig victimGroup = config.getGroupForPlayer(victim);

        // 执行攻击者命令（上下文：攻击者视角）
        executeCommands(attackerGroup.getAttackerCommands(), attacker, victim, damage, weaponName, attacker);

        // 执行被攻击者命令（上下文：被攻击者视角）
        executeCommands(victimGroup.getVictimCommands(), attacker, victim, damage, weaponName, victim);
    }

    /**
     * 执行一组命令。
     *
     * @param commands    命令列表
     * @param attacker    攻击者
     * @param victim      被攻击者
     * @param damage      伤害数值
     * @param weapon      武器名称
     * @param context     PlaceholderAPI 上下文玩家（攻击者命令=攻击者，被攻击者命令=被攻击者）
     */
    private void executeCommands(java.util.List<String> commands,
                                  Player attacker, Player victim,
                                  double damage, String weapon,
                                  Player context) {
        for (String cmd : commands) {
            if (cmd.isEmpty()) continue;

            // 1. 替换自定义占位符
            String processed = cmd
                    .replace("%pvp_attacker%", attacker.getName())
                    .replace("%pvp_victim%", victim.getName())
                    .replace("%pvp_damage%", String.format("%.1f", damage))
                    .replace("%pvp_weapon%", weapon);

            // 2. 通过 PlaceholderAPI 替换标准占位符
            if (hasPapi) {
                processed = PlaceholderAPI.setPlaceholders(context, processed);
            }

            // 3. 控制台执行
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
        }
    }

    /** 获取玩家手中的武器名 */
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
