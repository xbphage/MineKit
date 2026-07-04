# Xbp Plugin — AI 开发指引

## 项目概览
基于 Paper/Spigot 1.16.5 的模块化插件，Maven 构建（Java 1.8）。

## 指令体系

**主命令唯一入口：`/xbp`**

所有功能模块的指令均为 `/xbp` 的二级子命令（SubCommand），绝不新增顶层指令。

```
/xbp reload                  — 重载所有配置
/xbp feature list            — 列出所有功能及开关状态
/xbp feature <name> on       — 启用指定功能
/xbp feature <name> off      — 禁用指定功能
```

### 新增功能模块时的指令注册规则
- 功能模块实现 `SubCommand` 接口
- 在 `Feature.getSubCommands()` 中返回
- `XbpCommand` 自动注册到 `/xbp` 下

## 模块架构

```
XbpPlugin (主类)
  └── FeatureManager (功能管理器)
       └── Feature (接口)
            ├── RespawnFeature (复活属性给予)
            └── ... (后续扩展)
```

### Feature 生命周期
1. `loadConfig(config)` — 从 config.yml 读取配置
2. `onEnable(plugin)` — 注册 Listener、注册子命令到 XbpCommand
3. `onDisable(plugin)` — 注销 Listener、取消任务、移除子命令
4. `isEnabled()` — 返回 features.<name>.enabled 状态

### 核心原则
- **关闭 = 零开销**：功能禁用时不注册 Listener、不注册指令、无定时任务
- **动态开关**：`/xbp feature <name> on|off` 运行时启停
- **可扩展**：新增功能 = 新建 Feature 实现类 + config.yml 加一段，不修改主类
- **默认关闭**：所有功能 `启用: false`，由服主按需开启

### 标准权限组结构
每个需要玩家分级的模块必须包含三个组：
1. **default** — 无条件兜底，新功能默认不影响 default 组
2. **独立OP组** — OP 玩家跳过权限匹配，新功能默认不影响 OP 组
3. **vip** — 示例配置写在此组下
- **例外**：除非专门说明，新功能不得修改 default 和 OP 组的配置行为
