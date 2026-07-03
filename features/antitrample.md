# AntiTrample Feature — 农田防踩踏

## 功能概述

阻止耕地（FARMLAND）因实体踩踏退化为泥土。
只有开关控制，无额外配置。

## 原理

监听 `BlockPhysicsEvent`，当作用在耕地上时取消事件。
耕地因此无法因任何物理更新退化为泥土（包括踩踏、缺水等）。

## 配置

```yaml
features:
  antitrample:
    enabled: true
```

## 边界

| 场景 | 行为 |
|------|------|
| 玩家/生物在耕地上跳跃 | 耕地不退化 |
| 耕地附近无水 | 不会因缺水退化（BlockPhysicsEvent 被拦截） |
| 玩家在耕地上放置方块 | 正常放置，不受影响 |
