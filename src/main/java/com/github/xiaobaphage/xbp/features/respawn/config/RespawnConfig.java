package com.github.xiaobaphage.xbp.features.respawn.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * respawn 功能配置。
 * 负责解析 config.yml 中 respawn 段，提供玩家匹配逻辑。
 */
public class RespawnConfig {

    private static final double DEFAULT_HEALTH = 20.0;
    private static final int DEFAULT_FOOD = 20;

    private final Logger logger;
    private boolean enableOpGroup;
    private GroupConfig opGroup;
    private List<GroupConfig> groups;
    private GroupConfig defaultGroup;

    public RespawnConfig(Logger logger) {
        this.logger = logger;
    }

    /** 从 ConfigurationSection 加载配置 */
    public void load(ConfigurationSection section) {
        if (section == null) {
            logger.warning("[Respawn] 配置段 'respawn' 不存在，使用默认值");
            enableOpGroup = true;
            opGroup = createDefaultOpGroup();
            groups = createDefaultGroups();
            defaultGroup = findDefaultGroup();
            return;
        }

        // 独立 OP 组
        enableOpGroup = section.getBoolean("enable-op-group", true);
        ConfigurationSection opSection = section.getConfigurationSection("op-group");
        if (opSection != null) {
            opGroup = parseGroup("op-group", null, opSection);
        } else {
            opGroup = createDefaultOpGroup();
        }

        // 普通权限组
        ConfigurationSection groupsSection = section.getConfigurationSection("groups");
        groups = new ArrayList<>();
        if (groupsSection != null) {
            for (String key : groupsSection.getKeys(false)) {
                ConfigurationSection gs = groupsSection.getConfigurationSection(key);
                if (gs == null) continue;
                String perm = key.equals("default") ? null : gs.getString("permission");
                GroupConfig g = parseGroup(key, perm, gs);
                groups.add(g);
            }
        }
        if (groups.isEmpty()) {
            groups.addAll(createDefaultGroups());
        }
        defaultGroup = findDefaultGroup();
        if (defaultGroup == null) {
            logger.warning("[Respawn] 未找到 default 组，创建一个默认组");
            defaultGroup = new GroupConfig("default", null, DEFAULT_HEALTH, DEFAULT_FOOD, new ArrayList<>());
            groups.add(0, defaultGroup);
        }
    }

    /**
     * 获取玩家应使用的组配置。
     *
     * 1. OP 组模式 → OP 玩家直接返回 op-group
     * 2. 普通权限最长匹配
     * 3. 无匹配 → default 组
     */
    public GroupConfig getGroupForPlayer(Player player) {
        if (enableOpGroup && player.isOp()) {
            return opGroup;
        }
        GroupConfig best = defaultGroup;
        for (GroupConfig g : groups) {
            String perm = g.getPermission();
            if (perm != null && player.hasPermission(perm)) {
                if (best == defaultGroup
                        || perm.length() > best.getPermission().length()) {
                    best = g;
                }
            }
        }
        return best;
    }

    /* ======== 解析工具 ======== */

    private GroupConfig parseGroup(String name, String permission, ConfigurationSection section) {
        double health = section.getDouble("health", DEFAULT_HEALTH);
        int food = section.getInt("food", DEFAULT_FOOD);
        List<EffectData> effects = new ArrayList<>();

        ConfigurationSection efSection = section.getConfigurationSection("effects");
        if (efSection != null) {
            for (String key : efSection.getKeys(false)) {
                ConfigurationSection es = efSection.getConfigurationSection(key);
                if (es == null) continue;
                int duration = es.getInt("duration", 0);
                int amplifier = es.getInt("amplifier", 0);
                if (duration <= 0) {
                    logger.warning("[Respawn] 效果 '" + key + "' 的 duration ≤ 0，已跳过");
                    continue;
                }
                effects.add(new EffectData(key.toUpperCase(), duration, amplifier));
            }
        }

        return new GroupConfig(name, permission, health, food, effects);
    }

    private GroupConfig findDefaultGroup() {
        for (GroupConfig g : groups) {
            if ("default".equals(g.getName())) return g;
        }
        return null;
    }

    private GroupConfig createDefaultOpGroup() {
        List<EffectData> ops = new ArrayList<>();
        ops.add(new EffectData("REGENERATION", 600, 3));
        ops.add(new EffectData("SPEED", 600, 1));
        ops.add(new EffectData("FIRE_RESISTANCE", 600, 0));
        ops.add(new EffectData("DAMAGE_RESISTANCE", 600, 1));
        return new GroupConfig("op-group", null, 40.0, 20, ops);
    }

    private List<GroupConfig> createDefaultGroups() {
        List<GroupConfig> list = new ArrayList<>();
        list.add(new GroupConfig("default", null, DEFAULT_HEALTH, DEFAULT_FOOD, new ArrayList<>()));

        List<EffectData> vipEffects = new ArrayList<>();
        vipEffects.add(new EffectData("REGENERATION", 100, 1));
        list.add(new GroupConfig("vip", "respawn.vip", 30.0, 20, vipEffects));

        List<EffectData> vipPlusEffects = new ArrayList<>();
        vipPlusEffects.add(new EffectData("REGENERATION", 200, 2));
        vipPlusEffects.add(new EffectData("DAMAGE_RESISTANCE", 300, 0));
        list.add(new GroupConfig("vip_plus", "respawn.vip.plus", 40.0, 20, vipPlusEffects));

        List<EffectData> adminEffects = new ArrayList<>();
        adminEffects.add(new EffectData("REGENERATION", 600, 3));
        adminEffects.add(new EffectData("SPEED", 600, 1));
        adminEffects.add(new EffectData("FIRE_RESISTANCE", 600, 0));
        list.add(new GroupConfig("admin", "respawn.admin", 40.0, 20, adminEffects));

        return list;
    }
}
