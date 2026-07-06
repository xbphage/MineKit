package com.github.xbphage.minekit.sudo;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

/**
 * 代理 CommandSender。
 * 身份=目标玩家名（命令上下文），权限+输出= sudo 调用者。
 */
public class SudoCommandSender implements CommandSender {

    private final CommandSender sudoer;
    private final String targetName;

    public SudoCommandSender(CommandSender sudoer, String targetName) {
        this.sudoer = sudoer;
        this.targetName = targetName;
    }

    // ── 输出 → sudo 调用者 ──
    @Override public void sendMessage(String message) { sudoer.sendMessage(message); }
    @Override public void sendMessage(String[] messages) { sudoer.sendMessage(messages); }
    @Override public void sendMessage(UUID sender, String message) { sudoer.sendMessage(sender, message); }
    @Override public void sendMessage(UUID sender, String[] messages) { sudoer.sendMessage(sender, messages); }

    // ── 权限 → sudo 调用者 ──
    @Override public boolean isOp() { return sudoer.isOp(); }
    @Override public void setOp(boolean value) { sudoer.setOp(value); }
    @Override public boolean isPermissionSet(String name) { return sudoer.isPermissionSet(name); }
    @Override public boolean isPermissionSet(Permission perm) { return sudoer.isPermissionSet(perm); }
    @Override public boolean hasPermission(String name) { return sudoer.hasPermission(name); }
    @Override public boolean hasPermission(Permission perm) { return sudoer.hasPermission(perm); }
    @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) { return sudoer.addAttachment(plugin, name, value); }
    @Override public PermissionAttachment addAttachment(Plugin plugin) { return sudoer.addAttachment(plugin); }
    @Override public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) { return sudoer.addAttachment(plugin, name, value, ticks); }
    @Override public PermissionAttachment addAttachment(Plugin plugin, int ticks) { return sudoer.addAttachment(plugin, ticks); }
    @Override public void removeAttachment(PermissionAttachment attachment) { sudoer.removeAttachment(attachment); }
    @Override public void recalculatePermissions() { sudoer.recalculatePermissions(); }
    @Override public Set<PermissionAttachmentInfo> getEffectivePermissions() { return sudoer.getEffectivePermissions(); }

    // ── 身份 → 目标玩家 ──
    @Override public String getName() { return targetName; }
    @Override public @NotNull Server getServer() { return Bukkit.getServer(); }
    @Override public @NotNull Spigot spigot() { return sudoer.spigot(); }
}
