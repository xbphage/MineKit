package com.github.xbphage.minekit.core;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 二级子命令接口。
 * 所有功能模块的子命令均实现此接口，注册到 /xbp 主命令下。
 */
public interface SubCommand {

    /** 子命令名称，如 "reload" */
    String getName();

    /** 执行此子命令所需的权限节点，null 表示无需额外权限 */
    String getPermission();

    /** 执行子命令逻辑 */
    boolean execute(CommandSender sender, String[] args);

    /** Tab 补全 */
    List<String> tabComplete(CommandSender sender, String[] args);
}
