package com.rinaorc.zombiez.economy.gui;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.economy.shops.Shop;
import com.rinaorc.zombiez.economy.shops.ShopItem;
import com.rinaorc.zombiez.economy.shops.ShopManager;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Interface graphique d'un magasin
 */
public class ShopGUI {

    private final ZombieZPlugin plugin;
    private final Player player;
    private final Shop shop;
    private final ShopManager shopManager;
    
    private Inventory inventory;
    private int currentPage = 0;
    private final int itemsPerPage = 28; // 7x4 slots
    private final List<ShopItem> items;
    
    // Mapping slot -> item
    private final Map<Integer, ShopItem> slotMapping;

    public ShopGUI(ZombieZPlugin plugin, Player player, Shop shop, ShopManager shopManager) {
        this.plugin = plugin;
        this.player = player;
        this.shop = shop;
        this.shopManager = shopManager;
        this.items = shop.getOrderedItems();
        this.slotMapping = new HashMap<>();
    }

    /**
     * Ouvre le GUI
     */
    public void open() {
        createInventory();
        player.openInventory(inventory);
    }

    /**
     * Crée l'inventaire
     */
    private void createInventory() {
        String title = shop.getDisplayName() + " §7[Page " + (currentPage + 1) + "/" + getMaxPages() + "]";
        inventory = Bukkit.createInventory(null, 54, title);
        
        slotMapping.clear();
        
        // Bordure décorative
        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
            .name(" ")
            .build();
        
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(45 + i, border);
        }
        for (int i = 0; i < 6; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
        
        // Items du shop
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());
        
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            // Sauter les bordures
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            if (slot >= 44) break;
            
