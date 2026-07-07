package com.github.xbphage.minekit.features.itemtoexp;

import com.github.xbphage.minekit.core.Feature;
import com.github.xbphage.minekit.core.FeatureManager;
import com.github.xbphage.minekit.core.SubCommand;
import com.github.xbphage.minekit.features.itemtoexp.command.ItemToExpCommand;
import com.github.xbphage.minekit.features.itemtoexp.config.ItemToExpConfig;
import com.github.xbphage.minekit.features.itemtoexp.listener.ItemToExpListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemToExpFeature implements Feature {

    private static final String NAME = "itemtoexp";
    private static final String DESCRIPTION = "物品换经验 + 禁止存放 — 用物品兑换经验等级，可禁止特定物品存入容器";

    private boolean enabled;
    private ItemToExpConfig config;
    private ItemToExpCommand command;
    private ItemToExpListener listener;

    @Override public String getName() { return NAME; }
    @Override public String getDescription() { return DESCRIPTION; }
    @Override public boolean isEnabled() { return enabled; }

    @Override
    public void loadConfig(FileConfiguration cfg) {
        this.enabled = cfg.getBoolean("features." + NAME + ".启用", false);
    }

    @Override
    public void onEnable(JavaPlugin plugin) {
        this.config = new ItemToExpConfig();
        this.config.load(plugin.getConfig().getConfigurationSection(NAME));
        this.command = new ItemToExpCommand(this.config);
        this.listener = new ItemToExpListener(this.config);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getLogger().info("[ItemToExp] 功能已启用"
                + (config.isLockEnabled() ? " (禁止存放已激活)" : ""));
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }
        this.command = null;
        this.config = null;
        plugin.getLogger().info("[ItemToExp] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return command != null ? Collections.singletonList(command) : Collections.emptyList();
    }

    @Override
    public List<String> getConfigDetail(JavaPlugin plugin) {
        List<String> lines = new ArrayList<>();
        lines.add("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        lines.add("&6  &l物品换经验");
        lines.add("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        int count = (config != null) ? config.getRecipes().size() : 0;
        lines.add("&7  交换配方: " + (count > 0 ? "&a" + count + " 个" : "&c无"));
        lines.add("&7  禁止存放: " + (config != null && config.isLockEnabled() ? "&a已开启" : "&c未配置"));
        lines.add("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛");
        return lines;
    }
}
