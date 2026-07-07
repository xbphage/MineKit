package com.github.xbphage.minekit.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 物品名翻译器。
 * 从打包在 JAR 内的官方 zh_cn.json 加载翻译数据，支持外部文件覆盖。
 * 支持 minecraft:xxx、item.minecraft.xxx、block.minecraft.xxx 三种键格式查表。
 */
public class ItemTranslator {

    private final Map<String, String> cache = new HashMap<>();
    private boolean loadedFromFile = false;

    public ItemTranslator(JavaPlugin plugin) {
        // 1. 加载 JAR 内内置翻译资源（必选）
        loadFromResource(plugin);

        // 2. 加载外部文件覆盖（可选，plugins/MineKit/lang/zh_cn.json）
        File external = new File(plugin.getDataFolder(), "lang/zh_cn.json");
        if (external.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(external), StandardCharsets.UTF_8)) {
                loadFromJson(reader);
                loadedFromFile = true;
                plugin.getLogger().info("[翻译] 已加载外部翻译文件");
            } catch (Exception e) {
                plugin.getLogger().warning("[翻译] 加载外部翻译文件失败: " + e.getMessage());
            }
        }
    }

    /** 从 JAR 内资源文件加载翻译 */
    private void loadFromResource(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("lang/zh_cn.json")) {
            if (in == null) {
                plugin.getLogger().warning("[翻译] 未找到内置翻译文件 lang/zh_cn.json");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                loadFromJson(reader);
                plugin.getLogger().info("[翻译] 已加载内置翻译文件 (" + cache.size() + " 条)");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[翻译] 加载内置翻译文件失败: " + e.getMessage());
        }
    }

    /** 使用 Gson 解析 JSON 翻译流 */
    private void loadFromJson(InputStreamReader reader) {
        JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();

            // 只处理物品和方块相关的翻译
            if (key.startsWith("item.minecraft.") || key.startsWith("block.minecraft.")) {
                cache.put(key, value);
                // 同时以 minecraft:xxx 格式存储，方便直接从配置中查找
                cache.put(key.replaceFirst("^(item|block)\\.", ""), value);
            }
        }
    }

    /**
     * 获取物品中文名。
     * 支持 minecraft:diamond、item.minecraft.diamond、DIAMOND 等输入格式。
     */
    public String get(String itemKey) {
        if (itemKey == null) return "未知";

        // 1. 直接匹配（缓存命中）
        String cached = cache.get(itemKey);
        if (cached != null) return cached;

        // 2. minecraft:xxx → 尝试 item.minecraft.xxx 和 block.minecraft.xxx
        if (itemKey.contains(":")) {
            String dotKey = itemKey.replace(":", ".");
            String val = cache.get("item." + dotKey);
            if (val != null) { cache.put(itemKey, val); return val; }
            val = cache.get("block." + dotKey);
            if (val != null) { cache.put(itemKey, val); return val; }
        }

        // 3. 兜底：格式化为英文名
        String fallback = formatEnglishName(itemKey);
        cache.put(itemKey, fallback);
        return fallback;
    }

    /** 将英文 key 格式化为可读文本，如 "minecraft:rotten_flesh" → "Rotten Flesh" */
    private String formatEnglishName(String key) {
        String clean = key;
        if (clean.startsWith("minecraft:") || clean.startsWith("MINECRAFT:"))
            clean = clean.substring(10);
        if (clean.startsWith("item.minecraft.") || clean.startsWith("block.minecraft."))
            clean = clean.substring(clean.lastIndexOf('.') + 1);
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : clean.toCharArray()) {
            if (c == '_') { nextUpper = true; sb.append(' '); }
            else if (nextUpper) { sb.append(Character.toUpperCase(c)); nextUpper = false; }
            else { sb.append(Character.toLowerCase(c)); }
        }
        return sb.toString();
    }

    public boolean isLoadedFromFile() {
        return loadedFromFile;
    }
}
