package com.github.xiaobaphage.xbp.core.command;

import com.github.xiaobaphage.xbp.core.Feature;
import com.github.xiaobaphage.xbp.core.FeatureManager;
import com.github.xiaobaphage.xbp.core.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * /xbp 主指令分发器。
 * 内建子命令 + Feature 注册的子命令。
 */
public class XbpCommand implements TabExecutor {

    private static final String PERM_ADMIN = "xbp.admin";

    private final FeatureManager featureManager;
    private final Map<String, SubCommand> builtinCommands = new LinkedHashMap<>();
    /** Feature 注册的子命令（运行时动态增删） */
    private final Map<String, SubCommand> featureCommands = new HashMap<>();

    public XbpCommand(FeatureManager featureManager) {
        this.featureManager = featureManager;
        registerBuiltinCommands();
    }

    private void registerBuiltinCommands() {
        // reload
        builtinCommands.put("reload", new SubCommand() {
            @Override
            public String getName() { return "reload"; }

            @Override
            public String getPermission() { return PERM_ADMIN; }

            @Override
            public boolean execute(CommandSender sender, String[] args) {
                if (!sender.hasPermission(PERM_ADMIN)) {
                    sender.sendMessage(color("&c你没有权限执行此命令"));
                    return true;
                }
                featureManager.reloadAll();
                sender.sendMessage(color("&a配置已重载"));
                return true;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                return Collections.emptyList();
            }
        });

        // feature
        builtinCommands.put("feature", new SubCommand() {
            @Override
            public String getName() { return "feature"; }

            @Override
            public String getPermission() { return PERM_ADMIN; }

            @Override
            public boolean execute(CommandSender sender, String[] args) {
                if (!sender.hasPermission(PERM_ADMIN)) {
                    sender.sendMessage(color("&c你没有权限执行此命令"));
                    return true;
                }
                if (args.length == 0) {
                    sender.sendMessage(color("&c用法: /xbp feature <list|名称 on|off>"));
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
                                ? color("&a✔ 启用")
                                : color("&c✘ 禁用");
                        sender.sendMessage(color("&e" + f.getName())
                                + " - " + f.getDescription()
                                + " [" + status + color("&r]"));
                    }
                    return true;
                }

                // feature <name> on|off
                String featureName = sub;
                if (args.length < 2) {
                    sender.sendMessage(color("&c用法: /xbp feature " + featureName + " <on|off>"));
                    return true;
                }
                Feature f = featureManager.getFeature(featureName);
                if (f == null) {
                    sender.sendMessage(color("&c功能 '" + featureName + "' 不存在"));
                    return true;
                }
                String action = args[1].toLowerCase();
                switch (action) {
                    case "on":
                        featureManager.enableFeature(featureName);
                        sender.sendMessage(color("&a功能 '" + featureName + "' 已启用"));
                        break;
                    case "off":
                        featureManager.disableFeature(featureName);
                        sender.sendMessage(color("&c功能 '" + featureName + "' 已禁用"));
                        break;
                    default:
                        sender.sendMessage(color("&c用法: /xbp feature " + featureName + " <on|off>"));
                }
                return true;
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String[] args) {
                if (args.length == 1) {
                    List<String> suggestions = new ArrayList<>();
                    suggestions.add("list");
                    suggestions.addAll(featureManager.getAllFeatures().keySet());
                    return filter(suggestions, args[0]);
                }
                if (args.length == 2) {
                    return filter(Arrays.asList("on", "off"), args[1]);
                }
                return Collections.emptyList();
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&6/xbp &e- 插件主命令"));
            sender.sendMessage(color("&7  /xbp reload &f- 重载配置"));
            sender.sendMessage(color("&7  /xbp feature list &f- 功能列表"));
            sender.sendMessage(color("&7  /xbp feature <名称> on|off &f- 开关功能"));
            return true;
        }

        String subName = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        // 先查内建命令
        SubCommand sub = builtinCommands.get(subName);
        if (sub != null) {
            return sub.execute(sender, subArgs);
        }

        // 再查 Feature 注册的子命令
        sub = featureCommands.get(subName);
        if (sub != null) {
            if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
                sender.sendMessage(color("&c你没有权限执行此命令"));
                return true;
            }
            return sub.execute(sender, subArgs);
        }

        sender.sendMessage(color("&c未知子命令，请输入 /xbp 查看帮助"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // 合并内建 + feature 子命令
            Set<String> all = new HashSet<>(builtinCommands.keySet());
            all.addAll(featureCommands.keySet());
            return filter(new ArrayList<>(all), args[0]);
        }
        // 二级补全交给子命令
        String subName = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        SubCommand sub = builtinCommands.get(subName);
        if (sub != null) return sub.tabComplete(sender, subArgs);
        sub = featureCommands.get(subName);
        if (sub != null) return sub.tabComplete(sender, subArgs);
        return Collections.emptyList();
    }

    /* ======== Feature 子命令管理 ======== */

    /** 注册一个 Feature 的子命令列表 */
    public void registerFeatureSubCommands(List<SubCommand> commands) {
        for (SubCommand cmd : commands) {
            featureCommands.put(cmd.getName().toLowerCase(), cmd);
        }
    }

    /** 移除一个 Feature 的子命令列表 */
    public void unregisterFeatureSubCommands(List<SubCommand> commands) {
        for (SubCommand cmd : commands) {
            featureCommands.remove(cmd.getName().toLowerCase());
        }
    }

    /* ======== 工具方法 ======== */

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
