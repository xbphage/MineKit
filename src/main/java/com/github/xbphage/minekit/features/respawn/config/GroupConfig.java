package com.github.xbphage.minekit.features.respawn.config;

import java.util.Collections;
import java.util.List;

/**
 * 权限组配置数据。
 */
public class GroupConfig {

    private final String name;               // 组名
    private final String permission;         // 权限节点（default 组为 null）
    private final double health;             // 血量
    private final int food;                  // 饱食度
    private final List<EffectData> effects;  // 药水效果

    public GroupConfig(String name, String permission, double health, int food, List<EffectData> effects) {
        this.name = name;
        this.permission = permission;
        this.health = health;
        this.food = food;
        this.effects = effects != null ? effects : Collections.emptyList();
    }

    public String getName() { return name; }

    public String getPermission() { return permission; }

    public double getHealth() { return health; }

    public int getFood() { return food; }

    public List<EffectData> getEffects() { return effects; }
}
