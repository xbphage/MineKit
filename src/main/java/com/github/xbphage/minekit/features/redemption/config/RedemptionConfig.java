package com.github.xbphage.minekit.features.redemption.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RedemptionConfig {

    private List<RedemptionItem> items = new ArrayList<>();

    public void load(ConfigurationSection section) {
        items.clear();
        if (section == null) return;
        List<?> raw = section.getList("物品");
        if (raw == null) return;
        for (Object obj : raw) {
            if (obj instanceof ConfigurationSection) {
                ConfigurationSection cs = (ConfigurationSection) obj;
                addItem(cs.getString("物品"), cs.getInt("数量", 1), cs.getInt("降低", 1));
            } else if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                Object mat = map.get("物品");
                if (mat != null) {
                    int amount = map.containsKey("数量") ? toInt(map.get("数量"), 1) : 1;
                    int reduce = map.containsKey("降低") ? toInt(map.get("降低"), 1) : 1;
                    addItem(mat.toString(), amount, reduce);
                }
            }
        }
    }

    /** 将 Object 转为 int，兼容 Number 和 String */
    private static int toInt(Object obj, int def) {
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return def; }
    }

    private void addItem(String mat, int amount, int reduce) {
        if (mat != null && amount > 0 && reduce > 0) {
            items.add(new RedemptionItem(mat, amount, reduce));
        }
    }

    public List<RedemptionItem> getItems() { return items; }

    public static class RedemptionItem {
        private final String material;
        private final int amount;
        private final int reduce;

        public RedemptionItem(String material, int amount, int reduce) {
            this.material = material; this.amount = amount; this.reduce = reduce;
        }
        public String getMaterial() { return material; }
        public int getAmount() { return amount; }
        public int getReduce() { return reduce; }
    }
}
