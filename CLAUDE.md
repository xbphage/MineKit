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


### 编译警告处理规则
以下警告在 Java 8 + Paper 1.16.5 环境中不可避免或已确认安全，**不要在代码中尝试消除它们**：

| 警告 | 原因 | 处理方式 |
|------|------|---------|
| `getCommand("minekit")` 等 NPE | IDE 无法推断 plugin.yml 已注册 | `@SuppressWarnings("all")` 加类级别 |
| `player.spigot().sendMessage(BaseComponent)` 已过时 | Paper API 标注过时但 Component API 不完善 | **保留 + `@SuppressWarnings("deprecation")`** |
| `getMaxHealth()` 已过时 | 改用 `getAttribute(GENERIC_MAX_HEALTH).getValue()` | 替代写法，注意 Attribute import |
| `addPotionEffect(Effect, boolean)` 已过时 | Paper API 标注，用 `removePotionEffect` + `addPotionEffect(Effect)` | 已全局替代 |
| `getDisplayName()` 已过时 | Paper 1.16.5 Component API 不完善 | **保留 + `@SuppressWarnings("deprecation")`** |
| `@NotNull` 注解缺失 | TabExecutor 接口要求 `@NotNull` | 加 `org.jetbrains.annotations.NotNull` import |
| 效果配置混写 `["name", 60, 1]` | YAML 字符串+数字混写 IDE 警告 | **全部写为字符串** `["name", "60", "1"]` |
| 未使用的 import/field | 代码清理遗漏 | 及时删除，或加 `@SuppressWarnings("unused")` |
| 方法返回值未使用 | 调用方不需要返回值 | 加 `@SuppressWarnings("unused")` |
| 空白行 | Javadoc/代码格式 | 删除多余空行 |
| Paper API 传递依赖 CVE | guava、gson、snakeyaml 来自 Paper 本身 | **不可修复，忽略** |
| 局部变量冗余 | 简化后未合并 | 直接用原始值替代 |
