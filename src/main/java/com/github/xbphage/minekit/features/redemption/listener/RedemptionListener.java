package com.github.xbphage.minekit.features.redemption.listener;

import com.github.xbphage.minekit.features.redemption.config.RedemptionConfig;
import com.github.xbphage.minekit.features.redemption.config.RedemptionConfig.RedemptionItem;
import com.github.xbphage.minekit.features.respawn.config.EffectData;
import com.github.xbphage.minekit.features.respawn.config.FrequencyTier;
import com.github.xbphage.minekit.lang.EffectNames;
import com.github.xbphage.minekit.lang.ItemTranslator;
import com.github.xbphage.minekit.records.RecordManager;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.attribute.Attribute;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

@SuppressWarnings({"deprecation"})
public class RedemptionListener implements Listener {

    private final JavaPlugin plugin;
    private final RecordManager records;
    private final RedemptionConfig config;
    private final ItemTranslator translator;
    private final List<FrequencyTier> penaltyTiers;

    public RedemptionListener(JavaPlugin plugin, RecordManager records, RedemptionConfig config,
                              ItemTranslator translator, List<FrequencyTier> penaltyTiers) {
        this.plugin = plugin;
        this.records = records;
        this.config = config;
        this.translator = translator;
        this.penaltyTiers = penaltyTiers;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            String uuid = player.getUniqueId().toString();
            int deaths = records.getDeathsLastHour(uuid);
            int redeemed = records.getRedemptionAmount(uuid);
            TextComponent prompt = new TextComponent(color("&6⚖ &e赎罪 &7- 点击查看详细"));
            prompt.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/minekit redemption " + deaths + " " + redeemed));
            prompt.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text("§e点击查看赎罪详情\n§7当前: " + deaths + "次死亡")));
            player.spigot().sendMessage(prompt);
        }, 3L);
    }

    public void sendRedemptionPanel(Player player, int deaths, int redeemed) {
        int effective = Math.max(0, deaths - redeemed);

        player.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        player.sendMessage(color("&6  &l赎罪 &7- 消耗物品降低死亡惩罚"));
        player.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        player.sendMessage(color("&7  ✟死亡: &e" + deaths + " &7| ✞赎罪: &a" + redeemed
                + " &7| ➤有效: &e" + effective));
        player.sendMessage(color("&b  [i] 死亡与赎罪均以60分钟为窗口滚动计算，过时自动失效"));

        FrequencyTier current = findTier(effective);
        player.sendMessage(color("&7  当前档位: 血量 &f" + (int)current.getHealth()
                + " &7饱食度 &f" + current.getFood()));

        player.sendMessage("");
        player.sendMessage(color("&6  &l惩罚细则"));
        int currentThreshold = findThreshold(effective);
        int currentIdx = -1;
        for (int i = 0; i < penaltyTiers.size(); i++) {
            if (penaltyTiers.get(i).getThreshold() == currentThreshold) { currentIdx = i; break; }
        }
        int[] showIdx;
        if (currentIdx < 0) showIdx = new int[0];
        else if (penaltyTiers.size() <= 3) {
            showIdx = new int[penaltyTiers.size()];
            for (int i = 0; i < penaltyTiers.size(); i++) showIdx[i] = i;
        } else {
            int s = Math.max(0, currentIdx - 1);
            int e = Math.min(penaltyTiers.size() - 1, currentIdx + 1);
            showIdx = new int[e - s + 1];
            for (int i = s; i <= e; i++) showIdx[i - s] = i;
        }
        for (int idx : showIdx) {
            FrequencyTier t = penaltyTiers.get(idx);
            boolean isCurrent = idx == currentIdx;
            String c = isCurrent ? "&a" : (idx < currentIdx ? "&7" : "&e");
            StringBuilder sb = new StringBuilder();
            sb.append(c).append("  ").append(t.getThreshold()).append("次: 血量&f").append((int)t.getHealth());
            sb.append(" ").append(c).append("饱食度&f").append(t.getFood());
            if (t.getEffects().isEmpty()) {
                sb.append(" ").append(c).append("效果: &8无");
                if (isCurrent) sb.append(" &a◀ 当前");
                player.sendMessage(color(sb.toString()));
            } else {
                sb.append(" ").append(c).append("效果:");
                if (isCurrent) sb.append(" &a◀ 当前");
                player.sendMessage(color(sb.toString()));
                for (EffectData ef : t.getEffects()) {
                    player.sendMessage(color("&8    &f" + EffectNames.cn(ef.getType())
                            + " &7" + (ef.getDurationTicks()/20) + "s &7Lv" + (ef.getAmplifier()+1)));
                }
            }
        }

        player.sendMessage("");
        player.sendMessage(color("&6  &l赎罪物品"));
        for (RedemptionItem item : config.getItems()) {
            int has = countItem(player, item.getMaterial());
            boolean enough = has >= item.getAmount();
            String icon = enough ? "&a✔" : "&c✘";
            String cnName = translator.get(item.getMaterial());
            String suffix = enough ? " &7你有 &a" + has + " &7个" : " &7(不足)";
            TextComponent btn = new TextComponent(color(icon + " &f" + cnName + " &7x" + item.getAmount()
                    + " → 降低 &b" + item.getReduce() + " &7级" + suffix));
            if (enough) {
                btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/minekit redeem " + item.getMaterial() + " " + item.getAmount() + " " + item.getReduce()));
                btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text("§a点击赎罪\n§7消耗 " + cnName + " x" + item.getAmount() + " 降低 " + item.getReduce() + " 级")));
            } else {
                btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text("§c物品不足\n§7需要 " + cnName + " x" + item.getAmount() + " ，当前拥有 " + has + " 个")));
            }
            player.spigot().sendMessage(btn);
        }
        player.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    public boolean redeem(Player player, String material, int amount, int reduce) {
        if (!removeItems(player, material, amount)) return false;
        records.recordRedemption(player.getUniqueId().toString(), player.getName(), reduce);
        int deaths = records.getDeathsLastHour(player.getUniqueId().toString());
        int redeemed = records.getRedemptionAmount(player.getUniqueId().toString());
        int effective = Math.max(0, deaths - redeemed);
        FrequencyTier tier = findTier(effective);
        if (tier.getHealth() > 0) player.setHealth(Math.min(tier.getHealth(), java.util.Optional.ofNullable(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).map(a -> a.getValue()).orElse(20.0)));
        player.setFoodLevel(tier.getFood());
        for (EffectData ef : tier.getEffects()) {
            PotionEffectType type = PotionEffectType.getByName(ef.getType());
            if (type != null) {
                player.removePotionEffect(type);
                player.addPotionEffect(new PotionEffect(type, ef.getDurationTicks(), ef.getAmplifier()));
            }
        }
        player.sendMessage(color("&a✔ 赎罪成功！有效次数从 &e" + (effective + reduce) + " &a降至 &e" + effective));
        sendRedemptionPanel(player, deaths, redeemed);
        return true;
    }

    private int countItem(Player player, String material) {
        Material mat = Material.getMaterial(material.replace("minecraft:", "").toUpperCase());
        if (mat == null) return 0;
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) count += item.getAmount();
        }
        return count;
    }

    private boolean removeItems(Player player, String material, int amount) {
        Material mat = Material.getMaterial(material.replace("minecraft:", "").toUpperCase());
        if (mat == null) return false;
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != mat) continue;
            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);
            remaining -= take;
        }
        return remaining <= 0;
    }

    private FrequencyTier findTier(int deaths) {
        FrequencyTier best = penaltyTiers.get(0);
        for (FrequencyTier t : penaltyTiers) {
            if (deaths >= t.getThreshold()) best = t;
        }
        return best;
    }

    private int findThreshold(int deaths) {
        int best = 0;
        for (FrequencyTier t : penaltyTiers) {
            if (deaths >= t.getThreshold()) best = t.getThreshold();
        }
        return best;
    }

    static String color(String msg) { return ChatColor.translateAlternateColorCodes('&', msg); }
}
