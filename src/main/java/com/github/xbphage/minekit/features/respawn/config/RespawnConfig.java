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
    private List<FrequencyTier> tiers = new ArrayList<>();
    private Map<String, PermissionOverride> overrides = new HashMap<>();

    public RespawnConfig(Logger logger) {
        this.logger = logger;
    }

    public void load(ConfigurationSection section) {
        enableOpGroup = section != null && section.getBoolean("启用独立OP组", true);
        if (section != null) {
            ConfigurationSection base = section.getConfigurationSection("频率惩罚基数");
            if (base != null) tiers = parseTiers(base.getConfigurationSection("一小时"));
        }
        if (tiers.isEmpty()) tiers = createDefaultTiers();
        tiers.sort(Comparator.comparingInt(FrequencyTier::getThreshold));

        if (section != null) {
            ConfigurationSection permSection = section.getConfigurationSection("权限");
            if (permSection != null) {
                for (String key : permSection.getKeys(false)) {
                    boolean immune = permSection.getBoolean(key + ".完全免疫", false);
                    String perm = immune ? null : permSection.getString(key + ".权限");
                    int offset = permSection.getInt(key + ".偏移", 0);
                    overrides.put(key, new PermissionOverride(key, perm, offset, immune));
                }
            }
        }
    }

    public PenaltyResult getPenalty(Player player, int deaths24h) {
        // 独立 OP 组开关：开启时 OP 强制免疫
        if (enableOpGroup && player.isOp()) {
            return new PenaltyResult(DEFAULT_HEALTH, DEFAULT_FOOD, new ArrayList<>(), null);
        }

        int offset = 0;
        PermissionOverride best = null;
        for (PermissionOverride po : overrides.values()) {
            if (po.perm != null && player.hasPermission(po.perm)) {
                if (best == null || po.perm.length() > best.perm.length()) {
                    best = po;
                }
            }
            // 无权限节点的免疫/偏移配置（如 config 中的 op 段）仅对 OP 生效
            if (po.perm == null && player.isOp()) {
                if (best == null) best = po;
            }
        }
        if (best != null && best.immune) {
            return new PenaltyResult(DEFAULT_HEALTH, DEFAULT_FOOD, new ArrayList<>(), null);
        }
        if (best != null) offset = best.offset;

        int effective = Math.max(0, deaths24h + offset);
        FrequencyTier tier = findTier(tiers, effective);
        return new PenaltyResult(tier.getHealth(), tier.getFood(), tier.getEffects(), tier.getMessage());
    }

    public boolean isEnableOpGroup() { return enableOpGroup; }

    private FrequencyTier findTier(List<FrequencyTier> list, int deaths) {
        FrequencyTier best = list.get(0);
        for (FrequencyTier t : list) {
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

    private List<FrequencyTier> createDefaultTiers() {
        List<FrequencyTier> list = new ArrayList<>();
        list.add(new FrequencyTier(0, 20, 20, parseInline(
                Arrays.asList("speed",60,1), Arrays.asList("haste",60,1),
                Arrays.asList("strength",60,1), Arrays.asList("regeneration",30,1),
                Arrays.asList("night_vision",30,1), Arrays.asList("glowing",60,1)
        ), "你重生了，身体回到了巅峰时期，你感受到身体里有力量在流动"));
        list.add(new FrequencyTier(3, 20, 20, new ArrayList<>(), "你重生了"));
        list.add(new FrequencyTier(5, 18, 18, parseInline(
                Arrays.asList("nausea",2,1), Arrays.asList("weakness",7,1), Arrays.asList("mining_fatigue",7,1)
        ), "你连续重生，感到有点疲惫"));
        list.add(new FrequencyTier(7, 16, 16, parseInline(
                Arrays.asList("nausea",3,1), Arrays.asList("weakness",10,1), Arrays.asList("mining_fatigue",10,1)
        ), "你连续重生，感到有点困倦"));
        list.add(new FrequencyTier(10, 12, 10, parseInline(
                Arrays.asList("nausea",5,1), Arrays.asList("weakness",60,1), Arrays.asList("mining_fatigue",30,1)
        ), "你连续重生，感到力不从心"));
        list.add(new FrequencyTier(15, 10, 8, parseInline(
                Arrays.asList("blindness",5,1), Arrays.asList("weakness",90,1),
                Arrays.asList("mining_fatigue",60,1), Arrays.asList("slowness",3,1)
        ), "你连续重生，感到眼前一黑"));
        return list;
    }

    private List<EffectData> parseInline(List<?>... entries) {
        List<EffectData> list = new ArrayList<>();
        for (List<?> e : entries) {
            String type = resolveEffect(e.get(0).toString().toUpperCase());
            int d = ((Number) e.get(1)).intValue();
            int a = ((Number) e.get(2)).intValue();
            if (type != null && d > 0) list.add(new EffectData(type, d, a));
        }
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
