package com.github.xbphage.minekit.features.respawn.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Logger;

public class RespawnConfig {

    private static final double DEFAULT_HEALTH = 20.0;
    private static final int DEFAULT_FOOD = 20;

    private static final Map<String, String> EFFECT_ALIASES = new HashMap<>();
    static {
        EFFECT_ALIASES.put("MINING_FATIGUE", "SLOW_DIGGING");
        EFFECT_ALIASES.put("HASTE", "FAST_DIGGING");
        EFFECT_ALIASES.put("NAUSEA", "CONFUSION");
        EFFECT_ALIASES.put("RESISTANCE", "DAMAGE_RESISTANCE");
        EFFECT_ALIASES.put("ABSORPTION", "ABSORPTION");
        EFFECT_ALIASES.put("HEALTH_BOOST", "HEALTH_BOOST");
        EFFECT_ALIASES.put("NIGHT_VISION", "NIGHT_VISION");
        EFFECT_ALIASES.put("STRENGTH", "INCREASE_DAMAGE");
        EFFECT_ALIASES.put("GLOWING", "GLOWING");
        EFFECT_ALIASES.put("SPEED", "SPEED");
        EFFECT_ALIASES.put("BLINDNESS", "BLINDNESS");
        EFFECT_ALIASES.put("SLOWNESS", "SLOW");
    }

    private final Logger logger;
    private boolean enableOpGroup;
    private List<FrequencyTier> hourlyTiers = new ArrayList<>();
    private List<FrequencyTier> dailyTiers = new ArrayList<>();
    private Map<String, PermissionOverride> overrides = new HashMap<>();
    private PermissionOverride opOverride;

    public RespawnConfig(Logger logger) {
        this.logger = logger;
    }

    public void load(ConfigurationSection section) {
        enableOpGroup = section != null && section.getBoolean("启用独立OP组", true);

        // 频率惩罚基数
        if (section != null) {
            ConfigurationSection base = section.getConfigurationSection("频率惩罚基数");
            if (base != null) {
                hourlyTiers = parseTiers(base.getConfigurationSection("一小时"));
                dailyTiers = parseTiers(base.getConfigurationSection("本天"));
            }
        }
        if (hourlyTiers.isEmpty()) hourlyTiers = createDefaultHourly();
        if (dailyTiers.isEmpty()) dailyTiers = createDefaultDaily();
        hourlyTiers.sort(Comparator.comparingInt(FrequencyTier::getThreshold));
        dailyTiers.sort(Comparator.comparingInt(FrequencyTier::getThreshold));

        // 权限
        if (section != null) {
            ConfigurationSection permSection = section.getConfigurationSection("权限");
            if (permSection != null) {
                for (String key : permSection.getKeys(false)) {
                    boolean immune = permSection.getBoolean(key + ".完全免疫", false);
                    if (immune) {
                        opOverride = new PermissionOverride(key, null, 0, true);
                        continue;
                    }
                    String perm = permSection.getString(key + ".权限");
                    int offset = permSection.getInt(key + ".偏移", 0);
                    overrides.put(key, new PermissionOverride(key, perm, offset, false));
                }
            }
        }
    }

    /** 根据死亡次数和权限获取适用的惩罚档位 */
    public PenaltyResult getPenalty(Player player, int deathsHour, int deathsToday) {
        // OP 免疫
        if (enableOpGroup && player.isOp()) {
            return new PenaltyResult(DEFAULT_HEALTH, DEFAULT_FOOD, new ArrayList<>(), null);
        }

        // 查权限的偏移量
        int offset = 0;
        PermissionOverride best = null;
        for (PermissionOverride po : overrides.values()) {
            if (po.perm != null && player.hasPermission(po.perm)) {
                if (best == null || po.perm.length() > best.perm.length()) {
                    best = po;
                }
            }
        }
        if (best != null && best.immune) {
            return new PenaltyResult(DEFAULT_HEALTH, DEFAULT_FOOD, new ArrayList<>(), null);
        }
        if (best != null) offset = best.offset;

        int effectiveHour = Math.max(0, deathsHour + offset);
        int effectiveDay = Math.max(0, deathsToday + offset);

        FrequencyTier hourTier = findTier(hourlyTiers, effectiveHour);
        FrequencyTier dayTier = findTier(dailyTiers, effectiveDay);

        // "最高"模式：取惩罚更重的（血量更低的）
        FrequencyTier finalTier;
        if (hourTier.getHealth() <= dayTier.getHealth()) {
            finalTier = hourTier;
        } else {
            finalTier = dayTier;
        }

        return new PenaltyResult(finalTier.getHealth(), finalTier.getFood(),
                finalTier.getEffects(), finalTier.getMessage());
    }

    public boolean isEnableOpGroup() { return enableOpGroup; }

    private FrequencyTier findTier(List<FrequencyTier> tiers, int deaths) {
        FrequencyTier best = tiers.get(0); // threshold 0 兜底
        for (FrequencyTier t : tiers) {
            if (deaths >= t.getThreshold()) best = t;
        }
        return best;
    }

