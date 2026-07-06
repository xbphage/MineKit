package com.github.xbphage.minekit.features.pvp.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * PvP 功能配置。
 * 负责解析 config.yml 中 pvp 段，提供玩家权限组匹配逻辑。
 */
public class PvpConfig {

    private final Logger logger;
    private boolean cancelVanillaDamage;
    private boolean enableOpGroup;
    private PvpGroupConfig opGroup;
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

        // 独立 OP 组
        enableOpGroup = section.getBoolean("启用独立OP组", true);
        ConfigurationSection opSection = section.getConfigurationSection("OP组");
        if (opSection != null) {
            opGroup = parseGroup(opSection);
        } else {
            opGroup = createDefaultOpGroup();
        }

        // 普通权限组
        ConfigurationSection groupsSection = section.getConfigurationSection("权限组");
        if (groupsSection != null) {
            for (String key : groupsSection.getKeys(false)) {
                ConfigurationSection gs = groupsSection.getConfigurationSection(key);
                if (gs == null) continue;
                String perm = key.equals("default") ? null : gs.getString("权限");
                List<String> atkCmds = gs.getStringList("攻击者命令");
                List<String> vicCmds = gs.getStringList("被攻击者命令");
                groups.add(new PvpGroupConfig(key, perm, atkCmds, vicCmds));
            }
        }
        if (groups.isEmpty()) {
            logger.warning("[PvP] 未找到权限组配置，使用默认值");
            createDefaultGroups();
        }

        defaultGroup = findDefaultGroup();
        if (defaultGroup == null) {
            logger.warning("[PvP] 未找到 default 组，自动创建");
            defaultGroup = new PvpGroupConfig("default", null, new ArrayList<>(), new ArrayList<>());
            groups.add(0, defaultGroup);
        }
    }

    public boolean isCancelVanillaDamage() { return cancelVanillaDamage; }
    public boolean isEnableOpGroup() { return enableOpGroup; }
    public PvpGroupConfig getOpGroup() { return opGroup; }
    public List<PvpGroupConfig> getGroups() { return groups; }

    /**
     * 获取玩家匹配的 PvP 组。
     * 1. OP 组模式 → OP 玩家直接返回 OP组
     * 2. 普通权限最长匹配
     * 3. 无匹配 → default 组
     */
    public PvpGroupConfig getGroupForPlayer(Player player) {
        if (enableOpGroup && player.isOp()) {
            return opGroup;
        }
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

    /** 获取攻击者对应的组（用于 attacker-commands） */
    public PvpGroupConfig getGroupForAttacker(Player attacker) {
        return getGroupForPlayer(attacker);
    }

    /** 获取被攻击者对应的组（用于 victim-commands） */
    public PvpGroupConfig getGroupForVictim(Player victim) {
        return getGroupForPlayer(victim);
    }

    private PvpGroupConfig parseGroup(ConfigurationSection section) {
        List<String> atkCmds = section.getStringList("攻击者命令");
        List<String> vicCmds = section.getStringList("被攻击者命令");
        return new PvpGroupConfig("op-group", null, atkCmds, vicCmds);
    }

    private PvpGroupConfig findDefaultGroup() {
        for (PvpGroupConfig g : groups) {
            if ("default".equals(g.getName())) return g;
        }
        return null;
    }

    private void createDefault() {
        cancelVanillaDamage = false;
        enableOpGroup = true;
        opGroup = createDefaultOpGroup();
        createDefaultGroups();
        defaultGroup = findDefaultGroup();
    }

    private PvpGroupConfig createDefaultOpGroup() {
        return new PvpGroupConfig("op-group", null, new ArrayList<>(), new ArrayList<>());
    }

    private void createDefaultGroups() {
        groups.add(new PvpGroupConfig("default", null, new ArrayList<>(), new ArrayList<>()));
        groups.add(new PvpGroupConfig("vip", "pvp.vip", new ArrayList<>(), new ArrayList<>()));
    }
}
