package com.github.xiaobaphage.xbp.features.antitrample.config;

import com.github.xiaobaphage.xbp.features.antitrample.config.AntiTrampleGroupConfig.AntiTrampleEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AntiTrampleConfig {

    private static final Map<String, String> EFFECT_ALIASES = new HashMap<>();
    static {
        EFFECT_ALIASES.put("MINING_FATIGUE", "SLOW_DIGGING");
        EFFECT_ALIASES.put("NAUSEA", "CONFUSION");
        EFFECT_ALIASES.put("SLOWNESS", "SLOW");
        EFFECT_ALIASES.put("RESISTANCE", "DAMAGE_RESISTANCE");
    }

    private final Logger logger;
    private boolean enableOpGroup;
    private AntiTrampleGroupConfig opGroup;
    private List<AntiTrampleGroupConfig> groups;
    private AntiTrampleGroupConfig defaultGroup;

    public AntiTrampleConfig(Logger logger) {
        this.logger = logger;
    }

    public void load(ConfigurationSection section) {
        if (section == null) {
            logger.warning("[防踩踏] 配置段不存在，使用默认值");
            applyDefaults();
            return;
        }

        enableOpGroup = section.getBoolean("启用独立OP组", true);
        ConfigurationSection opSection = section.getConfigurationSection("OP组");
        if (opSection != null) {
            opGroup = parseGroup("op-group", null, opSection);
        } else {
            opGroup = createDefaultOpGroup();
        }

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
            logger.warning("[防踩踏] 未找到 default 组，自动创建");
            defaultGroup = new AntiTrampleGroupConfig("default", null, 0, new ArrayList<>());
            groups.add(0, defaultGroup);
        }
    }

    public AntiTrampleGroupConfig getGroupForPlayer(Player player) {
        if (enableOpGroup && player.isOp()) {
            return opGroup;
        }
        AntiTrampleGroupConfig best = defaultGroup;
        for (AntiTrampleGroupConfig g : groups) {
            String perm = g.getPermission();
            if (perm != null && player.hasPermission(perm)) {
                if (best == defaultGroup || perm.length() > best.getPermission().length()) {
                    best = g;
                }
            }
        }
        return best;
    }

    public boolean isEnableOpGroup() { return enableOpGroup; }
    public AntiTrampleGroupConfig getOpGroup() { return opGroup; }
    public List<AntiTrampleGroupConfig> getGroups() { return groups; }

    private AntiTrampleGroupConfig parseGroup(String name, String permission, ConfigurationSection section) {
        double damage = section.getDouble("伤害", 0);
        List<AntiTrampleEffect> effects = new ArrayList<>();

        List<?> rawList = section.getList("效果");
        if (rawList != null) {
            for (int i = 0; i < rawList.size(); i++) {
                Object obj = rawList.get(i);
                if (!(obj instanceof List)) continue;
                List<?> entry = (List<?>) obj;
                if (entry.size() < 3) continue;

                String rawType = entry.get(0).toString();
                int duration = ((Number) entry.get(1)).intValue();
                int amplifier = ((Number) entry.get(2)).intValue();
                if (duration <= 0) continue;

                String typeName = resolveEffect(rawType.toUpperCase());
                if (typeName == null) {
                    logger.warning("[防踩踏] 未知效果: " + rawType);
                    continue;
                }
                effects.add(new AntiTrampleEffect(typeName, duration, amplifier));
            }
        }
        return new AntiTrampleGroupConfig(name, permission, damage, effects);
    }

    private String resolveEffect(String name) {
        String alias = EFFECT_ALIASES.get(name);
        return alias != null ? alias : name;
    }

    private AntiTrampleGroupConfig findDefaultGroup() {
        for (AntiTrampleGroupConfig g : groups) {
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

    private AntiTrampleGroupConfig createDefaultOpGroup() {
        return new AntiTrampleGroupConfig("op-group", null, 0, new ArrayList<>());
    }

    private List<AntiTrampleGroupConfig> createDefaultGroups() {
        List<AntiTrampleGroupConfig> list = new ArrayList<>();
        List<AntiTrampleEffect> de = new ArrayList<>();
        de.add(new AntiTrampleEffect("CONFUSION", 10, 1));
        de.add(new AntiTrampleEffect("SLOW", 30, 1));
        de.add(new AntiTrampleEffect("WEAKNESS", 90, 1));
        de.add(new AntiTrampleEffect("SLOW_DIGGING", 90, 1));
        list.add(new AntiTrampleGroupConfig("default", null, 5, de));
        list.add(new AntiTrampleGroupConfig("vip", "antitrample.vip", 0, new ArrayList<>()));
        return list;
    }
}
