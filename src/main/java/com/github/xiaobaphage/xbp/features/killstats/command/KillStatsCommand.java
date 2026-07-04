package com.github.xiaobaphage.xbp.features.killstats.command;

import com.github.xiaobaphage.xbp.core.SubCommand;
import com.github.xiaobaphage.xbp.features.killstats.database.KillStatsDatabase;
import com.github.xiaobaphage.xbp.features.killstats.database.KillStatsDatabase.KillDetail;
import com.github.xiaobaphage.xbp.features.killstats.database.KillStatsDatabase.PlayerData;
import com.github.xiaobaphage.xbp.features.killstats.database.KillStatsDatabase.RankEntry;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class KillStatsCommand implements SubCommand {

    private static final String PERM = "xbp.killstats";
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    private final KillStatsDatabase db;

    public KillStatsCommand(KillStatsDatabase db) {
        this.db = db;
    }

    @Override
    public String getName() { return "kills"; }

    @Override
    public String getPermission() { return PERM; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            // 查看自己的战绩
            if (!(sender instanceof Player)) {
                sender.sendMessage(color("&c用法: /xbp kills <玩家名>"));
                return true;
            }
            showPlayerStats(sender, (Player) sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if ("top".equals(sub)) {
            showTopKills(sender);
            return true;
        }
        if ("deaths".equals(sub)) {
            showTopDeaths(sender);
            return true;
        }
        if ("detail".equals(sub) && args.length >= 2) {
            showKillDetail(sender, args[1]);
            return true;
        }

        // 查看指定玩家
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null) {
            showPlayerStats(sender, target);
        } else {
            try {
                UUID uuid = UUID.fromString(args[0]);
                PlayerData data = db.getPlayerData(uuid);
                showPlayerStatsRaw(sender, args[0], data);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(color("&c玩家 '" + args[0] + "' 不在线"));
            }
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("top", "deaths", "detail").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /* ======== 战绩展示 ======== */

    private void showPlayerStats(CommandSender sender, Player player) {
        PlayerData data = db.getPlayerData(player.getUniqueId());
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l" + player.getName() + " &7- 战绩"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&e  🗡 击杀: &f" + data.getKills()));
        sender.sendMessage(color("&e  💀 死亡: &f" + data.getDeaths()));
        sender.sendMessage(color("&e  📊 K/D: &f" + DF.format(data.getKd())));
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private void showPlayerStatsRaw(CommandSender sender, String name, PlayerData data) {
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l" + name + " &7- 战绩"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&e  🗡 击杀: &f" + data.getKills()));
        sender.sendMessage(color("&e  💀 死亡: &f" + data.getDeaths()));
        sender.sendMessage(color("&e  📊 K/D: &f" + DF.format(data.getKd())));
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    /* ======== 排行榜 ======== */

    private void showTopKills(CommandSender sender) {
        List<RankEntry> top = db.getKillsTop(10);
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l🏆 击杀排行 Top 10"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));

        if (top.isEmpty()) {
            sender.sendMessage(color("&7  暂无数据"));
            sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
            return;
        }

        if (sender instanceof Player) {
            sendClickableTop((Player) sender, top);
        } else {
            for (RankEntry e : top) {
                sender.sendMessage(color(String.format("&7  #%d &f%s &7- &e%d 击杀", e.getRank(), e.getPlayerName(), e.getValue())));
            }
        }
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private void showTopDeaths(CommandSender sender) {
        List<RankEntry> top = db.getDeathsTop(10);
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l💀 死亡排行 Top 10"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));

        if (top.isEmpty()) {
            sender.sendMessage(color("&7  暂无数据"));
            sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
            return;
        }

        if (sender instanceof Player) {
            sendClickableTop((Player) sender, top);
        } else {
            for (RankEntry e : top) {
                sender.sendMessage(color(String.format("&7  #%d &f%s &7- &e%d 次死亡", e.getRank(), e.getPlayerName(), e.getValue())));
            }
        }
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    /** 排行榜点击交互 */
    private void sendClickableTop(Player player, List<RankEntry> list) {
        for (RankEntry e : list) {
            TextComponent nameComp = new TextComponent(color(String.format("  #%d &f%s", e.getRank(), e.getPlayerName())));
            nameComp.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/xbp kills detail " + e.getUuid()
            ));
            nameComp.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new Text("§e点击查看 " + e.getPlayerName() + " 杀了谁")
            ));
            TextComponent valueComp = new TextComponent(color(" &7- &e" + e.getValue()));
            player.spigot().sendMessage(nameComp, valueComp);
        }
    }

    /* ======== 击杀明细 ======== */

    private void showKillDetail(CommandSender sender, String uuidStr) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(color("&c无效的 UUID"));
            return;
        }

        PlayerData data = db.getPlayerData(uuid);
        List<KillDetail> details = db.getKillDetails(uuid);
        String name = data.getName() != null ? data.getName() : uuidStr.substring(0, 8) + "...";

        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l" + name + " &7- 击杀明细"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));

        if (details.isEmpty()) {
            sender.sendMessage(color("&7  暂无击杀记录"));
        } else {
            for (KillDetail d : details) {
                String vName = d.getVictimName() != null ? d.getVictimName() : d.getVictimUuid().substring(0, 8);
                sender.sendMessage(color(String.format("&7  ✘ &f%s &7- &c%d 次", vName, d.getCount())));
            }
        }
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
