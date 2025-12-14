package com.rinaorc.zombiez.economy.shops;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.*;

/**
 * Représente un magasin dans le jeu
 */
@Getter
public class Shop {

    private final String id;
    private final String displayName;
    private final ShopType type;
    private final Material icon;
    
    // Localisation (pour NPC shops)
    @Setter private Location npcLocation;
    @Setter private String npcName;
    
    // Restrictions
    private final int requiredZone;
    private final int requiredLevel;
    
    // Items
    private final Map<String, ShopItem> items;
    private final List<String> itemOrder; // Ordre d'affichage
    
    // Multiplicateurs
    @Setter private double buyMultiplier = 1.0;
    @Setter private double sellMultiplier = 1.0;

    @Builder
    public Shop(String id, String displayName, ShopType type, Material icon,
                int requiredZone, int requiredLevel) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.icon = icon != null ? icon : Material.CHEST;
        this.requiredZone = requiredZone;
        this.requiredLevel = requiredLevel;
        this.items = new LinkedHashMap<>();
        this.itemOrder = new ArrayList<>();
    }

    /**
     * Ajoute un item au shop
     */
    public void addItem(ShopItem item) {
        items.put(item.getId(), item);
        if (!itemOrder.contains(item.getId())) {
            itemOrder.add(item.getId());
        }
    }

    /**
     * Retire un item du shop
     */
    public void removeItem(String itemId) {
        items.remove(itemId);
        itemOrder.remove(itemId);
    }

    /**
     * Obtient un item par son ID
     */
    public ShopItem getItem(String itemId) {
        return items.get(itemId);
    }

    /**
     * Obtient tous les items dans l'ordre
     */
    public List<ShopItem> getOrderedItems() {
        List<ShopItem> ordered = new ArrayList<>();
        for (String id : itemOrder) {
            ShopItem item = items.get(id);
            if (item != null) {
                ordered.add(item);
            }
        }
        return ordered;
    }

    /**
     * Obtient le prix d'achat avec multiplicateur
     */
    public int getAdjustedBuyPrice(ShopItem item) {
        return (int) (item.getBuyPrice() * buyMultiplier);
    }

    /**
     * Obtient le prix de vente avec multiplicateur
     */
    public int getAdjustedSellPrice(ShopItem item) {
        return (int) (item.getSellPrice() * sellMultiplier);
    }

    /**
     * Restock tous les items
     */
    public void restockAll() {
        items.values().forEach(ShopItem::checkRestock);
    }

    /**
     * Vérifie si un joueur peut accéder au shop
     */
    public boolean canAccess(int playerLevel, int maxUnlockedZone) {
        if (requiredLevel > 0 && playerLevel < requiredLevel) return false;
        if (requiredZone > 0 && maxUnlockedZone < requiredZone) return false;
        return true;
    }

    /**
     * Types de shops
     */
    public enum ShopType {
        GENERAL("Général", Material.CHEST, "§7"),
        WEAPONS("Armes", Material.IRON_SWORD, "§c"),
        ARMOR("Armures", Material.IRON_CHESTPLATE, "§9"),
        CONSUMABLES("Consommables", Material.POTION, "§d"),
        MATERIALS("Matériaux", Material.DIAMOND, "§b"),
        UPGRADES("Améliorations", Material.ANVIL, "§6"),
        RARE("Raretés", Material.NETHER_STAR, "§e"),
        BLACK_MARKET("Marché Noir", Material.ENDER_CHEST, "§8"),
        ZONE_SPECIFIC("Zone", Material.GRASS_BLOCK, "§a");

        @Getter private final String displayName;
        @Getter private final Material icon;
        @Getter private final String colorCode;

        ShopType(String displayName, Material icon, String colorCode) {
            this.displayName = displayName;
            this.icon = icon;
            this.colorCode = colorCode;
        }
    }

    /**
     * Crée un shop pré-configuré pour une zone
     */
    public static Shop createZoneShop(int zoneId) {
        String zoneName = getZoneName(zoneId);
        Shop shop = Shop.builder()
            .id("zone_" + zoneId)
            .displayName("Marchand - " + zoneName)
            .type(ShopType.ZONE_SPECIFIC)
            .icon(Material.VILLAGER_SPAWN_EGG)
            .requiredZone(zoneId)
            .requiredLevel(1)
            .build();

        // Ajouter les items par défaut selon la zone
        addDefaultZoneItems(shop, zoneId);
        
        return shop;
    }

    private static void addDefaultZoneItems(Shop shop, int zoneId) {
        // Items de base disponibles partout
        shop.addItem(ShopItem.vanilla("bread", "Pain", Material.BREAD, 10, 2));
        shop.addItem(ShopItem.vanilla("cooked_beef", "Steak", Material.COOKED_BEEF, 25, 5));
        shop.addItem(ShopItem.vanilla("torch", "Torches (x16)", Material.TORCH, 15, 2));
        
        // Items progressifs par zone
        if (zoneId >= 1) {
            shop.addItem(ShopItem.vanilla("wooden_sword", "Épée en Bois", Material.WOODEN_SWORD, 50, 10));
            shop.addItem(ShopItem.vanilla("leather_helmet", "Casque en Cuir", Material.LEATHER_HELMET, 75, 15));
        }
        
        if (zoneId >= 2) {
            shop.addItem(ShopItem.vanilla("stone_sword", "Épée en Pierre", Material.STONE_SWORD, 150, 30));
            shop.addItem(ShopItem.vanilla("chainmail_chestplate", "Plastron en Mailles", Material.CHAINMAIL_CHESTPLATE, 300, 60));
            shop.addItem(ShopItem.consumable("healing_potion", "Potion de Soin", Material.POTION, 100, 
                List.of("Restaure 4 cœurs", "Instantané")));
        }
        
        if (zoneId >= 3) {
            shop.addItem(ShopItem.vanilla("iron_sword", "Épée en Fer", Material.IRON_SWORD, 400, 80));
            shop.addItem(ShopItem.vanilla("iron_chestplate", "Plastron en Fer", Material.IRON_CHESTPLATE, 600, 120));
            shop.addItem(ShopItem.vanilla("shield", "Bouclier", Material.SHIELD, 350, 70));
        }
        
        if (zoneId >= 5) {
            shop.addItem(ShopItem.vanilla("diamond_sword", "Épée en Diamant", Material.DIAMOND_SWORD, 1500, 300));
            shop.addItem(ShopItem.vanilla("diamond_chestplate", "Plastron en Diamant", Material.DIAMOND_CHESTPLATE, 2500, 500));
            shop.addItem(ShopItem.consumable("strength_potion", "Potion de Force", Material.POTION, 500,
                List.of("+3 dégâts", "Durée: 3 min")));
        }
        
        if (zoneId >= 7) {
            shop.addItem(ShopItem.vanilla("golden_apple", "Pomme Dorée", Material.GOLDEN_APPLE, 1000, 200));
            shop.addItem(ShopItem.vanilla("totem", "Totem d'Immortalité", Material.TOTEM_OF_UNDYING, 5000, 1000));
        }
        
        if (zoneId >= 9) {
            shop.addItem(ShopItem.vanilla("netherite_sword", "Épée en Netherite", Material.NETHERITE_SWORD, 10000, 2000));
            shop.addItem(ShopItem.vanilla("netherite_chestplate", "Plastron en Netherite", Material.NETHERITE_CHESTPLATE, 15000, 3000));
            shop.addItem(ShopItem.vanilla("enchanted_golden_apple", "Pomme Enchantée", Material.ENCHANTED_GOLDEN_APPLE, 8000, 1600));
        }
    }

    private static String getZoneName(int zoneId) {
        return switch (zoneId) {
            case 0 -> "Spawn";
            case 1 -> "Village Abandonné";
            case 2 -> "Plaines Désolées";
            case 3 -> "Désert Aride";
            case 4 -> "Forêt Sombre";
            case 5 -> "Marécages Toxiques";
            case 6 -> "Zone PvP";
            case 7 -> "Montagnes Hostiles";
            case 8 -> "Toundra Glaciale";
            case 9 -> "Terres Corrompues";
            case 10 -> "Enfer";
            case 11 -> "Citadelle Finale";
            default -> "Zone " + zoneId;
        };
    }
}
