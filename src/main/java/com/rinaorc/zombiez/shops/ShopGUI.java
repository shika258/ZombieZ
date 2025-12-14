package com.rinaorc.zombiez.shops;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * GUIs pour les différents shops
 */
public class ShopGUI {

    private static final String ZONE_SHOP_TITLE = "§6§lShop - ";
    private static final String GEM_SHOP_TITLE = "§d§lShop de Gemmes";
    private static final String SELL_SHOP_TITLE = "§e§lVendre vos Items";

    private final ZombieZPlugin plugin;
    private final ShopManager shopManager;

    // Joueurs avec un GUI ouvert
    private final Map<UUID, OpenGUI> openGUIs;

    public ShopGUI(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.shopManager = new ShopManager(plugin); // Utilise le ShopManager local de ce package
        this.openGUIs = new HashMap<>();
    }

    /**
     * Ouvre le shop de zone pour un joueur
     */
    public void openZoneShop(Player player, int zoneId) {
        ShopManager.ZoneShop shop = shopManager.getZoneShop(zoneId);
        if (shop == null) {
            player.sendMessage("§cAucun shop disponible dans cette zone.");
            return;
        }

        int size = Math.min(54, ((shop.getItems().size() / 9) + 1) * 9 + 9);
        Inventory inv = Bukkit.createInventory(null, size, ZONE_SHOP_TITLE + shop.getName());

        // Bordure décorative
        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ").build();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(size - 9 + i, border);
        }

        // Items du shop
        int slot = 9;
        for (ShopManager.ShopItem item : shop.getItems()) {
            ItemStack display = new ItemBuilder(item.getMaterial())
                .name(item.getName())
                .lore(
                    item.getDescription(),
                    "",
                    "§7Prix: " + (item.getCurrency() == ShopManager.ShopItem.Currency.POINTS ?
                        "§6" + item.getPrice() + " Points" : "§d" + item.getPrice() + " Gemmes"),
                    "",
                    "§eCliquez pour acheter"
                )
                .build();
            inv.setItem(slot++, display);
        }

