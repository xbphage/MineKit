# PvP Feature — PvP 检测执行命令

## 功能概述

检测玩家攻击玩家行为，取消原版伤害，根据**攻击者和被攻击者各自的权限**分别执行自定义命令。

命令支持四个 PvP 专用占位符 + 所有 PlaceholderAPI 标准占位符。

---

## 占位符

| 占位符 | 说明 |
|--------|------|
| `%pvp_attacker%` | 攻击者玩家名 |
| `%pvp_victim%` | 被攻击者玩家名 |
| `%pvp_damage%` | 造成的伤害数值 |
| `%pvp_weapon%` | 武器名称（优先显示自定义名称） |
| `%player%` | PlaceholderAPI 上下文玩家 |
| `%player_health%` | 上下文玩家当前血量（PAPI 标准） |

**上下文玩家**：攻击者命令中 `%player%` = 攻击者，被攻击者命令中 `%player%` = 被攻击者。

---

## 配置

```yaml
pvp:
  groups:
    default:
      attacker-commands: []
      victim-commands:
        - "damage %player% 2"

    warrior:
      permission: "pvp.warrior"
      attacker-commands:
        - "say 我攻击了 %pvp_victim% ，造成 %pvp_damage% 伤害"
        - "give %player% minecraft:cooked_beef 1"
      victim-commands:
        - "damage %player% 4"

    tank:
      permission: "pvp.tank"
      attacker-commands: []
      victim-commands:
        - "effect give %player% minecraft:absorption 10 1"
```

### 字段说明

| 字段 | 说明 |
|------|------|
| `attacker-commands` | 该权限玩家攻击别人时，执行的命令列表 |
| `victim-commands` | 该权限玩家被攻击时，执行的命令列表 |
| `permission` | 权限节点（default 组不需要） |

### 权限匹配

同 Respawn 功能：**最长权限匹配**。玩家拥有多个匹配权限时，取 permission 字符串最长的组。

---

## 逻辑流程

```
EntityDamageByEntityEvent (攻击者→被攻击者)
    ↓
取消原版伤害事件（setCancelled(true)）
    ↓
根据攻击者权限 → 获取攻击者权限组 → 执行 attacker-commands（上下文=攻击者）
根据被攻击者权限 → 获取被攻击者权限组 → 执行 victim-commands（上下文=被攻击者）
    ↓
命令中的占位符替换：
  1. 替换 %pvp_attacker% / %pvp_victim% / %pvp_damage% / %pvp_weapon%
  2. PlaceholderAPI 替换 %player%, %player_health% 等标准占位符
    ↓
控制台执行命令
```

## 示例

| 场景 | 行为 |
|------|------|
| default 玩家被攻击 | 受到 `damage %player% 2` 的伤害 |
| 攻击者有 `pvp.warrior` | 攻击者获得熟牛肉，发送消息 |
| 被攻击者有 `pvp.tank` | 获得 10 秒伤害吸收 |

## 依赖

需要服务器安装 **PlaceholderAPI** 插件。未安装时 PvP 功能仍可工作，但 PAPI 标准占位符不会被替换。