    private List<FrequencyTier> parseTiers(ConfigurationSection section) {
        List<FrequencyTier> list = new ArrayList<>();
        if (section == null) return list;
        for (String key : section.getKeys(false)) {
            try {
                int threshold = Integer.parseInt(key);
                double health = section.getDouble(key + ".血量", DEFAULT_HEALTH);
                int food = section.getInt(key + ".饱食度", DEFAULT_FOOD);
                String msg = section.getString(key + ".消息");
                List<EffectData> effects = new ArrayList<>();
                List<?> rawEffects = section.getList(key + ".效果");
                if (rawEffects != null) parseEffects(rawEffects, effects);
                list.add(new FrequencyTier(threshold, health, food, effects, msg));
            } catch (NumberFormatException ignored) {}
        }
        return list;
    }

    private void parseEffects(List<?> rawList, List<EffectData> out) {
        for (Object obj : rawList) {
            if (!(obj instanceof List)) continue;
            List<?> entry = (List<?>) obj;
            if (entry.size() < 3) continue;
            String type = resolveEffect(entry.get(0).toString().toUpperCase());
            int duration = ((Number) entry.get(1)).intValue();
            int amp = ((Number) entry.get(2)).intValue();
            if (type != null && duration > 0) out.add(new EffectData(type, duration, amp));
        }
    }

    private String resolveEffect(String name) {
        String alias = EFFECT_ALIASES.get(name);
        return alias != null ? alias : name;
    }

    private List<FrequencyTier> createDefaultHourly() {
        List<EffectData> e0 = new ArrayList<>(); parseEffects(Arrays.asList(
                Arrays.asList("speed",60,1), Arrays.asList("haste",60,1), Arrays.asList("strength",60,1),
                Arrays.asList("regeneration",30,1), Arrays.asList("night_vision",30,1), Arrays.asList("glowing",60,1)
        ), e0);
        List<FrequencyTier> list = new ArrayList<>();
        list.add(new FrequencyTier(0, 20, 20, e0, "你重生了，身体回到了巅峰时期，你感受到身体里有力量在流动"));
        list.add(new FrequencyTier(3, 20, 20, new ArrayList<>(), "你重生了"));
        List<EffectData> e5 = new ArrayList<>(); parseEffects(Arrays.asList(
                Arrays.asList("nausea",2,1), Arrays.asList("weakness",7,1), Arrays.asList("mining_fatigue",7,1)
        ), e5); list.add(new FrequencyTier(5, 18, 18, e5, "你连续重生，感到有点疲惫"));
        List<EffectData> e7 = new ArrayList<>(); parseEffects(Arrays.asList(
                Arrays.asList("nausea",3,1), Arrays.asList("weakness",10,1), Arrays.asList("mining_fatigue",10,1)
        ), e7); list.add(new FrequencyTier(7, 16, 16, e7, "你连续重生，感到有点困倦"));
        List<EffectData> e10 = new ArrayList<>(); parseEffects(Arrays.asList(
                Arrays.asList("nausea",5,1), Arrays.asList("weakness",60,1), Arrays.asList("mining_fatigue",30,1)
        ), e10); list.add(new FrequencyTier(10, 12, 10, e10, "你连续重生，感到力不从心"));
        List<EffectData> e15 = new ArrayList<>(); parseEffects(Arrays.asList(
                Arrays.asList("blindness",5,1), Arrays.asList("weakness",90,1),
                Arrays.asList("mining_fatigue",60,1), Arrays.asList("slowness",3,1)
        ), e15); list.add(new FrequencyTier(15, 10, 8, e15, "你连续重生，感到眼前一黑"));
        return list;
    }

    private List<FrequencyTier> createDefaultDaily() {
        List<FrequencyTier> list = new ArrayList<>();
        list.add(new FrequencyTier(0, 30, 10, new ArrayList<>(), null));
        list.add(new FrequencyTier(3, 22, 8, new ArrayList<>(), null));
        list.add(new FrequencyTier(6, 16, 6, new ArrayList<>(), null));
        list.add(new FrequencyTier(10, 12, 4, new ArrayList<>(), null));
        list.add(new FrequencyTier(15, 8, 2, new ArrayList<>(), null));
        return list;
    }

    public static class PenaltyResult {
        private final double health; private final int food;
        private final List<EffectData> effects; private final String message;
        public PenaltyResult(double health, int food, List<EffectData> effects, String message) {
            this.health = health; this.food = food; this.effects = effects; this.message = message;
        }
        public double getHealth() { return health; }
        public int getFood() { return food; }
        public List<EffectData> getEffects() { return effects; }
        public String getMessage() { return message; }
    }

    private static class PermissionOverride {
        final String name; final String perm; final int offset; final boolean immune;
        PermissionOverride(String name, String perm, int offset, boolean immune) {
            this.name = name; this.perm = perm; this.offset = offset; this.immune = immune;
        }
    }
}
