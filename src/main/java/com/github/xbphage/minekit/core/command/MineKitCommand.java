package com.github.xbphage.minekit.core.command;

import com.github.xbphage.minekit.config.ConfigUpgrader;
import com.github.xbphage.minekit.config.ConfigValidator;
import com.github.xbphage.minekit.core.Feature;
import com.github.xbphage.minekit.core.FeatureManager;
import com.github.xbphage.minekit.core.SubCommand;
import com.github.xbphage.minekit.debug.DebugUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation", "unused"})
public class MineKitCommand implements TabExecutor {

    private static final String PERM_ADMIN = "minekit.admin";

    private final JavaPlugin plugin;
    private final FeatureManager featureManager;
    private final Map<String, SubCommand> builtinCommands = new LinkedHashMap<>();
    private final Map<String, SubCommand> featureCommands = new HashMap<>();

    public MineKitCommand(JavaPlugin plugin, FeatureManager featureManager) {
        this.plugin = plugin;
        this.featureManager = featureManager;
        registerBuiltinCommands();
    }

    private void registerBuiltinCommands() {
        builtinCommands.put("reload", new SubCommand() {
            @Override public String getName() { return "reload"; }
            @Override public String getPermission() { return PERM_ADMIN; }

            @Override
            public boolean execute(CommandSender sender, String[] args) {
                if (!sender.hasPermission(PERM_ADMIN)) {
                    sender.sendMessage(color("&c你没有权限执行此命令"));
                    return true;
                }
                ConfigUpgrader.upgrade(plugin);
                DebugUtil.reload(plugin.getConfig());
                ConfigValidator.validate(plugin);
                featureManager.reloadAll();
                sender.sendMessage(color("&a配置已重载"));
                return true;
            }

            @Override public List<String> tabComplete(CommandSender sender, String[] args) {
                return Collections.emptyList();
            }
        });

        builtinCommands.put("feature", new SubCommand() {
            @Override public String getName() { return "feature"; }
            @Override public String getPermission() { return PERM_ADMIN; }

            @Override
            public boolean execute(CommandSender sender, String[] args) {
                if (!sender.hasPermission(PERM_ADMIN)) {
                    sender.sendMessage(color("&c你没有权限执行此命令"));
                    return true;
                }
                if (args.length == 0) {
                    sender.sendMessage(color("&c用法: /minekit feature <list|名称 on|off>"));
                    return true;
                }
                String sub = args[0].toLowerCase();
                if ("list".equals(sub)) {
                    Map<String, Feature> all = featureManager.getAllFeatures();
                    if (all.isEmpty()) {
                        sender.sendMessage(color("&7没有已注册的功能"));
                        return true;
                    }
                    sender.sendMessage(color("&6===== 功能列表 ====="));
                    for (Feature f : all.values()) {
                        String status = featureManager.isActive(f.getName())
                                ? color("&a✔ 启用") : color("&c✘ 禁用");
                        sender.sendMessage(color("&e" + f.getName())
                                + " - " + f.getDescription()
                                + " [" + status + color("&r]"));
                    }
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(color("&c用法: /minekit feature " + sub + " <on|off|info>"));
                    return true;
                }
                Feature f = featureManager.getFeature(sub);
                if (f == null) {
                    sender.sendMessage(color("&c功能 '" + sub + "' 不存在"));
                    return true;
                }
                String action = args[1].toLowerCase();
                switch (action) {
                    case "info": showFeatureConfig(sender, f); break;
                    case "on":
                        featureManager.enableFeature(sub);
                        sender.sendMessage(color("&a功能 '" + sub + "' 已启用"));
                        break;
                    case "off":
                        featureManager.disableFeature(sub);
                        sender.sendMessage(color("&c功能 '" + sub + "' 已禁用"));
                        break;
                    default:
                        sender.sendMessage(color("&c用法: /minekit feature " + sub + " <on|off|info>"));
                }
                return true;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    List<String> list = new ArrayList<>();
                    list.add("list");
                    list.addAll(featureManager.getAllFeatures().keySet());
                    return filter(list, args[0]);
                }
                if (args.length == 2) return filter(Arrays.asList("on", "off"), args[1]);
                return Collections.emptyList();
            }
        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!sender.isOp() && !sender.hasPermission(PERM_ADMIN)) {
                sender.sendMessage(color("&c你没有权限执行此命令"));
                return true;
            }
            sendDashboard(sender);
            return true;
        }
        String subName = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        SubCommand sub = builtinCommands.get(subName);
        if (sub != null) return sub.execute(sender, subArgs);
        sub = featureCommands.get(subName);
        if (sub != null) {
            if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
                sender.sendMessage(color("&c你没有权限执行此命令"));
                return true;
            }
            return sub.execute(sender, subArgs);
        }
        sender.sendMessage(color("&c未知子命令，请输入 /minekit 查看面板"));
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            Set<String> all = new HashSet<>(builtinCommands.keySet());
            all.addAll(featureCommands.keySet());
            return filter(new ArrayList<>(all), args[0]);
        }
        String subName = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        SubCommand sub = builtinCommands.get(subName);
        if (sub != null) return sub.tabComplete(sender, subArgs);
        sub = featureCommands.get(subName);
        if (sub != null) return sub.tabComplete(sender, subArgs);
        return Collections.emptyList();
    }

    private void showFeatureConfig(CommandSender sender, Feature feature) {
        if (!(sender instanceof Player) && !sender.hasPermission(PERM_ADMIN)) {
            sender.sendMessage(color("&c你没有权限查看配置"));
            return;
        }
        for (String line : feature.getConfigDetail(plugin)) {
            sender.sendMessage(color(line));
        }
    }

    private void sendDashboard(CommandSender sender) {
        if (sender instanceof Player) {
            sendPlayerDashboard((Player) sender);
        } else {
            sendConsoleDashboard(sender);
        }
    }

    @SuppressWarnings("all")
    private void sendPlayerDashboard(Player player) {
        player.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        player.sendMessage(color("&6  &lMineKit &e插件 &7- &f模块化功能管理"));
        player.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        player.sendMessage("");

        for (Feature f : featureManager.getAllFeatures().values()) {
            boolean active = featureManager.isActive(f.getName());
            String name = f.getName();
            String desc = f.getDescription();

            TextComponent toggleBtn = new TextComponent(active
                    ? color("&a  [✔ 开启]") : color("&7  [✘ 关闭]"));
            toggleBtn.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/minekit feature " + name + " " + (active ? "off" : "on")));
            toggleBtn.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new Text(active ? "§c点击关闭 " + name + "\n§7当前: §a已启用"
                            : "§a点击开启 " + name + "\n§7当前: §c已关闭")));

            TextComponent nameComp = new TextComponent(color(" &f" + name));
            nameComp.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, "/minekit feature " + name + " info"));
            nameComp.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new Text("§6" + name + "\n§7" + desc + "\n\n§e点击查看详细配置")));

            String statusText = active ? color("&8[&a已启用&8]") : color("&8[&c已关闭&8]");

            player.spigot().sendMessage(toggleBtn, nameComp,
                    new TextComponent(color(" &7- " + desc + " " + statusText)));
        }

        player.sendMessage("");

        TextComponent reloadBtn = new TextComponent(color("  &a[🔄 重载配置]"));
        reloadBtn.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/minekit reload"));
        reloadBtn.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new Text("§a点击重载所有配置\n§7重新读取 config.yml")));

        TextComponent listBtn = new TextComponent(color("  &b[📋 功能列表]"));
        listBtn.setClickEvent(new ClickEvent(
                ClickEvent.Action.RUN_COMMAND, "/minekit feature list"));
        listBtn.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new Text("§b查看所有功能详细状态")));

        player.spigot().sendMessage(reloadBtn, listBtn);
        player.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private void sendConsoleDashboard(CommandSender sender) {
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &lMineKit &e插件 &7- &f模块化功能管理"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage("");
        sender.sendMessage(color("&6  &l功能状态"));
        for (Feature f : featureManager.getAllFeatures().values()) {
            String icon = featureManager.isActive(f.getName())
                    ? color("&a✔") : color("&c✘");
            sender.sendMessage(color("&7  " + icon + " &f" + f.getName() + "&7: &f" + f.getDescription()));
        }
        sender.sendMessage("");
        sender.sendMessage(color("&6  &l命令"));
        sender.sendMessage(color("&7  &e/minekit reload &7- 重载配置"));
        sender.sendMessage(color("&7  &e/minekit feature list &7- 功能列表"));
        sender.sendMessage(color("&7  &e/minekit feature <名称> on|off &7- 开关功能"));
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    public void registerFeatureSubCommands(List<SubCommand> commands) {
        for (SubCommand cmd : commands) {
            featureCommands.put(cmd.getName().toLowerCase(), cmd);
        }
    }

    public void unregisterFeatureSubCommands(List<SubCommand> commands) {
        for (SubCommand cmd : commands) {
            featureCommands.remove(cmd.getName().toLowerCase());
        }
    }

    private static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private static List<String> filter(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        return list.stream()
                .filter(s -> s != null && s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
