package com.github.xbphage.minekit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

/**
 * 配置升级器。
 * 检测 config.yml 版本，增量添加缺失配置，绝不覆盖已有值。
 */
public class ConfigUpgrader {

    private static final int CURRENT_VERSION = 2;

    public static void upgrade(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }

        // 读取当前硬盘上的配置
        FileConfiguration diskConfig = YamlConfiguration.loadConfiguration(configFile);
        int version = diskConfig.getInt("config-version", 1);

        if (version >= CURRENT_VERSION) {
            return;
        }

        plugin.getLogger().info("[配置] 当前版本 v" + version + "，升级至 v" + CURRENT_VERSION);

        // 加载内嵌的默认配置
        FileConfiguration defaultConfig;
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) {
                plugin.getLogger().warning("[配置] 无法读取内嵌默认配置");
                return;
            }
            defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[配置] 读取默认配置失败", e);
            return;
        }

        // 逐版本升级
        if (version < 2) {
            addMissingFeatures(diskConfig, defaultConfig, "features.report");
            addMissingFeatureSection(diskConfig, defaultConfig, "report");
        }
        // 后续版本： if (version < 3) ...

        // 写版本号
        diskConfig.set("config-version", CURRENT_VERSION);
        try {
            diskConfig.save(configFile);
            plugin.getLogger().info("[配置] 升级完成，当前版本 v" + CURRENT_VERSION);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[配置] 保存升级后配置失败", e);
        }
    }

    /** 添加缺失的功能开关，如 features.report */
    private static void addMissingFeatures(FileConfiguration disk, FileConfiguration defaults, String path) {
        if (disk.contains(path)) return;
        Object val = defaults.get(path);
        if (val != null) {
            disk.set(path, val);
        }
    }

    /**
     * 添加缺失的功能配置段（如 report 整段）。
     * 递归合并：如果顶级不存在则整段复制；如果存在则递归补缺失的子 key。
     */
    private static void addMissingFeatureSection(FileConfiguration disk, FileConfiguration defaults, String path) {
        if (!defaults.contains(path)) return;

        if (!disk.contains(path)) {
            // 完全不存才，整段复制
            disk.set(path, defaults.get(path));
            return;
        }

        // 已存在，递归补缺失的子 key
        ConfigurationSection defaultSection = defaults.getConfigurationSection(path);
        ConfigurationSection diskSection = disk.getConfigurationSection(path);
        if (defaultSection != null && diskSection != null) {
            mergeSection(diskSection, defaultSection);
        }
    }

    /** 递归合并两个 ConfigurationSection：diskSection 缺失的 key 从 defaultSection 补充 */
    private static void mergeSection(ConfigurationSection disk, ConfigurationSection defaults) {
        for (String key : defaults.getKeys(false)) {
            String fullPath = disk.getCurrentPath() + "." + key;
            if (disk.contains(key)) {
                // 如果双方都是子 section，递归合并
                ConfigurationSection subDisk = disk.getConfigurationSection(key);
                ConfigurationSection subDefault = defaults.getConfigurationSection(key);
                if (subDisk != null && subDefault != null) {
                    mergeSection(subDisk, subDefault);
                }
                // 否则已有值，不覆盖
            } else {
                // disk 缺少此 key，从 defaults 补充
                Object val = defaults.get(key);
                if (val != null) {
                    disk.set(key, val);
                }
            }
        }
    }
}
