package com.github.xiaobaphage.xbp.features.antitrample.config;

import java.util.Collections;
import java.util.List;

/**
 * 防踩踏权限组配置数据。
 */
public class AntiTrampleGroupConfig {

    private final String name;
    private final String permission;
    private final double damage;
    private final List<AntiTrampleEffect> effects;

    public AntiTrampleGroupConfig(String name, String permission, double damage, List<AntiTrampleEffect> effects) {
        this.name = name;
        this.permission = permission;
        this.damage = Math.max(damage, 0);
        this.effects = effects != null ? effects : Collections.emptyList();
    }

    public String getName() { return name; }
    public String getPermission() { return permission; }
    public double getDamage() { return damage; }
    public List<AntiTrampleEffect> getEffects() { return effects; }

    public static class AntiTrampleEffect {
        private final String type;
        private final int duration;
        private final int amplifier;

        public AntiTrampleEffect(String type, int duration, int amplifier) {
            this.type = type;
            this.duration = duration;
            this.amplifier = amplifier;
        }

        public String getType() { return type; }
        public int getDurationTicks() { return duration * 20; }
        public int getAmplifier() { return amplifier; }
    }
}
