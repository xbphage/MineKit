package com.github.xbphage.minekit.sudo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SudoCommand implements TabExecutor {

    private static final String PERM = "minekit.sudo";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 权限检查
        if (!sender.hasPermission(PERM) && !sender.isOp()) {
            sender.sendMessage(color("&c你没有权限执行此命令"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&c用法: /sudo <玩家名> <命令...>"));
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(color("&c玩家 '" + targetName + "' 不在线"));
            return true;
        }

        // 拼接命令（去掉 /sudo 和目标玩家名）
        StringBuilder cmdBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) cmdBuilder.append(" ");
            cmdBuilder.append(args[i]);
        }
        String cmd = cmdBuilder.toString();

        // 创建代理 CommandSender：身份=目标，权限+输出=调用者
        SudoCommandSender proxy = new SudoCommandSender(sender, target.getName());

        // 以代理身份执行命令
        boolean result = Bukkit.dispatchCommand(proxy, cmd);

        if (!result) {
            sender.sendMessage(color("&c命令执行失败，请检查命令是否正确"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
