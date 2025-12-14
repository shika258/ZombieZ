package com.rinaorc.zombiez.economy.shops;

import com.rinaorc.zombiez.items.ZombieZItem;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représente un item dans un shop
 */
@Getter
@Builder
public class ShopItem {

    private final String id;
    private final String displayName;
    private final Material material;
    private final List<String> lore;
    
    // Prix
    private final int buyPrice;          // Prix d'achat (points)
    private final int sellPrice;         // Prix de vente
    private final int gemPrice;          // Prix en gemmes (optionnel)
    
    // Stock
    @Setter private int stock;           // Stock actuel (-1 = infini)
    private final int maxStock;          // Stock maximum
    private final int restockAmount;     // Quantité de restock
    private final long restockInterval;  // Intervalle de restock (ms)
    
    // Restrictions
    private final int requiredLevel;     // Niveau requis
    private final int requiredZone;      // Zone requise débloquée
    private final String requiredPermission;
    
    // Type d'item
    private final ShopItemType type;
    
    // Pour les items ZombieZ procéduraux
    private final ItemType zombiezItemType;
    private final Rarity minRarity;
    private final int itemTier;
    
    // Limites d'achat
    private final int maxPurchasePerDay;
    @Setter private long lastRestock;

    /**
     * Crée l'ItemStack pour affichage
     */
    public ItemStack createDisplayItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§e" + displayName);
            
            List<String> itemLore = new ArrayList<>();
            if (lore != null) {
                lore.forEach(l -> itemLore.add("§7" + l));
            }
            
            itemLore.add("");
            itemLore.add("§7Prix d'achat: §6" + formatPrice(buyPrice) + " Points");
            if (gemPrice > 0) {
                itemLore.add("§7  ou §d" + gemPrice + " Gemmes");
            }
            if (sellPrice > 0) {
                itemLore.add("§7Prix de vente: §a" + formatPrice(sellPrice) + " Points");
            }
            
            itemLore.add("");
            if (stock >= 0) {
                String stockColor = stock > 10 ? "§a" : (stock > 0 ? "§e" : "§c");
                itemLore.add("§7Stock: " + stockColor + stock + "§7/" + maxStock);
            } else {
                itemLore.add("§7Stock: §a∞ Illimité");
            }
            
            if (requiredLevel > 1) {
                itemLore.add("§7Niveau requis: §e" + requiredLevel);
            }
            if (requiredZone > 0) {
                itemLore.add("§7Zone requise: §e" + requiredZone);
            }
            
            itemLore.add("");
            itemLore.add("§aClic gauche §7pour acheter");
            if (sellPrice > 0) {
                itemLore.add("§cClic droit §7pour vendre");
            }
            
            meta.setLore(itemLore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Crée l'item réel à donner au joueur
     */
    public ItemStack createRealItem() {
        return switch (type) {
            case VANILLA -> new ItemStack(material);
            case ZOMBIEZ_ITEM -> {
                // Génère un item ZombieZ
                var generator = com.rinaorc.zombiez.items.generator.ItemGenerator.getInstance();
                ZombieZItem item = generator.generateWithMinRarity(itemTier, minRarity, 0);
                if (zombiezItemType != null) {
                    item = generator.generate(itemTier, minRarity, zombiezItemType, 0);
                }
                yield item != null ? item.toItemStack() : new ItemStack(material);
            }
            case CONSUMABLE -> createConsumable();
            case UPGRADE -> createUpgradeItem();
            case SPECIAL -> createSpecialItem();
        };
    }

    private ItemStack createConsumable() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a" + displayName);
            List<String> itemLore = new ArrayList<>();
            if (lore != null) {
                lore.forEach(l -> itemLore.add("§7" + l));
            }
            itemLore.add("");
            itemLore.add("§eConsommable");
            meta.setLore(itemLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createUpgradeItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d" + displayName);
            List<String> itemLore = new ArrayList<>();
            if (lore != null) {
                lore.forEach(l -> itemLore.add("§7" + l));
            }
            itemLore.add("");
            itemLore.add("§dAmélioration");
            meta.setLore(itemLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSpecialItem() {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6" + displayName);
            List<String> itemLore = new ArrayList<>();
            if (lore != null) {
                lore.forEach(l -> itemLore.add("§7" + l));
            }
            itemLore.add("");
            itemLore.add("§6Item Spécial");
            meta.setLore(itemLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Vérifie si le joueur peut acheter cet item
     */
    public boolean canBuy(int playerLevel, int maxUnlockedZone, int points, int gems) {
        if (requiredLevel > 0 && playerLevel < requiredLevel) return false;
        if (requiredZone > 0 && maxUnlockedZone < requiredZone) return false;
        if (stock == 0) return false;
        
        // Vérifie le prix
        if (gemPrice > 0 && gems >= gemPrice) return true;
        return points >= buyPrice;
    }

    /**
     * Restock l'item si nécessaire
     */
    public boolean checkRestock() {
        if (stock < 0) return false; // Stock infini
        if (restockInterval <= 0) return false;
        
        long now = System.currentTimeMillis();
        if (now - lastRestock >= restockInterval) {
            stock = Math.min(stock + restockAmount, maxStock);
            lastRestock = now;
            return true;
        }
        return false;
    }

    private String formatPrice(int price) {
        if (price >= 1000000) {
            return String.format("%.1fM", price / 1000000.0);
        } else if (price >= 1000) {
            return String.format("%.1fK", price / 1000.0);
        }
        return String.valueOf(price);
    }

    /**
     * Types d'items de shop
     */
    public enum ShopItemType {
        VANILLA,        // Item Minecraft vanilla
        ZOMBIEZ_ITEM,   // Item procédural ZombieZ
        CONSUMABLE,     // Consommable (potions, nourriture)
        UPGRADE,        // Pierre d'amélioration
        SPECIAL         // Item spécial unique
    }

    /**
     * Builder helper pour créer des items rapidement
     */
    public static ShopItem vanilla(String id, String name, Material mat, int buyPrice, int sellPrice) {
        return ShopItem.builder()
            .id(id)
            .displayName(name)
            .material(mat)
            .buyPrice(buyPrice)
            .sellPrice(sellPrice)
            .stock(-1)
            .maxStock(-1)
            .restockAmount(0)
            .restockInterval(0)
            .requiredLevel(1)
            .requiredZone(0)
            .type(ShopItemType.VANILLA)
            .build();
    }

    public static ShopItem consumable(String id, String name, Material mat, int price, List<String> desc) {
        return ShopItem.builder()
            .id(id)
            .displayName(name)
            .material(mat)
            .lore(desc)
            .buyPrice(price)
            .sellPrice(0)
            .stock(-1)
            .maxStock(-1)
            .type(ShopItemType.CONSUMABLE)
            .build();
    }
}
