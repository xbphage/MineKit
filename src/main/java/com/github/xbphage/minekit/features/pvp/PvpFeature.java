package com.github.xbphage.minekit.features.pvp;

import com.github.xbphage.minekit.core.Feature;
import com.github.xbphage.minekit.core.FeatureManager;
import com.github.xbphage.minekit.core.SubCommand;
import com.github.xbphage.minekit.features.pvp.config.PvpConfig;
import com.github.xbphage.minekit.features.pvp.listener.PvpListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.xbphage.minekit.features.pvp.config.PvpGroupConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PvP 功能模块。
 *
 * 检测玩家攻击玩家，根据双方权限执行自定义命令。
 * 命令支持占位符：%pvp_attacker%, %pvp_victim%, %pvp_damage%, %pvp_weapon%
 * 以及所有 PlaceholderAPI 标准占位符。
 */
public class PvpFeature implements Feature {

    private static final String NAME = "pvp";
    private static final String DESCRIPTION = "PvP 检测 — 根据权限执行自定义命令";

    private boolean enabled;
    private PvpConfig pvpConfig;
    private PvpListener listener;

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESCRIPTION; }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void loadConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("features." + NAME + ".启用", false);
    }

    @Override
    public void onEnable(JavaPlugin plugin) {
        this.pvpConfig = new PvpConfig(plugin.getLogger());
        this.pvpConfig.load(plugin.getConfig().getConfigurationSection(NAME));

        this.listener = new PvpListener(plugin, pvpConfig);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        plugin.getLogger().info("[PvP] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }
        pvpConfig = null;
        plugin.getLogger().info("[PvP] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getConfigDetail(JavaPlugin plugin) {
        List<String> lines = new ArrayList<>();
        lines.add("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        lines.add("&6  &lPvP 检测 &7- 配置详情");
        lines.add("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");

        if (pvpConfig == null) {
            lines.add("&7  功能未启用，无配置数据");
            return lines;
        }

        lines.add("&7  " + (pvpConfig.isCancelVanillaDamage() ? "&a✔ 取消原版伤害" : "&c✘ 保留原版伤害"));

        boolean enableOp = pvpConfig.isEnableOpGroup();
        lines.add("&6\n  &l独立 OP 组: " + (enableOp ? "&a开启" : "&c关闭"));
        if (enableOp) {
            lines.addAll(formatPvpGroupDetail(pvpConfig.getOpGroup(), "  "));
        }

        lines.add("&6\n  &l权限组");
        for (PvpGroupConfig g : pvpConfig.getGroups()) {
            lines.addAll(formatPvpGroupDetail(g, "  "));
        }

        lines.add("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        return lines;
    }

    private List<String> formatPvpGroupDetail(PvpGroupConfig g, String indent) {
        List<String> lines = new ArrayList<>();
        String name = g.getName();
        String perm = g.getPermission();
        lines.add(indent + "&8┌ &f" + name + (perm != null ? " &7(" + perm + ")" : " &7(无权限)"));

        List<String> atk = g.getAttackerCommands();
        List<String> vic = g.getVictimCommands();
        lines.add(indent + "&8├ &7攻击者命令" + (atk.isEmpty() ? ": &8无" : ""));
        for (int i = 0; i < atk.size(); i++) {
            String prefix = (i == atk.size() - 1 && vic.isEmpty()) ? "&8└" : "&8│";
            lines.add(indent + " " + prefix + " &f" + atk.get(i));
        }
        lines.add(indent + "&8├ &7被攻击者命令" + (vic.isEmpty() ? ": &8无" : ""));
        for (int i = 0; i < vic.size(); i++) {
            String prefix = (i == vic.size() - 1) ? "&8└" : "&8│";
            lines.add(indent + " " + prefix + " &f" + vic.get(i));
        }
        if (atk.isEmpty() && vic.isEmpty()) {
            lines.add(indent + "&8└ &7无命令");
        } else if (atk.isEmpty()) {
            // last line already handled above
        } else if (!vic.isEmpty()) {
            lines.add(indent + "&8└ &7共 " + (atk.size() + vic.size()) + " 条命令");
        }
        return lines;
    }
}
