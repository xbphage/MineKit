package com.github.xiaobaphage.xbp.features.deathback;

import com.github.xiaobaphage.xbp.core.Feature;
import com.github.xiaobaphage.xbp.core.FeatureManager;
import com.github.xiaobaphage.xbp.core.SubCommand;
import com.github.xiaobaphage.xbp.features.deathback.listener.DeathBackListener;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DeathBackFeature implements Feature {

    private static final String NAME = "deathback";
    private static final String DESCRIPTION = "死亡回溯 — 死亡后点击回到死亡点";

    private boolean enabled;
    private DeathBackListener listener;

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
        this.listener = new DeathBackListener(this);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        plugin.getLogger().info("[DeathBack] 功能已启用");
    }

    @Override
    public void onDisable(JavaPlugin plugin) {
        if (listener != null) {
            FeatureManager.unregisterListener(listener);
            listener = null;
        }
        plugin.getLogger().info("[DeathBack] 功能已禁用");
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return Collections.singletonList(new SubCommand() {
            @Override
            public String getName() { return "back"; }

            @Override
            public String getPermission() { return null; }

            @Override
            public boolean execute(org.bukkit.command.CommandSender sender, String[] args) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(color("&c只有玩家可以使用此命令"));
                    return true;
                }
                Player player = (Player) sender;
                if (listener == null) return true;

                Location loc = listener.getDeathLocation(player.getUniqueId());
                if (loc == null) {
                    player.sendMessage(color("&c你没有可返回的死亡点"));
                    return true;
                }

                player.teleport(loc);
                player.sendMessage(color("&a已回到死亡点"));
                return true;
            }

            @Override
            public List<String> tabComplete(org.bukkit.command.CommandSender sender, String[] args) {
                return Collections.emptyList();
            }
        });
    }

    /** 复活后发送点击提示 */
    public void sendBackPrompt(Player player, Location deathLoc) {
        player.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        player.sendMessage(color("&c  ☠ 你已死亡"));
        player.sendMessage(color("&7  坐标: &f" + locToString(deathLoc)));

        TextComponent btn = new TextComponent(color("  &a[✔ 回到死亡点]"));
        btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/xbp back"));
        btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text("§a点击传送回死亡点")));

        TextComponent ignore = new TextComponent(color("  &7[✘ 忽略]"));
        ignore.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/xbp"));
        ignore.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text("§7忽略，返回主面板")));

        player.spigot().sendMessage(btn, ignore);
        player.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private String locToString(Location loc) {
        return String.format("%s %d %d %d",
                loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
