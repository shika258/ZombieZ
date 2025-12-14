package com.rinaorc.zombiez.items.types;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Arrays;
import java.util.List;

/**
 * Types d'items équipables
 * Définit les matériaux disponibles par tier et les stats de base
 */
@Getter
public enum ItemType {

    // ==================== ARMES ====================
    
    SWORD(
        "Épée",
        ItemCategory.WEAPON,
        EquipmentSlot.HAND,
        new Material[]{
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD
        },
        new int[]{1, 1, 3, 6, 8}, // Zone min par tier
        new double[]{4, 5, 6, 7, 8},   // Dégâts de base
        new double[]{1.6, 1.6, 1.6, 1.6, 1.6} // Vitesse d'attaque
    ),
    
    AXE(
        "Hache",
        ItemCategory.WEAPON,
        EquipmentSlot.HAND,
        new Material[]{
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE
        },
        new int[]{1, 1, 3, 6, 8},
        new double[]{7, 9, 9, 9, 10},  // Plus de dégâts
        new double[]{0.8, 0.8, 0.9, 1.0, 1.0} // Moins de vitesse
    ),
    
    MACE(
        "Masse",
        ItemCategory.WEAPON,
        EquipmentSlot.HAND,
        new Material[]{
            Material.MACE // 1.21.4
        },
        new int[]{7},
        new double[]{7},
        new double[]{0.6}
    ),
    
    BOW(
        "Arc",
        ItemCategory.WEAPON,
        EquipmentSlot.HAND,
        new Material[]{
            Material.BOW
        },
        new int[]{2},
        new double[]{6}, // Dégâts de flèche de base
        new double[]{1.0}
    ),
    
    CROSSBOW(
        "Arbalète",
        ItemCategory.WEAPON,
        EquipmentSlot.HAND,
        new Material[]{
            Material.CROSSBOW
        },
        new int[]{4},
        new double[]{9},
        new double[]{0.5}
    ),
    
    TRIDENT(
        "Trident",
        ItemCategory.WEAPON,
        EquipmentSlot.HAND,
        new Material[]{
            Material.TRIDENT
        },
        new int[]{5},
        new double[]{9},
        new double[]{1.1}
    ),

    // ==================== ARMURE ====================
    
    HELMET(
        "Casque",
        ItemCategory.ARMOR,
        EquipmentSlot.HEAD,
        new Material[]{
            Material.LEATHER_HELMET,
            Material.CHAINMAIL_HELMET,
            Material.IRON_HELMET,
            Material.DIAMOND_HELMET,
            Material.NETHERITE_HELMET
        },
        new int[]{1, 2, 3, 6, 8},
        new double[]{1, 2, 2, 3, 3}, // Points d'armure
        new double[]{0, 0, 0, 2, 3}  // Résistance d'armure
    ),
    
    CHESTPLATE(
        "Plastron",
        ItemCategory.ARMOR,
        EquipmentSlot.CHEST,
        new Material[]{
            Material.LEATHER_CHESTPLATE,
            Material.CHAINMAIL_CHESTPLATE,
            Material.IRON_CHESTPLATE,
            Material.DIAMOND_CHESTPLATE,
            Material.NETHERITE_CHESTPLATE
        },
        new int[]{1, 2, 3, 6, 8},
        new double[]{3, 5, 6, 8, 8},
        new double[]{0, 0, 0, 2, 3}
    ),
    
    LEGGINGS(
        "Jambières",
        ItemCategory.ARMOR,
        EquipmentSlot.LEGS,
        new Material[]{
            Material.LEATHER_LEGGINGS,
            Material.CHAINMAIL_LEGGINGS,
            Material.IRON_LEGGINGS,
            Material.DIAMOND_LEGGINGS,
            Material.NETHERITE_LEGGINGS
        },
        new int[]{1, 2, 3, 6, 8},
        new double[]{2, 4, 5, 6, 6},
        new double[]{0, 0, 0, 2, 3}
    ),
    
    BOOTS(
        "Bottes",
        ItemCategory.ARMOR,
        EquipmentSlot.FEET,
        new Material[]{
            Material.LEATHER_BOOTS,
            Material.CHAINMAIL_BOOTS,
            Material.IRON_BOOTS,
            Material.DIAMOND_BOOTS,
            Material.NETHERITE_BOOTS
        },
        new int[]{1, 2, 3, 6, 8},
        new double[]{1, 1, 2, 3, 3},
        new double[]{0, 0, 0, 2, 3}
    ),

