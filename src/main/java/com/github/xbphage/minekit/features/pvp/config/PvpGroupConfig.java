package com.github.xbphage.minekit.features.pvp.config;

import java.util.Collections;
import java.util.List;

/**
 * PvP 权限组配置数据。
 * 每个组包含攻击方命令列表和被攻击方命令列表。
 */
public class PvpGroupConfig {

    private final String name;
    private final String permission;
    private final List<String> attackerCommands;
    private final List<String> victimCommands;

    public PvpGroupConfig(String name, String permission,
                          List<String> attackerCommands, List<String> victimCommands) {
        this.name = name;
        this.permission = permission;
        this.attackerCommands = attackerCommands != null ? attackerCommands : Collections.emptyList();
        this.victimCommands = victimCommands != null ? victimCommands : Collections.emptyList();
    }

    public String getName() { return name; }
    public String getPermission() { return permission; }
    public List<String> getAttackerCommands() { return attackerCommands; }
    public List<String> getVictimCommands() { return victimCommands; }
}
