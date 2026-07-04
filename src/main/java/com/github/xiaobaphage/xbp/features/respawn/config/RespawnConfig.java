package com.github.xiaobaphage.xbp.features.respawn.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * respawn 功能配置。
 * 负责解析 config.yml 中 respawn 段，提供玩家匹配逻辑。
 */
public class RespawnConfig {

    private static final double DEFAULT_HEALTH = 20.0;
    private static final int DEFAULT_FOOD = 20;

    /** 常见效果名 → Bukkit PotionEffectType 名称映射 */
    private static final Map<String, String> EFFECT_ALIASES = new HashMap<>();
    static {
        EFFECT_ALIASES.put("MINING_FATIGUE", "SLOW_DIGGING");
        EFFECT_ALIASES.put("HASTE", "FAST_DIGGING");
        EFFECT_ALIASES.put("NAUSEA", "CONFUSION");
        EFFECT_ALIASES.put("RESISTANCE", "DAMAGE_RESISTANCE");
        EFFECT_ALIASES.put("ABSORPTION", "ABSORPTION");
        EFFECT_ALIASES.put("HEALTH_BOOST", "HEALTH_BOOST");
    }

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
            applyDefaults();
            return;
        }

        // 独立 OP 组
        enableOpGroup = section.getBoolean("启用独立OP组", true);
        ConfigurationSection opSection = section.getConfigurationSection("OP组");
        if (opSection != null) {
            opGroup = parseGroup("op-group", null, opSection);
        } else {
            opGroup = createDefaultOpGroup();
        }

        // 普通权限组
        groups = new ArrayList<>();
        ConfigurationSection groupsSection = section.getConfigurationSection("权限组");
        if (groupsSection != null) {
            for (String key : groupsSection.getKeys(false)) {
                ConfigurationSection gs = groupsSection.getConfigurationSection(key);
                if (gs == null) continue;
                String perm = key.equals("default") ? null : gs.getString("权限");
                groups.add(parseGroup(key, perm, gs));
            }
        }
        if (groups.isEmpty()) {
            groups.addAll(createDefaultGroups());
        }
        defaultGroup = findDefaultGroup();
        if (defaultGroup == null) {
            logger.warning("[Respawn] 未找到 default 组，自动创建");
            defaultGroup = new GroupConfig("default", null, DEFAULT_HEALTH, DEFAULT_FOOD, new ArrayList<>());
            groups.add(0, defaultGroup);
        }
    }

    /**
     * 获取玩家应使用的组配置。
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

    public boolean isEnableOpGroup() { return enableOpGroup; }

    /* ======== 解析工具 ======== */

    /**
     * 解析效果配置，支持两种格式：
     *   list: ["效果名", 持续秒数, 等级]
     *   如: ["mining_fatigue", 30, 1]
     */
    private GroupConfig parseGroup(String name, String permission, ConfigurationSection section) {
        double health = section.getDouble("血量", DEFAULT_HEALTH);
        int food = section.getInt("饱食度", DEFAULT_FOOD);
        List<EffectData> effects = new ArrayList<>();

        List<?> rawList = section.getList("效果");
        if (rawList != null) {
            for (int i = 0; i < rawList.size(); i++) {
                Object obj = rawList.get(i);
                if (!(obj instanceof List)) {
                    logger.warning("[Respawn] 组 '" + name + "' 第 " + (i + 1) + " 个效果格式错误，跳过");
                    continue;
                }
                List<?> entry = (List<?>) obj;
                if (entry.size() < 3) {
                    logger.warning("[Respawn] 组 '" + name + "' 第 " + (i + 1) + " 个效果字段不足，跳过");
                    continue;
                }
                String rawType = entry.get(0).toString();
                int duration = ((Number) entry.get(1)).intValue();
                int amplifier = ((Number) entry.get(2)).intValue();

                if (duration <= 0) {
                    logger.warning("[Respawn] 组 '" + name + "' 效果 '" + rawType + "' 持续时间 ≤ 0，跳过");
                    continue;
                }

                // 解析效果名（支持别名映射）
                String typeName = resolveEffectName(rawType.toUpperCase());
                if (typeName == null) {
                    logger.warning("[Respawn] 未知效果类型: " + rawType);
                    continue;
                }

                effects.add(new EffectData(typeName, duration, amplifier));
            }
        }

        return new GroupConfig(name, permission, health, food, effects);
    }

    /** 将常见效果名映射到 Bukkit PotionEffectType 名称 */
    private String resolveEffectName(String name) {
        // 先查别名映射
        String alias = EFFECT_ALIASES.get(name);
        if (alias != null) return alias;
        // 直接返回原名（如果 Bukkit 能识别）
        return name;
    }

    private GroupConfig findDefaultGroup() {
        for (GroupConfig g : groups) {
            if ("default".equals(g.getName())) return g;
        }
        return null;
    }

    private void applyDefaults() {
        enableOpGroup = true;
        opGroup = createDefaultOpGroup();
        groups = createDefaultGroups();
        defaultGroup = findDefaultGroup();
    }

    /** OP 组：满血满饥饿，无效果 */
    private GroupConfig createDefaultOpGroup() {
        return new GroupConfig("op-group", null, DEFAULT_HEALTH, DEFAULT_FOOD, new ArrayList<>());
    }

    /** default + vip，无 admin 和 vip_plus */
    private List<GroupConfig> createDefaultGroups() {
        List<GroupConfig> list = new ArrayList<>();

        // default：30血10饱食度 + 挖掘疲劳30秒 + 脆弱180秒 + 反胃5秒
        List<EffectData> defaultEffects = new ArrayList<>();
        defaultEffects.add(new EffectData("SLOW_DIGGING", 30, 1));
        defaultEffects.add(new EffectData("WEAKNESS", 180, 1));
        defaultEffects.add(new EffectData("CONFUSION", 5, 1));
        list.add(new GroupConfig("default", null, 30.0, 10, defaultEffects));

        // vip：40血20饱食度，无效果
        list.add(new GroupConfig("vip", "respawn.vip", 40.0, 20, new ArrayList<>()));

        return list;
    }
}
