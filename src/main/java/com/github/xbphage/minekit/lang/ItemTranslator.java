package com.github.xbphage.minekit.lang;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 物品名翻译器。
 * 优先加载 plugins/MineKit/lang/zh_cn.json（用户可放置官方翻译文件），
 * 否则使用内建映射。
 */
public class ItemTranslator {

    private static final Map<String, String> BUILTIN = new HashMap<>();
    private final Map<String, String> cache = new HashMap<>();
    private boolean loadedFromFile;

    static {
        // Minecraft 1.16.5 常用物品中文名（基于官方 zh_cn.json）
        BUILTIN.put("minecraft:diamond", "钻石");
        BUILTIN.put("minecraft:netherite_ingot", "下界合金锭");
        BUILTIN.put("minecraft:emerald", "绿宝石");
        BUILTIN.put("minecraft:gold_ingot", "金锭");
        BUILTIN.put("minecraft:iron_ingot", "铁锭");
        BUILTIN.put("minecraft:copper_ingot", "铜锭");
        BUILTIN.put("minecraft:diamond_block", "钻石块");
        BUILTIN.put("minecraft:netherite_block", "下界合金块");
        BUILTIN.put("minecraft:emerald_block", "绿宝石块");
        BUILTIN.put("minecraft:gold_block", "金块");
        BUILTIN.put("minecraft:iron_block", "铁块");
        BUILTIN.put("minecraft:apple", "苹果");
        BUILTIN.put("minecraft:golden_apple", "金苹果");
        BUILTIN.put("minecraft:enchanted_golden_apple", "附魔金苹果");
        BUILTIN.put("minecraft:ender_pearl", "末影珍珠");
        BUILTIN.put("minecraft:blaze_rod", "烈焰棒");
        BUILTIN.put("minecraft:ghast_tear", "恶魂之泪");
        BUILTIN.put("minecraft:ender_eye", "末影之眼");
        BUILTIN.put("minecraft:shulker_shell", "潜影壳");
        BUILTIN.put("minecraft:nautilus_shell", "鹦鹉螺壳");
        BUILTIN.put("minecraft:heart_of_the_sea", "海洋之心");
        BUILTIN.put("minecraft:nether_star", "下界之星");
        BUILTIN.put("minecraft:dragon_egg", "龙蛋");
        BUILTIN.put("minecraft:elytra", "鞘翅");
        BUILTIN.put("minecraft:totem_of_undying", "不死图腾");
        BUILTIN.put("minecraft:experience_bottle", "附魔之瓶");
        BUILTIN.put("minecraft:bone", "骨头");
        BUILTIN.put("minecraft:gunpowder", "火药");
        BUILTIN.put("minecraft:string", "线");
        BUILTIN.put("minecraft:feather", "羽毛");
        BUILTIN.put("minecraft:leather", "皮革");
        BUILTIN.put("minecraft:slime_ball", "粘液球");
    }

    public ItemTranslator(JavaPlugin plugin) {
        // 尝试加载外部文件
        File external = new File(plugin.getDataFolder(), "lang/zh_cn.json");
        if (external.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new java.io.FileInputStream(external), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                int ch;
                while ((ch = reader.read()) != -1) sb.append((char) ch);
                parseJson(sb.toString());
                loadedFromFile = true;
                plugin.getLogger().info("[翻译] 已加载外部翻译文件");
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("[翻译] 加载外部翻译文件失败，使用内建映射");
            }
        }
    }

    /** 获取物品中文名，如 "minecraft:diamond" → "钻石" */
    public String get(String itemKey) {
        String cached = cache.get(itemKey);
        if (cached != null) return cached;
        String builtin = BUILTIN.get(itemKey);
        if (builtin != null) return builtin;
        // 兜底：返回 key 本身
        return itemKey;
    }

    /** 简易 JSON 解析（仅解析键值对） */
    private void parseJson(String json) {
        // 只处理 "key": "value" 格式，用于翻译文件
        String[] lines = json.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("\"") && line.contains(": \"")) {
                int colon = line.indexOf(": \"");
                String key = line.substring(1, colon - 1).replace("\\\"", "\"");
                String val = line.substring(colon + 3, line.length() - 1);
                if (val.endsWith("\",")) val = val.substring(0, val.length() - 2);
                cache.put(key, val);
            }
        }
    }
}
