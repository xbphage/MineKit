package com.github.xbphage.minekit.features.respawn.config;

import java.util.Collections;
import java.util.List;

public class FrequencyTier {
    private final int threshold;
    private final double health;
    private final int food;
    private final List<EffectData> effects;
    private final String message;

    public FrequencyTier(int threshold, double health, int food, List<EffectData> effects, String message) {
        this.threshold = threshold;
        this.health = health;
        this.food = food;
        this.effects = effects != null ? effects : Collections.emptyList();
        this.message = message;
    }

    public int getThreshold() { return threshold; }
    public double getHealth() { return health; }
    public int getFood() { return food; }
    public List<EffectData> getEffects() { return effects; }
    public String getMessage() { return message; }
}
