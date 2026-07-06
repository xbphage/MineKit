# 赎罪系统（Redemption）实现计划

## 一、概念

玩家通过消耗物品永久降低「有效死亡次数」，从而减轻重生惩罚。赎罪次数记入数据库，跨会话持久。

## 二、数据流

```
死亡记录表 death_records (已有)
    ↓
查 60 分钟内死亡次数 = 3
    ↓
查 player_data.redemption_offset = 1（玩家之前赎罪的累计）
    ↓
有效次数 = 3 - 1 = 2
    ↓
查基数表 → tier.2
```

## 三、数据库

`records.db` 新增表：

```sql
CREATE TABLE player_data (
    uuid TEXT PRIMARY KEY,
    redemption_offset INTEGER DEFAULT 0
);
```

## 四、中文物品名

下载 Minecraft 1.16.5 官方 `zh_cn.json` → `assets/minecraft/lang/zh_cn.json`，插件启动时加载。

例如 `item.minecraft.diamond` → `钻石`，`item.minecraft.netherite_ingot` → `下界合金锭`。

## 五、配置文件

```yaml
redemption:
  物品:
    - 物品: "minecraft:diamond"
      数量: 1
      降低: 1
    - 物品: "minecraft:netherite_ingot"
      数量: 1
      降低: 3
```

## 六、玩家交互流程

```
玩家复活
    ↓
RespawnListener 正常计算惩罚并应用
    ↓
RedemptionListener 发送一行可点击文本：
  "§e⚖ 点击赎罪 §7(当前惩罚：死亡 X 次)"
    ↓
玩家点击 → 显示赎罪面板：
  ┌─────────────────────────────────┐
  │ §6【赎罪】                      │
  │ §7当前: 60分钟内死亡 §e3 §7次   │
  │ §7赎罪偏移: §a-1                │
  │ §7有效: §e2 §7次                 │
  │                                 │
  │ §6惩罚细则:                     │
  │ §7  0次: 20❤ 20饱食 满效果     │
  │ §7  3次: 20❤ 20饱食 无效果     │
  │ §7  5次: 18❤ 18饱食 nausea...  │
  │ ...                             │
  │                                 │
  │ §6可赎罪物品:                   │
  │ [✔ 钻石 x1 → 减1级] 你有 32个   │
  │ [✘ 下界合金锭 x1 → 减3级] 无   │
  └─────────────────────────────────┘
    ↓
玩家点击赎罪按钮
    ↓
消耗物品 → REDEMPTION_OFFSET +N
    ↓
重新查询有效次数 → 重新应用惩罚
```

## 七、需新增文件

| 文件 | 说明 |
|------|------|
| `redemption/RedemptionFeature.java` | Feature |
| `redemption/listener/RedemptionListener.java` | 发送赎罪提示 + 面板 |
| `redemption/config/RedemptionConfig.java` | 物品配置加载 |
| `lang/ItemTranslator.java` | 加载 zh_cn.json 翻译物品名 |
| `src/main/resources/lang/zh_cn.json` | 1.16.5 中文翻译文件 |

## 八、需修改文件

| 文件 | 改动 |
|------|------|
| `RecordManager.java` | 新增 player_data 表 + redemptionOffset 读写 |
| `RespawnConfig.java` | 无改动（有效次数已在外部算好传入） |
| `config.yml` | 加 `redemption` 配置段 |
| `MineKitPlugin.java` | 注册 RedemptionFeature |
