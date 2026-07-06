# MineKit Plugin — AI 开发指引

## 项目概览
基于 Paper/Spigot 1.16.5 的模块化插件，Maven 构建（Java 1.8）。

## 指令体系

**主命令唯一入口：`/minekit`**

所有功能模块的指令均为 `/minekit` 的二级子命令（SubCommand），绝不新增顶层指令。

内建子命令（MineKitCommand 提供）：
```
/minekit reload                  — 重载所有配置
/minekit feature list            — 列出所有功能及开关状态
/minekit feature <name> on       — 启用指定功能
/minekit feature <name> off      — 禁用指定功能
```

### 新增功能模块时的指令注册规则
- 功能模块实现 `SubCommand` 接口
- 在 `Feature.getSubCommands()` 中返回
- **FeatureManager 自动注册**：`enableFeature()` 时自动注册子命令到 MineKitCommand，`disableFeature()` 时自动注销
- Feature 内部**无需手动操作** MineKitCommand，只需在 `getSubCommands()` 中返回命令列表即可

## 模块架构

```
MineKitPlugin (主类)
  └── FeatureManager (功能管理器)
       └── Feature (接口)
            ├── RespawnFeature (复活属性给予)
            ├── AntiTrampleFeature (农田防踩踏)
            ├── PvpFeature (PvP 检测执行命令)
            ├── KillStatsFeature (击杀统计)
            └── DeathBackFeature (死亡回溯)
```

### Feature 生命周期
1. `loadConfig(config)` — 从 config.yml 读取配置
2. `onEnable(plugin)` — 注册 Listener、创建子命令对象
3. **FeatureManager 自动**：onEnable 后注册子命令到 MineKitCommand，onDisable 前从 MineKitCommand 注销
4. `onDisable(plugin)` — 注销 Listener、取消任务
5. `isEnabled()` — 返回 features.<name>.enabled 状态

### 核心原则
- **关闭 = 零开销**：功能禁用时不注册 Listener、不注册指令、无定时任务
- **动态开关**：`/minekit feature <name> on|off` 运行时启停
- **可扩展**：新增功能 = 新建 Feature 实现类 + config.yml 加一段，不修改主类
- **默认关闭**：所有功能 `启用: false`，由服主按需开启

### 标准权限组结构
每个需要玩家分级的模块必须包含三个组：
1. **default** — 无条件兜底，新功能默认不影响 default 组
2. **独立OP组** — OP 玩家跳过权限匹配，新功能默认不影响 OP 组
3. **vip** — 示例配置写在此组下
- **例外**：除非专门说明，新功能不得修改 default 和 OP 组的配置行为
