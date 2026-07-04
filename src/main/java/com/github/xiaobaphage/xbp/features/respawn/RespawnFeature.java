package com.github.xiaobaphage.xbp.features.respawn;

import com.github.xiaobaphage.xbp.core.Feature;
import com.github.xiaobaphage.xbp.core.FeatureManager;
import com.github.xiaobaphage.xbp.core.SubCommand;
import com.github.xiaobaphage.xbp.core.command.XbpCommand;
import com.github.xiaobaphage.xbp.features.respawn.config.RespawnConfig;
import com.github.xiaobaphage.xbp.features.respawn.listener.RespawnListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.xiaobaphage.xbp.features.respawn.config.EffectData;
import com.github.xiaobaphage.xbp.features.respawn.config.GroupConfig;

import java.util.ArrayList;
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

    @Override
    public List<String> getConfigDetail(JavaPlugin plugin) {
        List<String> lines = new ArrayList<>();
        lines.add("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        lines.add("&6  &l复活属性给予 &7- 配置详情");
        lines.add("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");

        if (respawnConfig == null) {
            lines.add("&7  功能未启用，无配置数据");
            return lines;
        }

        // OP 组
        boolean enableOp = respawnConfig.isEnableOpGroup();
        lines.add("&6\n  &l独立 OP 组: " + (enableOp ? "&a开启" : "&c关闭"));
        if (enableOp) {
            lines.addAll(formatGroupDetail(respawnConfig.getOpGroup(), "  "));
        }

        // 普通权限组
        lines.add("&6\n  &l权限组");
        for (GroupConfig g : respawnConfig.getGroups()) {
            lines.addAll(formatGroupDetail(g, "  "));
        }

        lines.add("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        return lines;
    }

    private List<String> formatGroupDetail(GroupConfig g, String indent) {
        List<String> lines = new ArrayList<>();
        String name = g.getName();
        String perm = g.getPermission();
        lines.add(indent + "&8┌ &f" + name + (perm != null ? " &7(" + perm + ")" : " &7(无权限)"));
        lines.add(indent + "&8├ &7血量: &f" + String.format("%.1f", g.getHealth()));
        lines.add(indent + "&8├ &7饱食度: &f" + g.getFood());
        if (g.getEffects().isEmpty()) {
            lines.add(indent + "&8└ &7效果: &8无");
        } else {
            lines.add(indent + "&8├ &7效果:");
            for (EffectData ef : g.getEffects()) {
                lines.add(indent + "&8│ &f" + ef.getType() + " &7" + (ef.getDurationTicks() / 20) + "s &7Lv" + (ef.getAmplifier() + 1));
            }
            lines.add(indent + "&8└ &7共 " + g.getEffects().size() + " 个效果");
        }
        return lines;
    }

    /** 注入 XbpCommand 引用，用于注册/注销子命令 */
    public void setXbpCommand(XbpCommand xbpCommand) {
        this.xbpCommand = xbpCommand;
    }
}
