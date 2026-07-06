package com.github.xbphage.minekit.core;

import com.github.xbphage.minekit.core.command.MineKitCommand;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class FeatureManager {

    private final JavaPlugin plugin;
    private final Map<String, Feature> features = new LinkedHashMap<>();
    private final Set<String> activeFeatures = new HashSet<>();
    private MineKitCommand xbpCommand;

    public FeatureManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setMineKitCommand(MineKitCommand cmd) {
        this.xbpCommand = cmd;
    }

    public void register(Feature feature) {
        features.put(feature.getName(), feature);
    }

    public boolean isActive(String name) {
        return activeFeatures.contains(name);
    }

    public boolean enableFeature(String name) {
        Feature f = features.get(name);
        if (f == null) return false;
        if (activeFeatures.contains(name)) return true;

        try {
            f.onEnable(plugin);
            activeFeatures.add(name);
            if (xbpCommand != null) {
                xbpCommand.registerFeatureSubCommands(f.getSubCommands());
            }
            plugin.getLogger().info("功能 [" + name + "] 已启用");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "启用功能 [" + name + "] 失败", e);
            return false;
        }
    }

    public boolean disableFeature(String name) {
        Feature f = features.get(name);
        if (f == null) return false;
        if (!activeFeatures.contains(name)) return true;

        try {
            if (xbpCommand != null) {
                xbpCommand.unregisterFeatureSubCommands(f.getSubCommands());
            }
            f.onDisable(plugin);
            activeFeatures.remove(name);
            plugin.getLogger().info("功能 [" + name + "] 已禁用");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "禁用功能 [" + name + "] 失败", e);
            return false;
        }
    }

    public Feature getFeature(String name) {
        return features.get(name);
    }

    public Map<String, Feature> getAllFeatures() {
        return Collections.unmodifiableMap(features);
    }

    public void syncFromConfig() {
        for (Feature f : features.values()) {
            boolean shouldEnable = f.isEnabled();
            if (shouldEnable && !activeFeatures.contains(f.getName())) {
                enableFeature(f.getName());
            } else if (!shouldEnable && activeFeatures.contains(f.getName())) {
                disableFeature(f.getName());
            }
        }
    }

    public void reloadAll() {
        for (String name : new HashSet<>(activeFeatures)) {
            disableFeature(name);
        }
        plugin.reloadConfig();
        for (Feature f : features.values()) {
            f.loadConfig(plugin.getConfig());
        }
        syncFromConfig();
    }

    public void disableAll() {
        for (String name : new HashSet<>(activeFeatures)) {
            disableFeature(name);
        }
    }

    public static void unregisterListener(Listener listener) {
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            handlerList.unregister(listener);
        }
    }

    public static void cancelAllTasks(JavaPlugin plugin) {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
