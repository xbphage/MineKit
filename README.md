# MineKit 🧩

**Minecraft Spigot/Paper 1.16+ 模块化功能插件**

每个功能独立开关，关闭时零性能开销。内建可视化 `/minekit` 管理面板，全部配置使用中文 YAML 键。

---

## 功能一览

| 功能 | 说明 | 配置段 |
|------|------|--------|
| ⚰️ **重生频率惩罚** | 按 60 分钟内死亡次数递减血量、饱食度、施加效果，权限组支持偏移量 | `respawn` |
| 🪙 **赎罪** | 消耗物品降低有效死亡次数，减轻重生惩罚（60 分钟滚动窗口） | `redemption` |
| 🌾 **农田防踩踏** | 阻止耕地因踩踏退化为泥土 | `antitrample` |
| ⚔️ **PvP 检测执行命令** | 玩家攻击玩家时根据双方权限执行自定义命令，支持 PlaceholderAPI | `pvp` |
| 📊 **击杀统计** | 击杀/死亡排行、击杀明细、死亡日志（默认启用） | — |
| 🔙 **死亡回溯** | 死亡后点击聊天栏按钮传送回死亡点 | `deathback` |
| 🎁 **物品换经验** | 手持物品一键兑换经验等级，支持自定义配方和禁止存放 | `itemtoexp` |
| 📢 **命令通报** | 非管理员执行命令时自动通报给在线管理员 | `report` |
| 👑 **/sudo** | 以玩家身份执行命令，输出归调用者 | 独立命令 |

---

## 快速开始

1. 将 `MineKit-*.jar` 放入 `plugins/` 目录
2. 启动服务器，生成默认 `config.yml`
3. 编辑 `config.yml`，将需要的功能 `启用: false` 改为 `启用: true`
4. 执行 `/minekit reload` 重载配置
5. 或在游戏内输入 `/minekit` 打开可视化面板一键开关

> 击杀统计默认启用，无需配置。配置版本自动升级，新增功能配置段会自动补入，不覆盖已有设置。

---

## 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/minekit` | 打开管理面板 | `minekit.admin` |
| `/minekit reload` | 重载所有配置 | `minekit.admin` |
| `/minekit feature list` | 列出功能状态 | `minekit.admin` |
| `/minekit feature <name> on\|off` | 开关功能 | `minekit.admin` |
| `/minekit exchange` | 查看可兑换的配方列表 | `minekit.itemtoexp` |
| `/minekit exchange <配方名>` | 兑换指定配方的经验等级 | `minekit.itemtoexp` |
| `/minekit kills` | 查看自己战绩 | `minekit.killstats` |
| `/minekit kills top` | 击杀排行 | `minekit.killstats` |
| `/minekit kills deaths` | 死亡排行 | `minekit.killstats` |
| `/minekit kills log <玩家>` | 查看死亡日志 | `minekit.killstats` |
| `/minekit back` | 传送回死亡点 | 无 |
| `/sudo <玩家> <命令>` | 以玩家身份执行命令 | `minekit.sudo` |

---

## PlaceholderAPI 占位符

安装 PlaceholderAPI 后自动注册：

| 占位符 | 说明 |
|--------|------|
| `%minekit_deaths_hour%` | 60 分钟内死亡次数（滚动窗口） |
| `%minekit_deaths_total%` | 总死亡次数 |
| `%minekit_kills_hour%` | 60 分钟内击杀数 |
| `%minekit_kills_total%` | 总击杀数 |

> 仅统计非 PvP 死亡（被玩家杀死不计入频率惩罚和占位符）。

---

## 权限

| 节点 | 说明 | 默认 |
|------|------|------|
| `minekit.admin` | 管理面板、重载、开关功能 | OP |
| `minekit.sudo` | 使用 `/sudo` 命令 | OP |
| `minekit.killstats` | 查看击杀统计 | 所有人 |
| `minekit.itemtoexp` | 查看/使用物品换经验 | 所有人 |
| `minekit.itemtoexp.bypass` | 绕过禁止存放限制 | OP |

---

## 配置示例

```yaml
features:
  respawn:
    启用: true         # 开启重生频率惩罚

respawn:
  频率惩罚基数:
    一小时:
      0:
        血量: 20.0
        饱食度: 20
        效果:
          - ["speed", "60", "1"]
          - ["haste", "60", "1"]
        消息: "你重生了，身体回到了巅峰时期"
      3:
        血量: 20.0
        饱食度: 20
        效果: []
        消息: "你重生了"
      5:
        血量: 18.0
        饱食度: 18
        效果:
          - ["nausea", "2", "1"]
          - ["weakness", "7", "1"]
          - ["mining_fatigue", "7", "1"]
        消息: "你连续重生，感到有点疲惫"

redemption:
  物品:
    - 物品: "minecraft:diamond"
      数量: 3
      降低: 5

itemtoexp:
  禁止存放:
    - "minecraft:milk_bucket"
  交换配方:
    diamond:
      物品: "minecraft:diamond"
      数量: 1
      经验等级: 5
    emerald:
      物品: "minecraft:emerald"
      数量: 3
      经验等级: 1
```

## 物品换经验交互

输入 `/minekit exchange` 查看所有可用配方，点击配方即可消耗物品兑换经验等级。

## 禁止存放

在 `itemtoexp.禁止存放` 中配置的物品无法存入任何容器（箱子、末影箱、潜影盒、漏斗、发射器等），也无法被漏斗/漏斗矿车转移。玩家持有 `minekit.itemtoexp.bypass` 权限可绕过限制。

## 赎罪交互

死亡后自动收到一行提示，点击可打开赎罪面板：

```
⚖ 赎罪 - 点击查看详细
```

赎罪面板显示当前死亡/赎罪/有效次数、惩罚细则表（前一次/当前/下一次三档）、可用物品列表。点击物品按钮消耗物品降低有效死亡次数，惩罚立即重新计算。

---

## 数据存储

- `plugins/MineKit/records.db` — SQLite 中央数据库（死亡记录 + 赎罪记录 + 击杀数据）

---

## 构建

```bash
mvn clean package
```

产物：`target/MineKit-*.jar`

每次编译版本号自动递增（1.0.0.1 → 1.0.0.2 → ...）。

---

## 技术栈

- **API**: Paper/Spigot 1.16+
- **构建**: Maven 3, Java 1.8
- **数据库**: SQLite (via sqlite-jdbc)
- **占位符**: PlaceholderAPI 2.11+
