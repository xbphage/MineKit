package com.github.xbphage.minekit.features.itemtoexp.command;

import com.github.xbphage.minekit.core.SubCommand;
import com.github.xbphage.minekit.features.itemtoexp.config.ItemToExpConfig;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class ItemToExpCommand implements SubCommand {

    private static final String PERM = "minekit.itemtoexp";
    private final ItemToExpConfig config;

    public ItemToExpCommand(ItemToExpConfig config) { this.config = config; }

    @Override public String getName() { return "exchange"; }
    @Override public String getPermission() { return PERM; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&c只有玩家可以执行此命令"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) { showRecipes(player); return true; }
        exchange(player, args[0]);
        return true;
    }

    private void showRecipes(Player player) {
        List<ItemToExpConfig.ExchangeRecipe> recipes = config.getRecipes();
        if (recipes.isEmpty()) {
            player.sendMessage(color("&c当前没有可用的交换配方"));
            return;
        }
        player.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        player.sendMessage(color("&6  &l物品换经验"));
        player.sendMessage(color("&8  &m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
        for (ItemToExpConfig.ExchangeRecipe r : recipes) {
            TextComponent msg = new TextComponent(color(String.format(
                    " &e%s &7×%d &f→ &a%d级经验",
                    matName(r.getMaterial()), r.getAmount(), r.getExpLevels())));
            msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/minekit exchange " + r.getName()));
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text("§a点击兑换\n§7需要: §f" + matName(r.getMaterial()) + " §7×" + r.getAmount())));
            player.spigot().sendMessage(msg);
        }
        player.sendMessage(color("&7点击上方配方即可兑换"));
        player.sendMessage(color("&8&m⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛⬛"));
    }

    private void exchange(Player p, String name) {
        ItemToExpConfig.ExchangeRecipe r = config.getRecipe(name);
        if (r == null) {
            p.sendMessage(color("&c未找到配方: " + name));
            p.sendMessage(color("&7可用配方: /minekit exchange"));
            return;
        }
        Material mat = parseMat(r.getMaterial());
        if (mat == null) {
            p.sendMessage(color("&c配置错误: 未知物品 " + r.getMaterial()));
            return;
        }
        int total = 0;
        for (ItemStack item : p.getInventory().getContents())
            if (item != null && item.getType() == mat) total += item.getAmount();
        if (total < r.getAmount()) {
            p.sendMessage(color(String.format("&c物品不足！需要 %s ×%d，你只有 %d 个",
                    matName(r.getMaterial()), r.getAmount(), total)));
            return;
        }
        int left = r.getAmount();
        for (ItemStack item : p.getInventory().getContents()) {
            if (left <= 0) break;
            if (item != null && item.getType() == mat) {
                int take = Math.min(item.getAmount(), left);
                item.setAmount(item.getAmount() - take);
                left -= take;
            }
        }
        p.giveExpLevels(r.getExpLevels());
        p.sendMessage(color(String.format("&a兑换成功！消耗 %s ×%d，获得 %d 级经验",
                matName(r.getMaterial()), r.getAmount(), r.getExpLevels())));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String pre = args[0].toLowerCase();
            return config.getRecipes().stream().map(ItemToExpConfig.ExchangeRecipe::getName)
                    .filter(n -> n.toLowerCase().startsWith(pre)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private Material parseMat(String s) {
        String c = s;
        if (c.startsWith("minecraft:") || c.startsWith("MINECRAFT:")) c = c.substring(10);
        return Material.getMaterial(c.toUpperCase());
    }

    private String matName(String s) {
        String c = s;
        if (c.startsWith("minecraft:") || c.startsWith("MINECRAFT:")) c = c.substring(10);
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char ch : c.toCharArray()) {
            if (ch == '_') { up = true; sb.append(' '); }
            else if (up) { sb.append(Character.toUpperCase(ch)); up = false; }
            else { sb.append(Character.toLowerCase(ch)); }
        }
        return sb.toString();
    }

    private static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