    // ==================== ACCESSOIRES ====================
    
    SHIELD(
        "Bouclier",
        ItemCategory.OFFHAND,
        EquipmentSlot.OFF_HAND,
        new Material[]{
            Material.SHIELD
        },
        new int[]{2},
        new double[]{0},
        new double[]{0}
    );

    private final String displayName;
    private final ItemCategory category;
    private final EquipmentSlot slot;
    private final Material[] materials;     // Matériaux par tier (0-4)
    private final int[] minZoneByTier;      // Zone minimum pour drop ce tier
    private final double[] baseStat1;       // Stat primaire par tier (dégâts ou armure)
    private final double[] baseStat2;       // Stat secondaire par tier (vitesse ou résistance)

    ItemType(String displayName, ItemCategory category, EquipmentSlot slot,
             Material[] materials, int[] minZoneByTier,
             double[] baseStat1, double[] baseStat2) {
        this.displayName = displayName;
        this.category = category;
        this.slot = slot;
        this.materials = materials;
        this.minZoneByTier = minZoneByTier;
        this.baseStat1 = baseStat1;
        this.baseStat2 = baseStat2;
    }

    /**
     * Obtient le tier maximum pour une zone donnée
     */
    public int getMaxTierForZone(int zoneId) {
        int maxTier = 0;
        for (int i = 0; i < minZoneByTier.length; i++) {
            if (zoneId >= minZoneByTier[i]) {
                maxTier = i;
            }
        }
        return maxTier;
    }

    /**
     * Obtient le matériau pour un tier donné
     */
    public Material getMaterialForTier(int tier) {
        if (tier < 0) tier = 0;
        if (tier >= materials.length) tier = materials.length - 1;
        return materials[tier];
    }

    /**
     * Obtient un matériau aléatoire approprié pour une zone
     */
    public Material getRandomMaterialForZone(int zoneId) {
        int maxTier = getMaxTierForZone(zoneId);
        int tier = (int) (Math.random() * (maxTier + 1));
        return getMaterialForTier(tier);
    }

    /**
     * Obtient le tier d'un matériau
     */
    public int getTierOfMaterial(Material material) {
        for (int i = 0; i < materials.length; i++) {
            if (materials[i] == material) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Obtient la stat de base 1 pour un tier
     */
    public double getBaseStat1ForTier(int tier) {
        if (tier < 0) tier = 0;
        if (tier >= baseStat1.length) tier = baseStat1.length - 1;
        return baseStat1[tier];
    }

    /**
     * Obtient la stat de base 2 pour un tier
     */
    public double getBaseStat2ForTier(int tier) {
        if (tier < 0) tier = 0;
        if (tier >= baseStat2.length) tier = baseStat2.length - 1;
        return baseStat2[tier];
    }

    /**
     * Vérifie si ce type est une arme
     */
    public boolean isWeapon() {
        return category == ItemCategory.WEAPON;
    }

    /**
     * Vérifie si ce type est une armure
     */
    public boolean isArmor() {
        return category == ItemCategory.ARMOR;
    }

    /**
     * Obtient le type d'item depuis un matériau
     */
    public static ItemType fromMaterial(Material material) {
        for (ItemType type : values()) {
            for (Material m : type.materials) {
                if (m == material) {
                    return type;
                }
            }
        }
        return null;
    }

    /**
     * Obtient tous les types d'armes
     */
    public static List<ItemType> getWeapons() {
        return Arrays.stream(values())
            .filter(ItemType::isWeapon)
            .toList();
    }

    /**
     * Obtient tous les types d'armures
     */
    public static List<ItemType> getArmors() {
        return Arrays.stream(values())
            .filter(ItemType::isArmor)
            .toList();
    }

    /**
     * Obtient un type aléatoire
     */
    public static ItemType random() {
        ItemType[] types = values();
        return types[(int) (Math.random() * types.length)];
    }

    /**
     * Obtient un type aléatoire d'une catégorie
     */
    public static ItemType randomOfCategory(ItemCategory category) {
        List<ItemType> filtered = Arrays.stream(values())
            .filter(t -> t.category == category)
            .toList();
        return filtered.get((int) (Math.random() * filtered.size()));
    }

    /**
     * Catégories d'items
     */
    public enum ItemCategory {
        WEAPON,
        ARMOR,
        OFFHAND,
        ACCESSORY
    }
}
