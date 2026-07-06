package com.github.xbphage.minekit.features.killstats.command;

import com.github.xbphage.minekit.core.SubCommand;
import com.github.xbphage.minekit.records.RecordManager;
import com.github.xbphage.minekit.records.RecordManager.DetailEntry;
import com.github.xbphage.minekit.records.RecordManager.DeathRecord;
import com.github.xbphage.minekit.records.RecordManager.RankEntry;
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

    private static final String PERM = "minekit.killstats";
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    private final RecordManager db;

    public KillStatsCommand(RecordManager db) {
        this.db = db;
    }

    @Override
    public String getName() { return "kills"; }
    @Override
    public String getPermission() { return PERM; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(color("&c用法: /xbp kills <玩家名>")); return true; }
            showPlayerStats(sender, (Player) sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("top".equals(sub)) { showTopKills(sender); return true; }
        if ("deaths".equals(sub)) { showTopDeaths(sender); return true; }
        if ("detail".equals(sub) && args.length >= 2) { showKillDetail(sender, args[1]); return true; }
        if ("log".equals(sub) && args.length >= 2) { showDeathLog(sender, args[1]); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target != null) showPlayerStats(sender, target);
        else sender.sendMessage(color("&c玩家 '" + args[0] + "' 不在线"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) return Arrays.asList("top", "deaths", "detail", "log").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }

    private void showPlayerStats(CommandSender sender, Player player) {
        String uuid = player.getUniqueId().toString();
        int kills = db.getTotalKills(uuid);
        int deaths = db.getTotalDeaths(uuid);
        int todayKills = db.getKillsToday(uuid);
        int todayDeaths = db.getDeathsToday(uuid);
        double kd = deaths == 0 ? (double) kills : (double) kills / deaths;
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l" + player.getName() + " &7- 战绩"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&e  🗡 总击杀: &f" + kills + color(" &7(今日 &e" + todayKills + "&7)")));
        sender.sendMessage(color("&e  💀 总死亡: &f" + deaths + color(" &7(今日 &e" + todayDeaths + "&7)")));
        sender.sendMessage(color("&e  📊 K/D: &f" + DF.format(kd)));
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private void showTopKills(CommandSender sender) {
        List<RankEntry> top = db.getKillsTop(10);
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l🏆 击杀排行 Top 10"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        if (top.isEmpty()) { sender.sendMessage(color("&7  暂无数据")); sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛")); return; }
        if (sender instanceof Player) sendClickableTop((Player) sender, top, "击杀");
        else for (RankEntry e : top) sender.sendMessage(color(String.format("&7  #%d &f%s &7- &e%d 击杀", e.getRank(), e.getName(), e.getCount())));
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private void showTopDeaths(CommandSender sender) {
        List<RankEntry> top = db.getDeathsTop(10);
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l💀 死亡排行 Top 10"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        if (top.isEmpty()) { sender.sendMessage(color("&7  暂无数据")); sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛")); return; }
        if (sender instanceof Player) sendClickableTop((Player) sender, top, "次死亡");
        else for (RankEntry e : top) sender.sendMessage(color(String.format("&7  #%d &f%s &7- &e%d 次死亡", e.getRank(), e.getName(), e.getCount())));
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private void sendClickableTop(Player player, List<RankEntry> list, String suffix) {
        for (RankEntry e : list) {
            TextComponent nameComp = new TextComponent(color(String.format("  #%d &f%s", e.getRank(), e.getName())));
            nameComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minekit kills detail " + e.getUuid()));
            nameComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§e点击查看 " + e.getName() + " 杀了谁")));
            TextComponent valueComp = new TextComponent(color(" &7- &e" + e.getCount()));
            player.spigot().sendMessage(nameComp, valueComp);
        }
    }

    private void showKillDetail(CommandSender sender, String uuidStr) {
        UUID uuid;
        try { uuid = UUID.fromString(uuidStr); } catch (IllegalArgumentException e) { sender.sendMessage(color("&c无效的 UUID")); return; }
        List<DetailEntry> details = db.getKillDetails(uuid.toString());
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l击杀明细 &7- 共 &e" + db.getTotalKills(uuid.toString()) + " &7次击杀"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        if (details.isEmpty()) sender.sendMessage(color("&7  暂无击杀记录"));
        else for (DetailEntry d : details) sender.sendMessage(color(String.format("&7  ✘ &f%s &7- &c%d 次", d.getName() != null ? d.getName() : d.getUuid().substring(0, 8), d.getCount())));
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private void showDeathLog(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) { sender.sendMessage(color("&c玩家 '" + name + "' 不在线")); return; }
        List<DeathRecord> records = db.getRecentDeaths(target.getUniqueId().toString(), 10);
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        sender.sendMessage(color("&6  &l" + target.getName() + " &7- 死亡日志"));
        sender.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        if (records.isEmpty()) sender.sendMessage(color("&7  无死亡记录"));
        else for (DeathRecord r : records) {
            String killer = r.getKillerName() != null ? " &c← " + r.getKillerName() : "";
            sender.sendMessage(color("&8  [#] &f" + r.getTime() + " &7" + r.getWorld() + killer));
            sender.sendMessage(color("&8      &7" + r.getReason()));
        }
        sender.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private static String color(String msg) { return ChatColor.translateAlternateColorCodes('&', msg); }
}