            ShopItem item = items.get(i);
            ItemStack display = createShopItemDisplay(item);
            inventory.setItem(slot, display);
            slotMapping.put(slot, item);
            slot++;
        }
        
        // Navigation
        if (currentPage > 0) {
            inventory.setItem(48, new ItemBuilder(Material.ARROW)
                .name("§a◄ Page Précédente")
                .lore("§7Cliquez pour revenir")
                .build());
        }
        
        if (currentPage < getMaxPages() - 1) {
            inventory.setItem(50, new ItemBuilder(Material.ARROW)
                .name("§a► Page Suivante")
                .lore("§7Cliquez pour avancer")
                .build());
        }
        
        // Info joueur
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        if (data != null) {
            inventory.setItem(49, new ItemBuilder(Material.GOLD_INGOT)
                .name("§6Votre Solde")
                .lore(
                    "§7Points: §6" + data.getPoints(),
                    "§7Gemmes: §d" + data.getGems(),
                    "",
                    "§aClic gauche §7= Acheter (Points)",
                    "§dClic droit §7= Acheter (Gemmes)",
                    "§eShift + Clic §7= Acheter x5"
                )
                .build());
        }
        
        // Bouton fermer
        inventory.setItem(53, new ItemBuilder(Material.BARRIER)
            .name("§cFermer")
            .build());
    }

    /**
     * Crée l'affichage d'un item de shop
     */
    private ItemStack createShopItemDisplay(ShopItem item) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        boolean canBuy = data != null && item.canBuy(
            data.getLevel().get(), 
            data.getMaxZoneReached(),
            (int) data.getPoints().get(),
            data.getGems().get()
        );
        
        ItemBuilder builder = new ItemBuilder(item.getMaterial());
        
        String nameColor = canBuy ? "§a" : "§c";
        builder.name(nameColor + item.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        if (item.getLore() != null) {
            item.getLore().forEach(l -> lore.add("§7" + l));
            lore.add("");
        }
        
        // Prix
        int adjustedBuyPrice = shop.getAdjustedBuyPrice(item);
        int adjustedSellPrice = shop.getAdjustedSellPrice(item);
        
        String priceColor = (data != null && data.getPoints().get() >= adjustedBuyPrice) ? "§a" : "§c";
        lore.add("§7Prix: " + priceColor + formatNumber(adjustedBuyPrice) + " Points");
        
        if (item.getGemPrice() > 0) {
            String gemColor = (data != null && data.getGems().get() >= item.getGemPrice()) ? "§a" : "§c";
            lore.add("§7  ou " + gemColor + item.getGemPrice() + " Gemmes");
        }
        
        if (adjustedSellPrice > 0) {
            lore.add("§7Revente: §e" + formatNumber(adjustedSellPrice) + " Points");
        }
        
        lore.add("");
        
        // Stock
        if (item.getStock() >= 0) {
            String stockColor = item.getStock() > 10 ? "§a" : (item.getStock() > 0 ? "§e" : "§c");
            lore.add("§7Stock: " + stockColor + item.getStock() + "§7/" + item.getMaxStock());
        } else {
            lore.add("§7Stock: §a∞ Illimité");
        }
        
        // Restrictions
        if (item.getRequiredLevel() > 1) {
            String lvlColor = (data != null && data.getLevel().get() >= item.getRequiredLevel()) ? "§a" : "§c";
            lore.add("§7Niveau: " + lvlColor + item.getRequiredLevel());
        }
        if (item.getRequiredZone() > 0) {
            String zoneColor = (data != null && data.getMaxZoneReached() >= item.getRequiredZone()) ? "§a" : "§c";
            lore.add("§7Zone: " + zoneColor + item.getRequiredZone());
        }
        
        lore.add("");
        if (canBuy) {
            lore.add("§aClic gauche §7→ Acheter (Points)");
            if (item.getGemPrice() > 0) {
                lore.add("§dClic droit §7→ Acheter (Gemmes)");
            }
            lore.add("§eShift §7→ Acheter x5");
        } else {
            lore.add("§cVous ne pouvez pas acheter");
        }
        
        builder.lore(lore);
        
        if (!canBuy) {
            builder.glow(false);
        }
        
        return builder.build();
    }

    /**
     * Gère un clic
     */
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        
        int slot = event.getRawSlot();
        
        // Navigation
        if (slot == 48 && currentPage > 0) {
            currentPage--;
            createInventory();
            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            return;
        }
        
        if (slot == 50 && currentPage < getMaxPages() - 1) {
            currentPage++;
            createInventory();
            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            return;
        }
        
        if (slot == 53) {
            player.closeInventory();
            return;
        }
        
        // Achat
        ShopItem item = slotMapping.get(slot);
        if (item == null) return;
        
        int quantity = event.isShiftClick() ? 5 : 1;
        boolean useGems = event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT;
        
        ShopManager.PurchaseResult result = shopManager.processPurchase(player, shop, item, quantity, useGems);
        
        if (result.success()) {
            player.sendMessage("§a" + result.message());
            createInventory(); // Refresh
            player.openInventory(inventory);
        } else {
            player.sendMessage("§c" + result.message());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private int getMaxPages() {
        return Math.max(1, (int) Math.ceil((double) items.size() / itemsPerPage));
    }

    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }

    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Listener pour les événements de GUI
     */
    public static class ShopGUIListener implements Listener {
        
        private final ZombieZPlugin plugin;
        
        public ShopGUIListener(ZombieZPlugin plugin) {
            this.plugin = plugin;
        }
        
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;
            
            String title = event.getView().getTitle();
            if (!title.contains("§") || !title.contains("[Page")) return;
            
            // Vérifier si c'est un shop GUI
            ShopManager shopManager = plugin.getShopManager();
            if (shopManager == null) return;
            
            // Trouver le shop par le titre
            for (Shop shop : shopManager.getShops().values()) {
                if (title.startsWith(shop.getDisplayName())) {
                    // C'est ce shop
                    ShopGUI gui = new ShopGUI(plugin, player, shop, shopManager);
                    gui.inventory = event.getInventory();
                    gui.handleClick(event);
                    return;
                }
            }
        }
        
        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player player)) return;
            
            ShopManager shopManager = plugin.getShopManager();
            if (shopManager != null) {
                shopManager.closeShop(player);
            }
        }
    }
}
