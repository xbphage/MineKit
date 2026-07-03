# Xbp Plugin — 总体设计

## 概述

Paper/Spigot 1.16.5 模块化插件。所有功能以 Feature 模块形式注册，可独立开关，关闭时零性能开销。
主命令 `/xbp` 为唯一指令入口，所有功能子命令均注册为其二级子命令。

---

## 一、架构总览

```
┌──────────────────────────────────────────────┐
│              XbpPlugin                       │  主入口 (继承 JavaPlugin)
│  ┌──────────────────────────────────────────┐│
│  │         FeatureManager                  ││  功能管理器
│  │  注册 · 开关 · 重载 · 生命周期管理      ││
│  └──────────────┬──────────────────────────┘│
│                 │                            │
│  ┌──────────────▼──────────────────────────┐│
│  │          Feature 接口                   ││
│  │  getName() / isEnabled() / onEnable()   ││
│  │  onDisable() / loadConfig()             ││
│  │  getSubCommands()                       ││
│  └──────────────┬──────────────────────────┘│
│                 │                            │
│  ┌──────────────▼──────────────────────────┐│
│  │    RespawnFeature   (features/respawn/) ││  首个功能
│  │    FeatureX          (features/xxx/)    ││  后续扩展
│  └─────────────────────────────────────────┘│
│                                              │
│  ┌──────────────────────────────────────────┐│
│  │         XbpCommand                      ││  主指令分发器
│  │  内建子命令 + Feature 注册的子命令       ││
│  └──────────────────────────────────────────┘│
└──────────────────────────────────────────────┘
```

---

## 二、Feature 接口规范

每个功能模块实现此接口：

| 方法 | 说明 |
|------|------|
| `getName()` | 唯一标识，如 `"respawn"` |
| `getDescription()` | 人类可读描述 |
| `isEnabled()` | 由 `features.<name>.enabled` 控制 |
| `onEnable(JavaPlugin)` | 注册 Listener、注册子命令 |
| `onDisable(JavaPlugin)` | 注销 Listener、取消任务、移除子命令 |
| `loadConfig(FileConfiguration)` | 从 config.yml 读取本模块配置段 |
| `getSubCommands()` | 返回 List<SubCommand>，注册到 `/xbp` |

### SubCommand 接口

```java
public interface SubCommand {
    String getName();
    String getPermission();  // null = 无需额外权限
    boolean execute(CommandSender sender, String[] args);
    List<String> tabComplete(CommandSender sender, String[] args);
}
```

---

## 三、指令体系

### 唯一主命令：`/xbp`

内建子命令（FeatureManager 提供）：

```
/xbp reload                   — 重载所有配置
/xbp feature list             — 列出所有功能及开关状态
/xbp feature <name> on        — 启用指定功能（热插拔）
/xbp feature <name> off       — 禁用指定功能（热插拔）
```

功能模块通过 `Feature.getSubCommands()` 注册自己的子命令。

**规则：永不新增顶层指令。所有功能指令都是 `/xbp` 的二级子命令。**

---

## 四、配置结构 (config.yml)

```yaml
features:
  <feature-name>:
    enabled: true     # false = 关闭，零性能开销

# 每个功能各自的配置段
<feature-name>:
  ...
```

---

## 五、FeatureManager 生命周期

```
服务器启动
    ↓
XbpPlugin.onEnable()
    ↓
读取 config.yml，解析 features 段
    ↓
注册所有 Feature（register）
    ↓
遍历 features: enabled: true 的模块 → onEnable()
    ├── 注册 Listener (PluginManager.registerEvents)
    ├── 注册子命令 (XbpCommand.addSubCommand)
    └── 启动必要定时任务
    ↓
运行时 /xbp feature xxx off → onDisable()
    ├── Handler.unregister(监听器)
    ├── XbpCommand.removeSubCommand
    └── BukkitScheduler.cancelTask
    ↓
运行时 /xbp feature xxx on  → onEnable()
    ↓
运行时 /xbp reload → 全流程重走
    ↓
服务器关闭
    ↓
XbpPlugin.onDisable() → 遍历所有 Feature 执行 onDisable()
```

---

## 六、扩展新功能步骤

1. 创建包 `features/<功能名>/`
2. 实现 `Feature` 接口（按需实现 `SubCommand`）
3. 在 `config.yml` 的 `features:` 下添加开关
4. 在 `config.yml` 中添加对应的配置段
5. 在 `XbpPlugin` 中注册新 Feature

不修改 FeatureManager、不修改 XbpCommand 核心逻辑。

---

## 七、性能原则

| 情况 | 行为 |
|------|------|
| 功能关闭 | Listener 不注册，事件零触发 |
| 运行时禁用 | 立即注销 Listener，取消任务，移除子命令 |
| 配置重载 | 全部 disable → 重读配置 → enable 开启项 |
| 配置损坏 | 打印错误栈，保持当前配置不变 |
