# Respawn Feature — 复活属性给予

## 功能概述

玩家死亡复活后，根据其权限自动设置血量、饱食度、药水效果。
效果不清除玩家原有药水效果，仅叠加配置效果。

---

## 配置段 (config.yml)

```yaml
features:
  respawn:
    enabled: true

respawn:
  # 独立 OP 组开关
  # true  → OP 玩家强制使用 op-group，跳过普通权限匹配
  # false → OP 玩家同普通玩家一样走权限匹配
  enable-op-group: true

  # 独立 OP 组
  op-group:
    health: 40.0
    food: 20
    effects:
      regeneration:
        duration: 600
        amplifier: 3
      speed:
        duration: 600
        amplifier: 1
      fire_resistance:
        duration: 600
        amplifier: 0
      resistance:
        duration: 600
        amplifier: 1

  # 普通权限组（按权限节点最长匹配）
  groups:
    default:
      health: 20.0
      food: 20
      effects: []

    vip:
      permission: "respawn.vip"
      health: 30.0
      food: 20
      effects:
        regeneration:
          duration: 100
          amplifier: 1

    vip_plus:
      permission: "respawn.vip.plus"
      health: 40.0
      food: 20
      effects:
        regeneration:
          duration: 200
          amplifier: 2
        damage_resistance:
          duration: 300
          amplifier: 0

    admin:
      permission: "respawn.admin"
      health: 40.0
      food: 20
      effects:
        regeneration:
          duration: 600
          amplifier: 3
        speed:
          duration: 600
          amplifier: 1
        fire_resistance:
          duration: 600
          amplifier: 0
```

### 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `enable-op-group` | boolean | 是否启用独立 OP 组 |
| `op-group` | object | OP 专用配置 |
| `groups` | list | 普通权限组列表 |
| `groups[].permission` | string | 权限节点（default 组为 null） |
| `groups[].health` | double | 复活后血量 |
| `groups[].food` | int | 复活后饱食度 |
| `effects` | map | 药水效果，键名 = PotionEffectType 名称 |
| `effects[].duration` | int | 效果持续时间（秒，自动转 tick ×20） |
| `effects[].amplifier` | int | 效果等级（0 = I级，1 = II级……） |

---

## 匹配逻辑

### getGroupForPlayer

```
输入: Player
输出: GroupConfig

1. enable-op-group == true 且 player.isOp()?
    ├── 是 → 返回 op-group
    └── 否 → 进入第 2 步

2. 遍历 groups:
    检查 player.hasPermission(group.permission)
    记录所有匹配的组
    选取 permission 字符串最长的组

3. 无任何匹配 → 返回 default 组
```

### 匹配示例

| 玩家权限 | 匹配结果 |
|----------|----------|
| 无任何 respawn 权限 | default 组 |
| `respawn.vip` | vip 组 |
| `respawn.vip` + `respawn.vip.plus` | vip_plus 组（更长） |
| `respawn.admin` | admin 组 |
| OP + enable-op-group: true | op-group（跳过普通匹配） |
| OP + enable-op-group: false | admin 组（按权限匹配） |

---

## 事件处理

### RespawnListener

```java
@EventHandler
public void onPlayerRespawn(PlayerRespawnEvent event) {
    Player player = event.getPlayer();

    // 延迟 1 tick：确保玩家已经完全重生
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if (!player.isOnline()) return;

        GroupConfig group = config.getGroupForPlayer(player);
        if (group == null) return;

        // 1. 设置血量（不超过 MaxHealth）
        player.setHealth(Math.min(group.health, player.getMaxHealth()));

        // 2. 设置饱食度
        player.setFoodLevel(group.food);

        // 3. 施加药水效果（不清除已有效果）
        for (EffectData ef : group.effects) {
            PotionEffectType type = PotionEffectType.getByName(ef.type);
            if (type != null) {
                player.addPotionEffect(
                    new PotionEffect(type, ef.duration * 20, ef.amplifier),
                    true  // 覆盖同类型已有效果
                );
            }
        }
    }, 1L);
}
```

---

## 边界情况

| 场景 | 处理 |
|------|------|
| 配置血量 > 玩家 MaxHealth | setHealth 自动截断 |
| 效果 amplifier < 0 | 校验后忽略 / 设为 0 |
| duration ≤ 0 | 忽略该效果 |
| effects 使用不存在的效果名 | 跳过该效果，日志警告 |
| 配置缺失 health / food | 默认 20.0 / 20 |
| 配置损坏 | 打印错误栈，保持当前配置 |
| 玩家下线后死亡（极端情况） | `if (!player.isOnline()) return` |
