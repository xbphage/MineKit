package com.github.xbphage.minekit.config;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * 配置校验器。
 * 检测 config.yml 中的常见问题，自动备份并修复。
 */
public class ConfigValidator {

    private static final String PREFIX = "&8[&e⚠&8] &c[配置错误]";

    public static boolean validate(JavaPlugin plugin) {
        Logger log = plugin.getLogger();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) return true;

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        boolean hasIssue = false;

        // 1. 检查 features 段
        ConfigurationSection features = config.getConfigurationSection("features");
        if (features == null) {
            log.severe("[配置] features 段缺失！服务端将使用默认配置");
            return false;
        }

        // 2. 检查每个功能的启用状态
        for (String key : features.getKeys(false)) {
            if (!features.isBoolean(key + ".启用")) {
                log.warning("[配置] features." + key + ".启用 类型错误或缺失，已重置为 false");
                features.set(key + ".启用", false);
                hasIssue = true;
            }
        }

        // 3. 检查效果格式（确保是 ["type", sec, lv] 形式）
        hasIssue |= validateEffects(config, "respawn.OP组.效果", log);
        hasIssue |= validateEffects(config, "respawn.权限组.default.效果", log);
        hasIssue |= validateEffects(config, "respawn.权限组.vip.效果", log);
        hasIssue |= validateEffects(config, "antitrample.OP组.效果", log);
        hasIssue |= validateEffects(config, "antitrample.权限组.default.效果", log);
        hasIssue |= validateEffects(config, "antitrample.权限组.vip.效果", log);

        // 4. 检查血量/饱食度/伤害值是否为数字
        hasIssue |= validateNumber(config, "respawn.OP组.血量", log);
        hasIssue |= validateNumber(config, "respawn.权限组.default.血量", log);
        hasIssue |= validateNumber(config, "respawn.权限组.vip.血量", log);
        hasIssue |= validateNumber(config, "antitrample.权限组.default.伤害", log);

        if (!hasIssue) return true;

        // 有问题的配置 → 备份
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File backup = new File(plugin.getDataFolder(), "config.yml.bak." + timestamp);
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.warning("[配置] 发现配置问题，已备份至 " + backup.getName());

            // 修复后保存
            config.save(configFile);
            log.warning("[配置] 已尝试修复配置问题，请检查 config.yml");
        } catch (IOException e) {
            log.severe("[配置] 备份或保存失败: " + e.getMessage());
        }

        // 警告在线管理员
        String warn = color(PREFIX + " 配置文件中发现格式问题，已自动备份并修复，请检查");
        for (Player admin : Bukkit.getOnlinePlayers()) {
            if (admin.isOp() || admin.hasPermission("minekit.admin")) {
                admin.sendMessage(warn);
            }
        }

        return true;
    }

    /** 验证效果列表：每项必须是 ["类型", 秒数, 等级]，秒数和等级兼容字符串和数字 */
    private static boolean validateEffects(FileConfiguration config, String path, Logger log) {
        if (!config.contains(path)) return false;
        List<?> list = config.getList(path);
        if (list == null || list.isEmpty()) return false;

        boolean fixed = false;
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            if (!(obj instanceof List)) {
                log.warning("[配置] " + path + " 第 " + (i + 1) + " 项格式错误，已移除");
                list.remove(i);
                i--;
                fixed = true;
                continue;
            }
            List<?> entry = (List<?>) obj;
            if (entry.size() < 3 || entry.get(0) == null || !isNumeric(entry.get(1)) || !isNumeric(entry.get(2))) {
                log.warning("[配置] " + path + " 第 " + (i + 1) + " 项字段不足或类型错误，已移除");
                list.remove(i);
                i--;
                fixed = true;
            }
        }
        if (fixed) {
            config.set(path, list);
        }
        return fixed;
    }

    /** 判断 Object 是否能转为有效数字（兼容 Number 和 String） */
    private static boolean isNumeric(Object obj) {
        if (obj instanceof Number) return true;
        if (obj instanceof String) {
            try { Integer.parseInt((String) obj); return true; } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    /** 验证数值类型 */
    private static boolean validateNumber(FileConfiguration config, String path, Logger log) {
        if (!config.contains(path)) return false;
        if (!config.isDouble(path) && !config.isInt(path)) {
            log.warning("[配置] " + path + " 应为数值，当前值: " + config.get(path) + "，已重置为默认值");
            config.set(path, 20.0);
            return true;
        }
        return false;
    }

    private static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
