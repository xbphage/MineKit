package com.github.xiaobaphage.xbp.features.pvp;

import com.github.xiaobaphage.xbp.core.Feature;
import com.github.xiaobaphage.xbp.core.FeatureManager;
import com.github.xiaobaphage.xbp.core.SubCommand;
import com.github.xiaobaphage.xbp.features.pvp.config.PvpConfig;
import com.github.xiaobaphage.xbp.features.pvp.listener.PvpListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

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
        this.enabled = config.getBoolean("features." + NAME + ".enabled", false);
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
}
