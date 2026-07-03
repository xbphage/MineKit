package com.github.xiaobaphage.xbp.features.respawn.config;

/**
 * 药水效果数据。
 */
public class EffectData {

    private final String type;       // PotionEffectType 名称
    private final int duration;      // 秒
    private final int amplifier;     // 等级（0 = I级）

    public EffectData(String type, int duration, int amplifier) {
        this.type = type;
        this.duration = Math.max(duration, 0);
        this.amplifier = Math.max(amplifier, 0);
    }

    public String getType() { return type; }

    /** 持续时间（tick） */
    public int getDurationTicks() { return duration * 20; }

    public int getAmplifier() { return amplifier; }
}
