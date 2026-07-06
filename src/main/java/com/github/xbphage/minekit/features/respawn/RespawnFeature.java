package com.github.xbphage.minekit.features.respawn;

import com.github.xbphage.minekit.core.Feature;
import com.github.xbphage.minekit.core.FeatureManager;
import com.github.xbphage.minekit.core.SubCommand;
import com.github.xbphage.minekit.core.command.MineKitCommand;
import com.github.xbphage.minekit.features.respawn.config.RespawnConfig;
import com.github.xbphage.minekit.features.respawn.listener.RespawnListener;
import com.github.xbphage.minekit.records.RecordManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RespawnFeature implements Feature {

    private static final String NAME = "respawn";
    private static final String DESCRIPTION = "重生频率惩罚 — 按死亡次数递减属性";

    private boolean enabled;
    private RespawnConfig respawnConfig;
    private RespawnListener listener;
    private MineKitCommand xbpCommand;

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
        this.respawnConfig = new RespawnConfig(plugin.getLogger());
        this.respawnConfig.load(plugin.getConfig().getConfigurationSection(NAME));

        RecordManager records = RecordManager.get();
        this.listener = new RespawnListener(plugin, respawnConfig, records);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        if (xbpCommand != null) {
            xbpCommand.registerFeatureSubCommands(getSubCommands());
        }
        plugin.getLogger().info("[Respawn] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }
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
        lines.add("&6  &l重生频率惩罚");
        lines.add("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        lines.add("&7  频率惩罚基数: " + (respawnConfig != null ? "&a已配置" : "&c未加载"));
        lines.add("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        return lines;
    }

    public void setMineKitCommand(MineKitCommand xbpCommand) {
        this.xbpCommand = xbpCommand;
    }
}
