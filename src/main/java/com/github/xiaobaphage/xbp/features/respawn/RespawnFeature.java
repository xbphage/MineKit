package com.github.xiaobaphage.xbp.features.respawn;

import com.github.xiaobaphage.xbp.core.Feature;
import com.github.xiaobaphage.xbp.core.FeatureManager;
import com.github.xiaobaphage.xbp.core.SubCommand;
import com.github.xiaobaphage.xbp.core.command.XbpCommand;
import com.github.xiaobaphage.xbp.features.respawn.config.RespawnConfig;
import com.github.xiaobaphage.xbp.features.respawn.listener.RespawnListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

/**
 * 复活属性给予功能模块。
 *
 * 玩家死亡复活后，根据权限自动设置血量、饱食度、药水效果。
 */
public class RespawnFeature implements Feature {

    private static final String NAME = "respawn";
    private static final String DESCRIPTION = "复活属性给予 — 死亡后设置血量、饱食度、效果";

    private boolean enabled;
    private RespawnConfig respawnConfig;
    private RespawnListener listener;
    private XbpCommand xbpCommand;

    @Override
    public String getName() { return NAME; }

    @Override
    public String getDescription() { return DESCRIPTION; }

    @Override
    public boolean isEnabled() { return enabled; }

    /**
     * loadConfig 仅读取开关状态。
     * 完整配置（含日志输出）在 onEnable 时加载。
     */
    @Override
    public void loadConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("features." + NAME + ".启用", false);
        // 不在 loadConfig 中加载全量配置，避免 Logger 尚未就绪
    }

    @Override
    public void onEnable(JavaPlugin plugin) {
        // 加载全量配置
        this.respawnConfig = new RespawnConfig(plugin.getLogger());
        this.respawnConfig.load(plugin.getConfig().getConfigurationSection(NAME));

        // 注册监听器
        this.listener = new RespawnListener(plugin, respawnConfig);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        // 注册子命令
        if (xbpCommand != null) {
            xbpCommand.registerFeatureSubCommands(getSubCommands());
        }

        plugin.getLogger().info("[Respawn] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        // 注销监听器
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }

        // 移除子命令
        if (xbpCommand != null) {
            xbpCommand.unregisterFeatureSubCommands(getSubCommands());
        }

        respawnConfig = null;
        plugin.getLogger().info("[Respawn] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return Collections.emptyList();
    }

    /** 注入 XbpCommand 引用，用于注册/注销子命令 */
    public void setXbpCommand(XbpCommand xbpCommand) {
        this.xbpCommand = xbpCommand;
    }
}
