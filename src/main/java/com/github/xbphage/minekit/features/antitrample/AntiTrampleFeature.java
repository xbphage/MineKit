package com.github.xbphage.minekit.features.antitrample;

import com.github.xbphage.minekit.core.Feature;
import com.github.xbphage.minekit.core.FeatureManager;
import com.github.xbphage.minekit.core.SubCommand;
import com.github.xbphage.minekit.features.antitrample.listener.AntiTrampleListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class AntiTrampleFeature implements Feature {

    private static final String NAME = "antitrample";
    private static final String DESCRIPTION = "农田防踩踏 — 阻止耕地退化";

    private boolean enabled;
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
        this.listener = new AntiTrampleListener();
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getLogger().info("[AntiTrample] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }
        plugin.getLogger().info("[AntiTrample] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return Collections.emptyList();
    }
}
