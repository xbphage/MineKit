package com.github.xbphage.minekit.features.itemtoexp.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemToExpConfig {

    private final List<ExchangeRecipe> recipes = new ArrayList<>();
    private final Set<Material> lockedMaterials = new HashSet<>();
    private boolean lockEnabled;

    public void load(ConfigurationSection section) {
        recipes.clear();
        lockedMaterials.clear();
        lockEnabled = false;
        if (section == null) return;

        List<String> locked = section.getStringList("禁止存放");
        if (!locked.isEmpty()) {
            lockEnabled = true;
            for (String matStr : locked) {
                Material mat = parseMaterial(matStr);
                if (mat != null) lockedMaterials.add(mat);
            }
        }

        ConfigurationSection recipeSection = section.getConfigurationSection("交换配方");
        if (recipeSection == null) return;

        for (String key : recipeSection.getKeys(false)) {
            ConfigurationSection entry = recipeSection.getConfigurationSection(key);
            if (entry == null) continue;

            String materialStr = entry.getString("物品");
            int amount = entry.getInt("数量", 1);
            int expLevels = entry.getInt("经验等级", 1);

            if (materialStr == null || materialStr.isEmpty() || amount <= 0 || expLevels <= 0) continue;

            recipes.add(new ExchangeRecipe(key, materialStr, amount, expLevels));
        }
    }

    public List<ExchangeRecipe> getRecipes() { return recipes; }

    public ExchangeRecipe getRecipe(String name) {
        for (ExchangeRecipe r : recipes) {
            if (r.getName().equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    public boolean isLockEnabled() { return lockEnabled; }

    public boolean isLocked(Material material) { return lockEnabled && lockedMaterials.contains(material); }

    private Material parseMaterial(String s) {
        String c = s;
        if (c.startsWith("minecraft:") || c.startsWith("MINECRAFT:")) c = c.substring(10);
        return Material.getMaterial(c.toUpperCase());
    }

    public static class ExchangeRecipe {
        private final String name;
        private final String material;
        private final int amount;
        private final int expLevels;

        public ExchangeRecipe(String name, String material, int amount, int expLevels) {
            this.name = name;
            this.material = material;
            this.amount = amount;
            this.expLevels = expLevels;
        }

        public String getName() { return name; }
        public String getMaterial() { return material; }
        public int getAmount() { return amount; }
        public int getExpLevels() { return expLevels; }
    }
}
