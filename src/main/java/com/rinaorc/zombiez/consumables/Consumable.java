package com.rinaorc.zombiez.consumables;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représente un item consommable avec des stats scalées selon la zone
 * Pattern similaire à FoodItem mais pour des effets actifs
 */
@Getter
public class Consumable {

    // Clés NBT pour la persistance
    public static final NamespacedKey CONSUMABLE_KEY = new NamespacedKey("zombiez", "consumable_type");
    public static final NamespacedKey CONSUMABLE_UUID = new NamespacedKey("zombiez", "consumable_uuid");
    public static final NamespacedKey CONSUMABLE_ZONE = new NamespacedKey("zombiez", "consumable_zone");
    public static final NamespacedKey CONSUMABLE_RARITY = new NamespacedKey("zombiez", "consumable_rarity");
    public static final NamespacedKey CONSUMABLE_STAT1 = new NamespacedKey("zombiez", "consumable_stat1");
    public static final NamespacedKey CONSUMABLE_STAT2 = new NamespacedKey("zombiez", "consumable_stat2");
    public static final NamespacedKey CONSUMABLE_STAT3 = new NamespacedKey("zombiez", "consumable_stat3");
    public static final NamespacedKey CONSUMABLE_USES = new NamespacedKey("zombiez", "consumable_uses");

    private final UUID uuid;
    private final ConsumableType type;
    private final ConsumableRarity rarity;
    private final int zoneId;

    // Stats calculées et stockées
    private final double stat1;
    private final double stat2;
    private final double stat3;

    // Utilisations (pour consommables multi-usage comme le jetpack)
    private int usesRemaining;

    /**
     * Constructeur pour créer un nouveau consommable
     */
    public Consumable(ConsumableType type, ConsumableRarity rarity, int zoneId) {
        this.uuid = UUID.randomUUID();
        this.type = type;
        this.rarity = rarity;
        this.zoneId = zoneId;

        // Calculer les stats scalées
        this.stat1 = type.calculateScaledStat1(zoneId, rarity);
        this.stat2 = type.calculateScaledStat2(zoneId, rarity);
        this.stat3 = type.calculateScaledStat3(zoneId, rarity);

        // Initialiser les utilisations (par défaut 1, sauf pour certains types)
        this.usesRemaining = calculateInitialUses();
    }

    /**
     * Constructeur pour charger un consommable depuis un ItemStack
     */
    public Consumable(UUID uuid, ConsumableType type, ConsumableRarity rarity, int zoneId,
                      double stat1, double stat2, double stat3, int usesRemaining) {
        this.uuid = uuid;
        this.type = type;
        this.rarity = rarity;
        this.zoneId = zoneId;
        this.stat1 = stat1;
        this.stat2 = stat2;
        this.stat3 = stat3;
        this.usesRemaining = usesRemaining;
    }

    /**
     * Calcule le nombre d'utilisations initiales basé sur le type
     */
    private int calculateInitialUses() {
        return switch (type) {
            case JETPACK -> (int) stat1; // Carburant en secondes converti en "ticks d'usage"
            case GRAPPLING_HOOK -> 3 + (int) (rarity.getStatMultiplier()); // 3-6 utilisations
            default -> 1; // Une seule utilisation par défaut
        };
    }

    /**
     * Utilise une charge du consommable
     * @return true si le consommable doit être consommé (plus d'utilisations)
     */
    public boolean use() {
        usesRemaining--;
        return usesRemaining <= 0;
    }

