package com.github.xbphage.minekit.features.killstats;

import com.github.xbphage.minekit.core.Feature;
import com.github.xbphage.minekit.core.SubCommand;
import com.github.xbphage.minekit.features.killstats.command.KillStatsCommand;
import com.github.xbphage.minekit.records.RecordManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class KillStatsFeature implements Feature {

    private static final String NAME = "killstats";
    private static final String DESCRIPTION = "击杀统计 — 击杀/死亡排行及明细";

    private boolean enabled;
    private KillStatsCommand command;

    @Override
    public String getName() { return NAME; }
    @Override
    public String getDescription() { return DESCRIPTION; }
    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void loadConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("features." + NAME + ".启用", true);
    }

    @Override
    public void onEnable(JavaPlugin plugin) {
        RecordManager recordDb = RecordManager.get();
        if (recordDb == null) {
            plugin.getLogger().warning("[KillStats] 记录系统未就绪，功能不可用");
            return;
        }
        this.command = new KillStatsCommand(recordDb);
        plugin.getLogger().info("[KillStats] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        plugin.getLogger().info("[KillStats] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        if (command != null) return Collections.singletonList(command);
        return Collections.emptyList();
    }
}
