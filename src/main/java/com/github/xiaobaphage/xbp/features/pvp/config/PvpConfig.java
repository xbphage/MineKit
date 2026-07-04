package com.github.xiaobaphage.xbp.features.pvp.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * PvP 功能配置。
 * 负责解析 config.yml 中 pvp 段，提供玩家权限组匹配逻辑。
 */
public class PvpConfig {

    private final Logger logger;
    private boolean cancelVanillaDamage;
    private List<PvpGroupConfig> groups;
    private PvpGroupConfig defaultGroup;

    public PvpConfig(Logger logger) {
        this.logger = logger;
    }

    /** 从 ConfigurationSection 加载配置 */
    public void load(ConfigurationSection section) {
        groups = new ArrayList<>();

        if (section == null) {
            logger.warning("[PvP] 配置段 'pvp' 不存在，使用默认值");
            createDefault();
            return;
        }

        // 是否取消原版伤害
        cancelVanillaDamage = section.getBoolean("取消原版伤害", false);

        ConfigurationSection groupsSection = section.getConfigurationSection("权限组");

        if (groupsSection == null) {
            logger.warning("[PvP] 未找到 groups 配置，使用默认值");
            createDefault();
            return;
        }

        for (String key : groupsSection.getKeys(false)) {
            ConfigurationSection gs = groupsSection.getConfigurationSection(key);
            if (gs == null) continue;

            String perm = key.equals("default") ? null : gs.getString("权限");
            List<String> atkCmds = gs.getStringList("攻击者命令");
            List<String> vicCmds = gs.getStringList("被攻击者命令");

            groups.add(new PvpGroupConfig(key, perm, atkCmds, vicCmds));
        }

        // 查找 default 组
        defaultGroup = findDefaultGroup();
        if (defaultGroup == null) {
            logger.warning("[PvP] 未找到 default 组，自动创建");
            defaultGroup = new PvpGroupConfig("default", null, new ArrayList<>(), new ArrayList<>());
            groups.add(0, defaultGroup);
        }
    }

    /**
     * 获取玩家匹配的 PvP 组（最长权限匹配）。
     * 无匹配时返回 default 组。
     */
    public boolean isCancelVanillaDamage() { return cancelVanillaDamage; }

    public PvpGroupConfig getGroupForPlayer(Player player) {
        PvpGroupConfig best = defaultGroup;
        for (PvpGroupConfig g : groups) {
            String perm = g.getPermission();
            if (perm != null && player.hasPermission(perm)) {
                if (best == defaultGroup || perm.length() > best.getPermission().length()) {
                    best = g;
                }
            }
        }
        return best;
    }

    private PvpGroupConfig findDefaultGroup() {
        for (PvpGroupConfig g : groups) {
            if ("default".equals(g.getName())) return g;
        }
        return null;
    }

    private void createDefault() {
        defaultGroup = new PvpGroupConfig("default", null, new ArrayList<>(), new ArrayList<>());
        groups.add(defaultGroup);
        groups.add(new PvpGroupConfig("warrior", "pvp.warrior",
                Arrays.asList("say 我攻击了 %pvp_victim% ，伤害 %pvp_damage%"),
                Arrays.asList("damage %player% 2")));
        groups.add(new PvpGroupConfig("tank", "pvp.tank",
                new ArrayList<>(),
                Arrays.asList("effect give %player% minecraft:absorption 10 1")));
    }
}