    /**
     * Crée l'ItemStack pour ce consommable
     */
    public ItemStack createItemStack() {
        ItemStack item = new ItemStack(type.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Nom coloré
            meta.setDisplayName(rarity.getColor() + type.getDisplayName());

            // Lore détaillé
            List<String> lore = buildLore();
            meta.setLore(lore);

            // Stocker les données NBT
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(CONSUMABLE_KEY, PersistentDataType.STRING, type.name());
            pdc.set(CONSUMABLE_UUID, PersistentDataType.STRING, uuid.toString());
            pdc.set(CONSUMABLE_ZONE, PersistentDataType.INTEGER, zoneId);
            pdc.set(CONSUMABLE_RARITY, PersistentDataType.STRING, rarity.name());
            pdc.set(CONSUMABLE_STAT1, PersistentDataType.DOUBLE, stat1);
            pdc.set(CONSUMABLE_STAT2, PersistentDataType.DOUBLE, stat2);
            pdc.set(CONSUMABLE_STAT3, PersistentDataType.DOUBLE, stat3);
            pdc.set(CONSUMABLE_USES, PersistentDataType.INTEGER, usesRemaining);

            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Met à jour un ItemStack existant avec les nouvelles données
     */
    public void updateItemStack(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Mettre à jour le lore
            meta.setLore(buildLore());

            // Mettre à jour les utilisations
            meta.getPersistentDataContainer().set(CONSUMABLE_USES, PersistentDataType.INTEGER, usesRemaining);

            item.setItemMeta(meta);
        }
    }

    /**
     * Construit le lore de l'item
     */
    private List<String> buildLore() {
        List<String> lore = new ArrayList<>();

        // Rareté et catégorie
        lore.add(rarity.getColor() + rarity.getDisplayName() + " §7| " +
                 type.getCategory().getColor() + type.getCategory().getDisplayName());
        lore.add("§8Zone " + zoneId);
        lore.add("");

        // Description
        lore.add("§7" + type.getDescription());
        lore.add("");

        // Stats spécifiques au type
        lore.addAll(getTypeSpecificLore());

        // Utilisations restantes si applicable
        if (usesRemaining > 1 || type == ConsumableType.JETPACK || type == ConsumableType.GRAPPLING_HOOK) {
            lore.add("");
            if (type == ConsumableType.JETPACK) {
                lore.add("§e⛽ Carburant: §f" + String.format("%.1f", (double) usesRemaining / 20.0) + "s");
            } else {
                lore.add("§e✦ Utilisations: §f" + usesRemaining);
            }
        }

        // Instructions
        lore.add("");
        lore.add("§8▸ §7Clic droit pour utiliser");

        return lore;
    }

    /**
     * Obtient le lore spécifique au type de consommable
     */
    private List<String> getTypeSpecificLore() {
        List<String> lore = new ArrayList<>();

        switch (type) {
            case TNT_GRENADE -> {
                lore.add("§c⚔ Dégâts: §f" + String.format("%.1f", stat1));
                lore.add("§e◎ Rayon: §f" + String.format("%.1f", stat2) + " blocs");
                lore.add("§b⏱ Délai: §f" + String.format("%.1f", stat3) + "s");
            }
            case INCENDIARY_BOMB -> {
                lore.add("§c🔥 DPS: §f" + String.format("%.1f", stat1));
                lore.add("§e◎ Rayon: §f" + String.format("%.1f", stat2) + " blocs");
                lore.add("§b⏱ Durée: §f" + String.format("%.1f", stat3) + "s");
            }
            case STICKY_CHARGE -> {
                lore.add("§c⚔ Dégâts: §f" + String.format("%.1f", stat1));
                lore.add("§e◎ Splash: §f" + String.format("%.1f", stat2) + " blocs");
                lore.add("§b⏱ Fusée: §f" + String.format("%.1f", stat3) + "s");
            }
            case ACID_JAR -> {
                lore.add("§c☠ DPS Poison: §f" + String.format("%.1f", stat1));
                lore.add("§e◎ Rayon: §f" + String.format("%.1f", stat2) + " blocs");
                lore.add("§b⏱ Durée: §f" + String.format("%.1f", stat3) + "s");
            }
            case JETPACK -> {
                lore.add("§b⛽ Carburant Total: §f" + String.format("%.1f", stat1) + "s");
                lore.add("§e↑ Puissance: §fMoyenne");
                lore.add("§7Maintenez clic droit pour voler!");
            }
            case GRAPPLING_HOOK -> {
                lore.add("§b➹ Portée: §f" + String.format("%.0f", stat1) + " blocs");
                lore.add("§e⏱ Cooldown: §f" + String.format("%.1f", stat3) + "s");
            }
            case UNSTABLE_PEARL -> {
                lore.add("§b➹ Portée: §f" + String.format("%.0f", stat1) + " blocs");
                lore.add("§c❤ Auto-dégâts: §f" + String.format("%.1f", stat2));
                lore.add("§e⏱ Cooldown: §f" + String.format("%.1f", stat3) + "s");
            }
            case COBWEB_TRAP -> {
                lore.add("§f✧ Toiles: §f" + (int) stat1 + "-" + ((int) stat1 + 2));
                lore.add("§b⏱ Durée: §f" + String.format("%.1f", stat2) + "s");
            }
            case DECOY -> {
                lore.add("§b⏱ Durée: §f" + String.format("%.1f", stat1) + "s");
                lore.add("§e◎ Rayon d'aggro: §f" + String.format("%.1f", stat2) + " blocs");
            }
            case TURRET -> {
                lore.add("§c⚔ Dégâts/tir: §f" + String.format("%.1f", stat1));
                lore.add("§b⏱ Durée: §f" + String.format("%.1f", stat2) + "s");
                lore.add("§e◎ Portée: §f" + String.format("%.1f", stat3) + " blocs");
            }
            case BANDAGE -> {
                lore.add("§a❤ Soin: §f" + String.format("%.1f", stat1));
                lore.add("§d✦ Regen: §f" + String.format("%.1f", stat2) + "s");
            }
            case ANTIDOTE -> {
                lore.add("§a✓ §fPurge tous les debuffs");
                lore.add("§d✦ Immunité: §f" + String.format("%.1f", stat2) + "s");
            }
            case ADRENALINE_KIT -> {
                lore.add("§a❤ Soin: §f" + String.format("%.1f", stat1));
                lore.add("§d✦ Regen: §f" + String.format("%.1f", stat2) + "s");
                lore.add("§b⚡ Speed: §f" + String.format("%.1f", stat3) + "s");
            }
        }

        return lore;
    }

    /**
     * Charge un Consumable depuis un ItemStack
     * @return Le Consumable ou null si l'item n'est pas un consommable valide
     */
    public static Consumable fromItemStack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(CONSUMABLE_KEY, PersistentDataType.STRING)) {
            return null;
        }