        // Boutons en bas
        inv.setItem(size - 5, new ItemBuilder(Material.EMERALD)
            .name("§a§lVos Points: §6" + plugin.getEconomyManager().getPoints(player))
            .build());
        inv.setItem(size - 4, new ItemBuilder(Material.AMETHYST_SHARD)
            .name("§d§lVos Gemmes: §b" + plugin.getEconomyManager().getGems(player))
            .build());

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), new OpenGUI(OpenGUI.Type.ZONE_SHOP, zoneId, shop));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.5f);
    }

    /**
     * Ouvre le GUI de vente
     */
    public void openSellGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, SELL_SHOP_TITLE);

        // Bordure
        ItemStack border = new ItemBuilder(Material.YELLOW_STAINED_GLASS_PANE)
            .name(" ").build();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, border);
        }

        // Instructions
        inv.setItem(4, new ItemBuilder(Material.BOOK)
            .name("§e§lComment vendre")
            .lore(
                "§7Placez vos items ZombieZ",
                "§7dans les slots ci-dessous",
                "§7puis cliquez sur §aVendre Tout",
                "",
                "§cSeuls les items ZombieZ",
                "§cpeuvent être vendus ici"
            )
            .build());

        // Bouton vendre tout
        inv.setItem(49, new ItemBuilder(Material.LIME_CONCRETE)
            .name("§a§lVendre Tout")
            .lore(
                "§7Vend tous les items",
                "§7placés ci-dessus"
            )
            .build());

        // Info
        inv.setItem(45, new ItemBuilder(Material.GOLD_INGOT)
            .name("§6§lVos Points: §e" + plugin.getEconomyManager().getPoints(player))
            .build());

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), new OpenGUI(OpenGUI.Type.SELL, 0, null));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.2f);
    }

    /**
     * Ouvre le shop de gemmes
     */
    public void openGemShop(Player player) {
        openGemShop(player, 0);
    }

    /**
     * Ouvre le shop de gemmes à une page spécifique
     */
    public void openGemShop(Player player, int page) {
        List<ShopManager.GemShopItem> items = shopManager.getGemShopItems();
        int itemsPerPage = 28; // 4 lignes de 7 items
        int totalPages = (items.size() - 1) / itemsPerPage + 1;
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, GEM_SHOP_TITLE + " §7(Page " + (page + 1) + "/" + totalPages + ")");

        // Bordure
        ItemStack border = new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
            .name(" ").build();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(45 + i, border);
        }
        for (int i = 0; i < 6; i++) {
            inv.setItem(i * 9, border);
            inv.setItem(i * 9 + 8, border);
        }

        // Items de cette page
        int startIndex = page * itemsPerPage;
        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };

        var playerData = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());

        for (int i = 0; i < slots.length && startIndex + i < items.size(); i++) {
            ShopManager.GemShopItem item = items.get(startIndex + i);

            Material material = getGemItemMaterial(item.getType());
            boolean owned = playerData != null && playerData.hasCosmetic(item.getId());

            ItemBuilder builder = new ItemBuilder(material)
                .name(item.getName())
                .lore(
                    item.getDescription(),
                    "",
                    "§7Type: §f" + item.getType().name(),
                    "§7Prix: §d" + item.getPrice() + " Gemmes",
                    ""
                );

            if (owned && item.getType() != ShopManager.GemShopItem.Type.BOOSTER &&
                item.getType() != ShopManager.GemShopItem.Type.CRATE) {
                builder.lore("§a✓ Déjà possédé");
                builder.glow(true);
            } else {
                builder.lore("§eCliquez pour acheter");
            }

            inv.setItem(slots[i], builder.build());
        }

        // Navigation
        if (page > 0) {
            inv.setItem(48, new ItemBuilder(Material.ARROW)
                .name("§e◀ Page Précédente")
                .build());
        }
        if (page < totalPages - 1) {
            inv.setItem(50, new ItemBuilder(Material.ARROW)
                .name("§ePage Suivante ▶")
                .build());
        }

        // Info gemmes
        inv.setItem(49, new ItemBuilder(Material.AMETHYST_SHARD)
            .name("§d§lVos Gemmes: §b" + plugin.getEconomyManager().getGems(player))
            .lore(
                "",
                "§7Gagnez des gemmes en:",
                "§7- Tuant des boss",
                "§7- Événements spéciaux",
                "§7- Achievements"
            )
            .build());

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), new OpenGUI(OpenGUI.Type.GEM_SHOP, page, null));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 1.2f);
    }

    /**
     * Obtient le matériel pour un type d'item du gem shop
     */
    private Material getGemItemMaterial(ShopManager.GemShopItem.Type type) {
        return switch (type) {
            case TITLE -> Material.NAME_TAG;
            case DEATH_EFFECT -> Material.SKELETON_SKULL;
            case TRAIL -> Material.FIREWORK_STAR;
            case PET -> Material.SPAWNER;
            case BOOSTER -> Material.EXPERIENCE_BOTTLE;
            case CRATE -> Material.CHEST;
        };
    }

    /**
     * Listener pour les interactions avec les GUIs
     */
    public static class ShopGUIListener implements Listener {
        private final ZombieZPlugin plugin;
        private final ShopGUI shopGUI;

        public ShopGUIListener(ZombieZPlugin plugin, ShopGUI shopGUI) {
            this.plugin = plugin;
            this.shopGUI = shopGUI;
        }

        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            if (!(event.getWhoClicked() instanceof Player player)) return;

            OpenGUI openGUI = shopGUI.openGUIs.get(player.getUniqueId());
            if (openGUI == null) return;

            String title = event.getView().getTitle();

            // Zone Shop
            if (title.startsWith(ZONE_SHOP_TITLE)) {
                event.setCancelled(true);
                handleZoneShopClick(player, event, openGUI);
            }
            // Sell GUI
            else if (title.equals(SELL_SHOP_TITLE)) {
                handleSellGUIClick(player, event);
            }
            // Gem Shop
            else if (title.startsWith(GEM_SHOP_TITLE)) {
                event.setCancelled(true);
                handleGemShopClick(player, event, openGUI);
            }
        }

        private void handleZoneShopClick(Player player, InventoryClickEvent event, OpenGUI openGUI) {
            int slot = event.getRawSlot();
            if (slot < 9 || slot >= event.getInventory().getSize() - 9) return;

            if (openGUI.getShop() instanceof ShopManager.ZoneShop zoneShop) {
                int itemIndex = slot - 9;
                if (itemIndex >= 0 && itemIndex < zoneShop.getItems().size()) {
                    ShopManager.ShopItem item = zoneShop.getItems().get(itemIndex);

                    if (shopGUI.shopManager.buyShopItem(player, item)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                        // Rafraîchir le GUI
                        shopGUI.openZoneShop(player, openGUI.getData());
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                    }
                }
            }
        }

        private void handleSellGUIClick(Player player, InventoryClickEvent event) {
            int slot = event.getRawSlot();

            // Bouton vendre tout
            if (slot == 49) {
                event.setCancelled(true);
                sellAllItems(player, event.getInventory());
                return;
            }

            // Bordures non cliquables
            if (slot < 9 || slot >= 45) {
                event.setCancelled(true);
            }
            // Zone de dépôt - permettre le placement
        }

        private void sellAllItems(Player player, Inventory inv) {
            int totalSold = 0;
            int totalPoints = 0;

            for (int i = 9; i < 45; i++) {
                ItemStack item = inv.getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;

                if (ZombieZItem.isZombieZItem(item)) {
                    ZombieZItem zzItem = ZombieZItem.fromItemStack(item);
                    if (zzItem != null) {
                        int price = shopGUI.shopManager.calculateSellPrice(zzItem);
                        totalPoints += price * item.getAmount();
                        totalSold += item.getAmount();
                        inv.setItem(i, null);
                    }
                }
            }

            if (totalSold > 0) {
                plugin.getEconomyManager().addPoints(player, totalPoints);
                player.sendMessage("§aVendu §e" + totalSold + " item(s) §apour §6" + totalPoints + " Points§a!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);
            } else {
                player.sendMessage("§cAucun item ZombieZ à vendre!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
            }
        }

        private void handleGemShopClick(Player player, InventoryClickEvent event, OpenGUI openGUI) {
            int slot = event.getRawSlot();
            int page = openGUI.getData();

            // Navigation
            if (slot == 48) {
                shopGUI.openGemShop(player, page - 1);
                return;
            }
            if (slot == 50) {
                shopGUI.openGemShop(player, page + 1);
                return;
            }

            // Items
            int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
            };

            int slotIndex = -1;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == slot) {
                    slotIndex = i;
                    break;
                }
            }

            if (slotIndex >= 0) {
                int itemIndex = page * 28 + slotIndex;
                List<ShopManager.GemShopItem> items = shopGUI.shopManager.getGemShopItems();

                if (itemIndex < items.size()) {
                    ShopManager.GemShopItem item = items.get(itemIndex);

                    if (shopGUI.shopManager.buyGemShopItem(player, item)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                        shopGUI.openGemShop(player, page);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1f);
                    }
                }
            }
        }

        @EventHandler
        public void onInventoryClose(InventoryCloseEvent event) {
            if (!(event.getPlayer() instanceof Player player)) return;

            OpenGUI openGUI = shopGUI.openGUIs.remove(player.getUniqueId());
            if (openGUI == null) return;

            // Si c'était un GUI de vente, rendre les items non vendus
            if (openGUI.getType() == OpenGUI.Type.SELL) {
                for (int i = 9; i < 45; i++) {
                    ItemStack item = event.getInventory().getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        if (player.getInventory().firstEmpty() != -1) {
                            player.getInventory().addItem(item);
                        } else {
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        }
                    }
                }
            }
        }
    }

    /**
     * Représente un GUI ouvert
     */
    private static class OpenGUI {
        enum Type {
            ZONE_SHOP, SELL, GEM_SHOP
        }

        private final Type type;
        private final int data;
        private final Object shop;

        OpenGUI(Type type, int data, Object shop) {
            this.type = type;
            this.data = data;
            this.shop = shop;
        }

        public Type getType() { return type; }
        public int getData() { return data; }
        public Object getShop() { return shop; }
    }
}
