package com.github.xiaobaphage.xbp.features.killstats;

import com.github.xiaobaphage.xbp.core.Feature;
import com.github.xiaobaphage.xbp.core.FeatureManager;
import com.github.xiaobaphage.xbp.core.SubCommand;
import com.github.xiaobaphage.xbp.features.killstats.command.KillStatsCommand;
import com.github.xiaobaphage.xbp.features.killstats.database.KillStatsDatabase;
import com.github.xiaobaphage.xbp.features.killstats.listener.KillStatsListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KillStatsFeature implements Feature {

    private static final String NAME = "killstats";
    private static final String DESCRIPTION = "击杀统计 — 记录击杀/死亡/排行";

    private boolean enabled;
    private KillStatsDatabase db;
    private KillStatsListener listener;
    private KillStatsCommand command;

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
        this.db = new KillStatsDatabase(plugin.getDataFolder(), plugin.getLogger());

        this.listener = new KillStatsListener(db);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        this.command = new KillStatsCommand(db);
        plugin.getLogger().info("[KillStats] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }
        if (db != null) {
            db.close();
            db = null;
        }
        plugin.getLogger().info("[KillStats] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        if (command != null) {
            return Collections.singletonList(command);
        }
        return Collections.emptyList();
    }
}
