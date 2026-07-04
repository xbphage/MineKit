package com.github.xiaobaphage.xbp.features.antitrample;

import com.github.xiaobaphage.xbp.core.Feature;
import com.github.xiaobaphage.xbp.core.FeatureManager;
import com.github.xiaobaphage.xbp.core.SubCommand;
import com.github.xiaobaphage.xbp.features.antitrample.config.AntiTrampleConfig;
import com.github.xiaobaphage.xbp.features.antitrample.config.AntiTrampleGroupConfig;
import com.github.xiaobaphage.xbp.features.antitrample.listener.AntiTrampleListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 农田防踩踏功能模块。
 * 阻止耕地退化，对踩踏者按权限组扣血 + 施加效果。
 */
public class AntiTrampleFeature implements Feature {

    private static final String NAME = "antitrample";
    private static final String DESCRIPTION = "农田防踩踏 — 踩踏者受罚";

    private boolean enabled;
    private AntiTrampleConfig antiTrampleConfig;
    private AntiTrampleListener listener;

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
        this.antiTrampleConfig = new AntiTrampleConfig(plugin.getLogger());
        this.antiTrampleConfig.load(plugin.getConfig().getConfigurationSection(NAME));

        this.listener = new AntiTrampleListener(antiTrampleConfig);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getLogger().info("[AntiTrample] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }
        antiTrampleConfig = null;
        plugin.getLogger().info("[AntiTrample] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getConfigDetail(JavaPlugin plugin) {
        List<String> lines = new ArrayList<>();
        lines.add("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        lines.add("&6  &l农田防踩踏 &7- 配置详情");
        lines.add("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");

        if (antiTrampleConfig == null) {
            lines.add("&7  功能未启用，无配置数据");
            return lines;
        }

        boolean enableOp = antiTrampleConfig.isEnableOpGroup();
        lines.add("&6\n  &l独立 OP 组: " + (enableOp ? "&a开启" : "&c关闭"));
        if (enableOp) {
            lines.addAll(formatDetail(antiTrampleConfig.getOpGroup(), "  "));
        }

        lines.add("&6\n  &l权限组");
        for (AntiTrampleGroupConfig g : antiTrampleConfig.getGroups()) {
            lines.addAll(formatDetail(g, "  "));
        }

        lines.add("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        return lines;
    }

    private List<String> formatDetail(AntiTrampleGroupConfig g, String indent) {
        List<String> lines = new ArrayList<>();
        String perm = g.getPermission();
        lines.add(indent + "&8┌ &f" + g.getName() + (perm != null ? " &7(" + perm + ")" : " &7(无权限)"));
        lines.add(indent + "&8├ &7扣血: &f" + String.format("%.0f", g.getDamage()));
        if (g.getEffects().isEmpty()) {
            lines.add(indent + "&8└ &7效果: &8无");
        } else {
            lines.add(indent + "&8├ &7效果:");
            for (AntiTrampleGroupConfig.AntiTrampleEffect ef : g.getEffects()) {
                int secs = ef.getDurationTicks() / 20;
                lines.add(indent + "&8│ &f" + ef.getType() + " &7" + secs + "s &7Lv" + (ef.getAmplifier() + 1));
            }
            lines.add(indent + "&8└ &7共 " + g.getEffects().size() + " 个效果");
        }
        return lines;
    }
}
