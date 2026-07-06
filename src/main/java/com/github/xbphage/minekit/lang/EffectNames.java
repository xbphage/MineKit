package com.github.xbphage.minekit.lang;

import java.util.HashMap;
import java.util.Map;

public class EffectNames {
    private static final Map<String, String> MAP = new HashMap<>();
    static {
        MAP.put("SPEED", "速度");
        MAP.put("SLOWNESS", "缓慢");
        MAP.put("SLOW", "缓慢");
        MAP.put("HASTE", "急迫");
        MAP.put("FAST_DIGGING", "急迫");
        MAP.put("SLOW_DIGGING", "挖掘疲劳");
        MAP.put("MINING_FATIGUE", "挖掘疲劳");
        MAP.put("STRENGTH", "力量");
        MAP.put("INCREASE_DAMAGE", "力量");
        MAP.put("JUMP", "跳跃提升");
        MAP.put("JUMP_BOOST", "跳跃提升");
        MAP.put("NAUSEA", "反胃");
        MAP.put("CONFUSION", "反胃");
        MAP.put("REGENERATION", "生命恢复");
        MAP.put("DAMAGE_RESISTANCE", "抗性提升");
        MAP.put("RESISTANCE", "抗性提升");
        MAP.put("FIRE_RESISTANCE", "防火");
        MAP.put("WATER_BREATHING", "水下呼吸");
        MAP.put("INVISIBILITY", "隐身");
        MAP.put("BLINDNESS", "失明");
        MAP.put("NIGHT_VISION", "夜视");
        MAP.put("HUNGER", "饥饿");
        MAP.put("WEAKNESS", "虚弱");
        MAP.put("POISON", "中毒");
        MAP.put("WITHER", "凋零");
        MAP.put("GLOWING", "发光");
        MAP.put("LEVITATION", "悬浮");
        MAP.put("LUCK", "幸运");
        MAP.put("UNLUCK", "霉运");
        MAP.put("SLOW_FALLING", "缓降");
        MAP.put("CONDUIT_POWER", "潮涌能量");
        MAP.put("DOLPHINS_GRACE", "海豚的恩惠");
        MAP.put("BAD_OMEN", "不祥之兆");
        MAP.put("HERO_OF_THE_VILLAGE", "村庄英雄");
        MAP.put("ABSORPTION", "伤害吸收");
        MAP.put("HEALTH_BOOST", "生命提升");
    }

    public static String cn(String key) {
        String result = MAP.get(key.toUpperCase());
        return result != null ? result : key;
    }
}
