package com.github.xiaobaphage.xbp.core;

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

/**
 * 功能管理器。
 * 负责 Feature 的注册、启用、禁用、重载。
 *
 * 运行时状态和配置状态分离：
 * - Feature.isEnabled() → config.yml 中的 enabled 标志（"是否应该启用"）
 * - FeatureManager 内部跟踪实际运行状态（"是否正在运行"）
 */
public class FeatureManager {

    private final JavaPlugin plugin;
    private final Map<String, Feature> features = new LinkedHashMap<>();
    /** 实际正在运行的功能 */
    private final Set<String> activeFeatures = new HashSet<>();

    public FeatureManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 注册功能模块 */
    public void register(Feature feature) {
        features.put(feature.getName(), feature);
    }

    /** 指定功能是否正在运行 */
    public boolean isActive(String name) {
        return activeFeatures.contains(name);
    }

    /** 启用指定功能 */
    public boolean enableFeature(String name) {
        Feature f = features.get(name);
        if (f == null) return false;
        if (activeFeatures.contains(name)) return true; // 已在运行

        try {
            f.onEnable(plugin);
            activeFeatures.add(name);
            plugin.getLogger().info("功能 [" + name + "] 已启用");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "启用功能 [" + name + "] 失败", e);
            return false;
        }
    }

    /** 禁用指定功能 */
    public boolean disableFeature(String name) {
        Feature f = features.get(name);
        if (f == null) return false;
        if (!activeFeatures.contains(name)) return true; // 未在运行

        try {
            f.onDisable(plugin);
            activeFeatures.remove(name);
            plugin.getLogger().info("功能 [" + name + "] 已禁用");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "禁用功能 [" + name + "] 失败", e);
            return false;
        }
    }

    /** 获取功能实例 */
    public Feature getFeature(String name) {
        return features.get(name);
    }

    /** 获取所有功能（不可修改视图） */
    public Map<String, Feature> getAllFeatures() {
        return Collections.unmodifiableMap(features);
    }

    /**
     * 按 config.yml 的 features 段同步开关状态。
     * 读取 features.<name>.enabled，启用应开的、关闭应关的。
     */
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

    /**
     * 全量重载。
     * 先全部 disable → 重新 loadConfig → 按新配置 sync。
     */
    public void reloadAll() {
        // 1. 全部禁用
        for (String name : Set.copyOf(activeFeatures)) {
            disableFeature(name);
        }

        // 2. 重新加载配置
        plugin.reloadConfig();
        for (Feature f : features.values()) {
            f.loadConfig(plugin.getConfig());
        }

        // 3. 按新配置启用
        syncFromConfig();
    }

    /** 服务器关闭时清理所有功能 */
    public void disableAll() {
        for (String name : Set.copyOf(activeFeatures)) {
            disableFeature(name);
        }
    }

    /** 工具方法：注销 Listener（遍历所有 HandlerList 实例方法） */
    public static void unregisterListener(Listener listener) {
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            handlerList.unregister(listener);
        }
    }

    /** 工具方法：取消所有已安排的定时任务（按插件过滤） */
    public static void cancelAllTasks(JavaPlugin plugin) {
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
