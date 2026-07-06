package com.github.xbphage.minekit.debug;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class DebugUtil {

    private static boolean enabled = false;
    private static Logger logger;

    public static void init(JavaPlugin plugin) {
        logger = plugin.getLogger();
        enabled = plugin.getConfig().getBoolean("debug", false);
        if (enabled) plugin.getLogger().info("[调试模式] 已启用");
    }

    public static void reload(FileConfiguration config) {
        enabled = config.getBoolean("debug", false);
    }

    public static void log(String feature, String message) {
        if (enabled) {
            logger.info("[调试-" + feature + "] " + message);
        }
    }

    public static boolean isEnabled() { return enabled; }
}