        try {
            String typeName = pdc.get(CONSUMABLE_KEY, PersistentDataType.STRING);
            String uuidStr = pdc.get(CONSUMABLE_UUID, PersistentDataType.STRING);
            Integer zoneId = pdc.get(CONSUMABLE_ZONE, PersistentDataType.INTEGER);
            String rarityName = pdc.get(CONSUMABLE_RARITY, PersistentDataType.STRING);
            Double stat1 = pdc.get(CONSUMABLE_STAT1, PersistentDataType.DOUBLE);
            Double stat2 = pdc.get(CONSUMABLE_STAT2, PersistentDataType.DOUBLE);
            Double stat3 = pdc.get(CONSUMABLE_STAT3, PersistentDataType.DOUBLE);
            Integer uses = pdc.get(CONSUMABLE_USES, PersistentDataType.INTEGER);

            if (typeName == null || uuidStr == null || zoneId == null || rarityName == null ||
                stat1 == null || stat2 == null || stat3 == null || uses == null) {
                return null;
            }

            return new Consumable(
                UUID.fromString(uuidStr),
                ConsumableType.valueOf(typeName),
                ConsumableRarity.valueOf(rarityName),
                zoneId,
                stat1,
                stat2,
                stat3,
                uses
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Vérifie si un ItemStack est un consommable ZombieZ
     */
    public static boolean isConsumable(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(CONSUMABLE_KEY, PersistentDataType.STRING);
    }

    /**
     * Obtient le type de consommable d'un ItemStack
     */
    public static ConsumableType getType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        String typeName = meta.getPersistentDataContainer().get(CONSUMABLE_KEY, PersistentDataType.STRING);
        if (typeName == null) return null;

        try {
            return ConsumableType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
