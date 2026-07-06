package com.github.xbphage.minekit.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class ConfigUpgrader {

    private static final int CURRENT_VERSION = 2;

    public static void upgrade(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }

        FileConfiguration diskConfig = YamlConfiguration.loadConfiguration(configFile);
        int version = diskConfig.getInt("config-version", 1);

        FileConfiguration defaultConfig;
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) return;
            defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(in));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[配置] 读取默认配置失败", e);
            return;
        }

        boolean changed = false;

        // 版本升级
        if (version < CURRENT_VERSION) {
            plugin.getLogger().info("[配置] 当前版本 v" + version + "，升级至 v" + CURRENT_VERSION);
            if (version < 2) {
                addMissingKey(diskConfig, defaultConfig, "features.report");
                addMissingSection(diskConfig, defaultConfig, "report");
            }
            diskConfig.set("config-version", CURRENT_VERSION);
            changed = true;
        }

        // 始终补充缺失的新功能配置（不依赖版本号）
        ConfigurationSection defaultFeatures = defaultConfig.getConfigurationSection("features");
        if (defaultFeatures != null) {
            for (String key : defaultFeatures.getKeys(false)) {
                String path = "features." + key;
                if (!diskConfig.contains(path)) {
                    diskConfig.set(path, defaultConfig.get(path));
                    changed = true;
                    plugin.getLogger().info("[配置] 新增功能开关: " + key);
                }
                if (defaultConfig.contains(key)) {
                    addMissingSection(diskConfig, defaultConfig, key);
                }
            }
        }

        if (changed) {
            try {
                diskConfig.save(configFile);
                plugin.getLogger().info("[配置] 已更新");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "[配置] 保存配置失败", e);
            }
        }
    }

    private static void addMissingKey(FileConfiguration disk, FileConfiguration defaults, String path) {
        if (!disk.contains(path) && defaults.contains(path)) {
            disk.set(path, defaults.get(path));
        }
    }

    private static void addMissingSection(FileConfiguration disk, FileConfiguration defaults, String path) {
        if (!defaults.contains(path)) return;
        if (!disk.contains(path)) {
            disk.set(path, defaults.get(path));
            return;
        }
        ConfigurationSection defaultSection = defaults.getConfigurationSection(path);
        ConfigurationSection diskSection = disk.getConfigurationSection(path);
        if (defaultSection != null && diskSection != null) {
            mergeSection(diskSection, defaultSection);
        }
    }

    private static void mergeSection(ConfigurationSection disk, ConfigurationSection defaults) {
        for (String key : defaults.getKeys(false)) {
            if (disk.contains(key)) {
                ConfigurationSection subDisk = disk.getConfigurationSection(key);
                ConfigurationSection subDefault = defaults.getConfigurationSection(key);
                if (subDisk != null && subDefault != null) {
                    mergeSection(subDisk, subDefault);
                }
            } else {
                Object val = defaults.get(key);
                if (val != null) disk.set(key, val);
            }
        }
    }
}
