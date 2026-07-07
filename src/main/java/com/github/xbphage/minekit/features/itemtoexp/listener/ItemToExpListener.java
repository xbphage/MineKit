package com.github.xbphage.minekit.features.itemtoexp.listener;

import com.github.xbphage.minekit.features.itemtoexp.config.ItemToExpConfig;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemToExpListener implements Listener {

    private final ItemToExpConfig config;

    public ItemToExpListener(ItemToExpConfig config) { this.config = config; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!config.isLockEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (player.isOp() || player.hasPermission("minekit.itemtoexp.bypass")) return;

        ItemStack item = null;
        boolean movingToContainer = false;

        if (event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT
                || event.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_RIGHT) {
            item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;
            Inventory top = event.getView().getTopInventory();
            if (event.getRawSlot() >= top.getSize()) {
                InventoryType t = top.getType();
                if (t != InventoryType.CRAFTING && t != InventoryType.CREATIVE && t != InventoryType.PLAYER)
                    movingToContainer = true;
            }
        } else {
            item = event.getCursor();
            if (item == null || item.getType() == Material.AIR) return;
            Inventory top = event.getView().getTopInventory();
            if (event.getRawSlot() < top.getSize()) {
                InventoryType t = top.getType();
                if (t != InventoryType.CRAFTING && t != InventoryType.CREATIVE && t != InventoryType.PLAYER)
                    movingToContainer = true;
            }
        }

        if (movingToContainer && config.isLocked(item.getType())) {
            event.setCancelled(true);
            player.sendMessage(color("&c此物品无法存放到容器中"));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!config.isLockEnabled()) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (player.isOp() || player.hasPermission("minekit.itemtoexp.bypass")) return;

        ItemStack item = event.getOldCursor();
        if (item == null || item.getType() == Material.AIR) return;
        if (!config.isLocked(item.getType())) return;

        Inventory top = event.getView().getTopInventory();
        InventoryType t = top.getType();
        if (t == InventoryType.CRAFTING || t == InventoryType.CREATIVE || t == InventoryType.PLAYER) return;

        for (int slot : event.getNewItems().keySet()) {
            if (slot < top.getSize()) {
                event.setCancelled(true);
                player.sendMessage(color("&c此物品无法存放到容器中"));
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (!config.isLockEnabled()) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;
        if (config.isLocked(item.getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        if (!config.isLockEnabled()) return;
        ItemStack item = event.getItem().getItemStack();
        if (item == null || item.getType() == Material.AIR) return;
        if (config.isLocked(item.getType())) {
            event.setCancelled(true);
        }
    }

    private static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
