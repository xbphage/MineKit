package com.github.xbphage.minekit.features.report;

import com.github.xbphage.minekit.core.Feature;
import com.github.xbphage.minekit.core.FeatureManager;
import com.github.xbphage.minekit.core.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReportFeature implements Feature {

    private static final String NAME = "report";
    private static final String DESCRIPTION = "命令通报 — 非管理员执行命令时通报给管理员";
    private static final String PERM_ADMIN = "minekit.admin";

    private boolean enabled;
    private List<String> ignoredCommands;
    private ReportListener listener;

    @Override
    public String getName() { return NAME; }
    @Override
    public String getDescription() { return DESCRIPTION; }
    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void loadConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("features." + NAME + ".启用", false);
        this.ignoredCommands = config.getStringList(NAME + ".忽略命令");
        if (ignoredCommands == null) ignoredCommands = new ArrayList<>();
    }

    @Override
    public void onEnable(JavaPlugin plugin) {
        this.listener = new ReportListener();
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getLogger().info("[Report] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }
        plugin.getLogger().info("[Report] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return Collections.emptyList();
    }

    private class ReportListener implements Listener {

        @EventHandler
        public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
            Player player = event.getPlayer();
            if (player.isOp() || player.hasPermission(PERM_ADMIN)) return;

            String command = event.getMessage();
            for (String ignored : ignoredCommands) {
                if (command.startsWith(ignored)) return;
            }

            // 管理员在线提示（单行）
            String msg = color("&8[&e!&8] &7" + player.getName() + " &f执行: &e" + command);
            for (Player admin : Bukkit.getOnlinePlayers()) {
                if (admin.isOp() || admin.hasPermission(PERM_ADMIN)) {
                    admin.sendMessage(msg);
                }
            }
            // 控制台醒目输出（单行）
            Bukkit.getConsoleSender().sendMessage(color("&8[&e!&8] &c" + player.getName() + " &f执行命令: &c" + command));
        }
    }

    static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
