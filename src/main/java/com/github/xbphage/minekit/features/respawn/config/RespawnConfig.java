package com.github.xbphage.minekit.features.respawn.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

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

    private List<FrequencyTier> tiers = new ArrayList<>();
    private final Map<String, PermissionOverride> overrides = new HashMap<>();

    public RespawnConfig() {}

    public void load(ConfigurationSection section) {
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
                    String perm = permSection.getString(key + ".权限");
                    int offset = permSection.getInt(key + ".偏移", 0);
                    overrides.put(key, new PermissionOverride(key, perm, offset, immune));
                }
            }
        }
    }

    public PenaltyResult getPenalty(Player player, int deaths) {
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

        int effective = Math.max(0, deaths + offset);
        FrequencyTier tier = findTier(tiers, effective);
        return new PenaltyResult(tier.getHealth(), tier.getFood(), tier.getEffects(), tier.getMessage());
    }

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
            int duration = toInt(entry.get(1));
            int amp = toInt(entry.get(2));
            if (type != null && duration > 0) out.add(new EffectData(type, duration, amp));
        }
    }

    /** 将 Object 转为 int，兼容 Number 和 String 类型 */
    private static int toInt(Object obj) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private String resolveEffect(String name) {
        String alias = EFFECT_ALIASES.get(name);
        return alias != null ? alias : name;
    }

    private List<FrequencyTier> createDefaultTiers() {
        return Arrays.asList(
            new FrequencyTier(0, 20, 20, new ArrayList<>(), "你重生了"),
            new FrequencyTier(1, 19, 18, parseInline(Arrays.asList("slowness",3,0)), "你感到死亡的气息还未完全散去"),
            new FrequencyTier(3, 17, 15, parseInline(Arrays.asList("weakness",15,0), Arrays.asList("mining_fatigue",15,0)), "连续死亡让你感到虚弱"),
            new FrequencyTier(5, 14, 12, parseInline(Arrays.asList("nausea",3,1), Arrays.asList("weakness",30,1), Arrays.asList("mining_fatigue",30,1)), "你连续重生，感到有点疲惫"),
            new FrequencyTier(8, 12, 10, parseInline(Arrays.asList("nausea",5,1), Arrays.asList("weakness",60,1), Arrays.asList("mining_fatigue",60,1)), "你连续重生，感到力不从心"),
            new FrequencyTier(12, 8, 6, parseInline(Arrays.asList("blindness",5,1), Arrays.asList("weakness",90,1), Arrays.asList("mining_fatigue",120,2), Arrays.asList("slowness",5,1)), "你连续重生，感到眼前一黑")
        );
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
