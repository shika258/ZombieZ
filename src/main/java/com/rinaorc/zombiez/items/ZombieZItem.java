package com.rinaorc.zombiez.items;

import com.rinaorc.zombiez.items.affixes.Affix;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.utils.ItemBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Représente un item ZombieZ avec ses stats procédurales
 */
@Getter
@Builder
public class ZombieZItem {

    // Identifiant unique de l'item
    private final UUID uuid;
    
    // Propriétés de base
    private final ItemType itemType;
    private final Material material;
    private final Rarity rarity;
    private final int tier;              // Tier du matériau (0-4)
    private final int zoneLevel;         // Zone où l'item a été drop
    
    // Noms
    private final String baseName;       // Nom de base (ex: "Épée")
    private final String generatedName;  // Nom complet généré
    
    // Stats de base (du matériau)
    private final Map<StatType, Double> baseStats;
    
    // Affixes et leurs stats
    private final List<RolledAffix> affixes;
    
    // Score de l'item (pour comparaison rapide)
    private final int itemScore;
    
    // Set d'équipement (optionnel)
    @Setter
    private String setId;
    
    // Timestamp de création
    private final long createdAt;

    // Si l'item a été "identifié" (pour système optionnel d'identification)
    @Setter
    private boolean identified;

    // Item Level (ILVL) - Système de puissance
    private final int itemLevel;

    // Pouvoir associé (optionnel)
    @Setter
    private String powerId;

    /**
     * Calcule toutes les stats combinées de l'item
     */
    public Map<StatType, Double> getTotalStats() {
        Map<StatType, Double> total = new HashMap<>(baseStats);
        
        for (RolledAffix affix : affixes) {
            for (var entry : affix.getRolledStats().entrySet()) {
                total.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }
        
        return total;
    }

    /**
     * Obtient une stat spécifique (combinée)
     */
    public double getStat(StatType stat) {
        return getTotalStats().getOrDefault(stat, 0.0);
    }

    /**
     * Vérifie si l'item a une stat spécifique
     */
    public boolean hasStat(StatType stat) {
        return getTotalStats().containsKey(stat);
    }

    /**
     * Obtient le préfixe de l'item (premier affix PREFIX)
     */
    public Optional<RolledAffix> getPrefix() {
        return affixes.stream()
            .filter(a -> a.getAffix().getType() == Affix.AffixType.PREFIX)
            .findFirst();
    }

    /**
     * Obtient le suffixe de l'item (premier affix SUFFIX)
     */
    public Optional<RolledAffix> getSuffix() {
        return affixes.stream()
            .filter(a -> a.getAffix().getType() == Affix.AffixType.SUFFIX)
            .findFirst();
    }

    /**
     * Obtient tous les préfixes
     */
    public List<RolledAffix> getPrefixes() {
        return affixes.stream()
            .filter(a -> a.getAffix().getType() == Affix.AffixType.PREFIX)
            .toList();
    }

    /**
     * Obtient tous les suffixes
     */
    public List<RolledAffix> getSuffixes() {
        return affixes.stream()
            .filter(a -> a.getAffix().getType() == Affix.AffixType.SUFFIX)
            .toList();
    }

    /**
     * Vérifie si l'item a un effet spécial
     */
    public boolean hasSpecialEffect() {
        return affixes.stream()
            .anyMatch(a -> a.getAffix().getSpecialEffect() != null);
    }

    /**
     * Obtient les effets spéciaux
     */
    public List<String> getSpecialEffects() {
        return affixes.stream()
            .map(a -> a.getAffix().getSpecialEffect())
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Convertit l'item en ItemStack Bukkit
     */
    public ItemStack toItemStack() {
        ItemBuilder builder = new ItemBuilder(material);

        // Nom coloré
        builder.name(rarity.getChatColor() + generatedName);

        // Lore
        List<String> lore = buildLore();
        builder.lore(lore);

        // Glow pour rare+
        if (rarity.isAtLeast(Rarity.RARE)) {
            builder.glow();
        }

        // Cacher tous les attributs vanilla (enchants, attributs, unbreakable, etc.)
        builder.hideAll();

        // Rendre l'item incassable (sans afficher le tag)
        builder.unbreakable();

        // Stocker les données dans le PDC
        ItemStack item = builder.build();
        storeData(item);

        return item;
    }

    /**
     * Convertit l'item en ItemStack avec le pouvoir appliqué
     * Cette méthode doit être utilisée avec PowerTriggerListener.applyPowerToItem
     */
    public ItemStack toItemStackWithPower(com.rinaorc.zombiez.items.power.PowerTriggerListener powerListener,
                                          com.rinaorc.zombiez.items.power.Power power) {
        ItemStack item = toItemStack();

        if (power != null && powerListener != null) {
            powerListener.applyPowerToItem(item, power, itemLevel);
        }

        return item;
    }

    /**
     * Construit le lore de l'item
     */
    private List<String> buildLore() {
        List<String> lore = new ArrayList<>();

        // ═══════════════════════════════════════
        // EN-TÊTE: Rareté et niveau
        // ═══════════════════════════════════════
        lore.add("");
        lore.add(rarity.getChatColor() + "§l" + rarity.getDisplayName().toUpperCase() + " " + rarity.getStars());
        lore.add("§8├ iLvl: " + getILVLColor() + itemLevel + " §8│ Zone: §7" + zoneLevel);
        lore.add("");

        // ═══════════════════════════════════════
        // STATS TOTALES (Vue d'ensemble rapide)
        // ═══════════════════════════════════════
        Map<StatType, Double> totalStats = getTotalStats();

        if (!totalStats.isEmpty()) {
            lore.add("§f§l✦ STATS TOTALES");
            lore.add("§8" + "─".repeat(24));

            // Grouper par catégorie pour un affichage organisé
            Map<StatType.StatCategory, List<Map.Entry<StatType, Double>>> statsByCategory = new LinkedHashMap<>();
            for (var entry : totalStats.entrySet()) {
                statsByCategory.computeIfAbsent(entry.getKey().getCategory(), k -> new ArrayList<>()).add(entry);
            }

            // Afficher par catégorie
            for (var categoryEntry : statsByCategory.entrySet()) {
                StatType.StatCategory category = categoryEntry.getKey();
                List<Map.Entry<StatType, Double>> stats = categoryEntry.getValue();

                // Mini header de catégorie
                lore.add(category.getColor() + "§l" + getCategoryIcon(category) + " " + category.getDisplayName());

                for (var stat : stats) {
                    StatType type = stat.getKey();
                    double value = stat.getValue();
                    String valueColor = value >= 0 ? "§a" : "§c";
                    String formattedValue = type.formatValue(value);

                    // Format compact avec indicateur visuel
                    lore.add("§8  │ " + type.getColor() + type.getIcon() + " §7" + type.getDisplayName() + ": " + valueColor + formattedValue);
                }
            }
            lore.add("");
        }

        // ═══════════════════════════════════════
        // DÉTAILS DES AFFIXES
        // ═══════════════════════════════════════
        if (!affixes.isEmpty()) {
            lore.add("§d§l⚜ AFFIXES §8(" + affixes.size() + ")");
            lore.add("§8" + "─".repeat(24));

            for (RolledAffix rolledAffix : affixes) {
                Affix affix = rolledAffix.getAffix();
                String tierColor = affix.getTier().getColor();
                String tierNumeral = affix.getTier().getNumeral();

                // Nom de l'affix avec son tier
                lore.add(tierColor + "▸ " + affix.getDisplayName() + " §8[" + tierNumeral + "]");

                // Stats de l'affix (compact)
                for (var entry : rolledAffix.getRolledStats().entrySet()) {
                    StatType type = entry.getKey();
                    double value = entry.getValue();
                    String valueColor = value >= 0 ? "§a" : "§c";

                    // Indiquer si c'est un god roll
                    String godRollIndicator = type.isGodRoll(value) ? " §6✦" : "";
                    lore.add("§8    └ " + type.getColor() + type.getIcon() + " §7" + type.getDisplayName() + ": " + valueColor + type.formatValue(value) + godRollIndicator);
                }

                // Effet spécial
                if (affix.getSpecialDescription() != null) {
                    lore.add("§8    └ §d✦ " + affix.getSpecialDescription());
                }
            }
            lore.add("");
        }

        // ═══════════════════════════════════════
        // PIED DE PAGE: Score et ID
        // ═══════════════════════════════════════
        lore.add("§8" + "═".repeat(24));
        lore.add("§7Item Score: " + getItemScoreColor() + "§l" + itemScore + " §8│ §8ID: " + uuid.toString().substring(0, 8));

        return lore;
    }

    /**
     * Obtient l'icône de catégorie
     */
    private String getCategoryIcon(StatType.StatCategory category) {
        return switch (category) {
            case OFFENSIVE -> "⚔";
            case DEFENSIVE -> "🛡";
            case ELEMENTAL -> "✧";
            case RESISTANCE -> "◈";
            case UTILITY -> "✦";
            case MOMENTUM -> "⚡";
            case GROUP -> "♦";
        };
    }

    /**
     * Obtient la couleur du score selon sa valeur
     */
    private String getItemScoreColor() {
        if (itemScore >= 5000) return "§c§l";
        if (itemScore >= 3000) return "§d";
        if (itemScore >= 1500) return "§6";
        if (itemScore >= 700) return "§5";
        if (itemScore >= 300) return "§9";
        if (itemScore >= 100) return "§a";
        return "§f";
    }

    /**
     * Obtient la couleur de l'ILVL selon sa valeur
     */
    private String getILVLColor() {
        if (itemLevel >= 90) return "§c§l"; // Rouge gras
        if (itemLevel >= 75) return "§6§l"; // Orange gras
        if (itemLevel >= 60) return "§d";   // Rose
        if (itemLevel >= 45) return "§5";   // Violet
        if (itemLevel >= 30) return "§9";   // Bleu
        if (itemLevel >= 15) return "§a";   // Vert
        return "§f";                        // Blanc
    }

    /**
     * Stocke les données dans le PersistentDataContainer
     */
    private void storeData(ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        // Namespace pour toutes les données
        NamespacedKey keyUuid = new NamespacedKey("zombiez", "uuid");
        NamespacedKey keyRarity = new NamespacedKey("zombiez", "rarity");
        NamespacedKey keyType = new NamespacedKey("zombiez", "type");
        NamespacedKey keyScore = new NamespacedKey("zombiez", "score");
        NamespacedKey keyZone = new NamespacedKey("zombiez", "zone");
        NamespacedKey keyCreated = new NamespacedKey("zombiez", "created");
        NamespacedKey keyAffixes = new NamespacedKey("zombiez", "affixes");
        NamespacedKey keyItemLevel = new NamespacedKey("zombiez", "item_level");
        NamespacedKey keyPowerId = new NamespacedKey("zombiez", "power_id");
        NamespacedKey keyHasPower = new NamespacedKey("zombiez", "has_power");

        pdc.set(keyUuid, PersistentDataType.STRING, uuid.toString());
        pdc.set(keyRarity, PersistentDataType.STRING, rarity.name());
        pdc.set(keyType, PersistentDataType.STRING, itemType.name());
        pdc.set(keyScore, PersistentDataType.INTEGER, itemScore);
        pdc.set(keyZone, PersistentDataType.INTEGER, zoneLevel);
        pdc.set(keyCreated, PersistentDataType.LONG, createdAt);
        pdc.set(keyItemLevel, PersistentDataType.INTEGER, itemLevel);

        // Stocker le pouvoir si présent
        if (powerId != null && !powerId.isEmpty()) {
            pdc.set(keyHasPower, PersistentDataType.BYTE, (byte) 1);
            pdc.set(keyPowerId, PersistentDataType.STRING, powerId);
        }
        
        // Sérialiser les affixes (format simplifié: "id1:tier1,id2:tier2")
        StringBuilder affixStr = new StringBuilder();
        for (RolledAffix ra : affixes) {
            if (affixStr.length() > 0) affixStr.append(",");
            affixStr.append(ra.getAffix().getId()).append(":").append(ra.getAffix().getTier().ordinal());
        }
        pdc.set(keyAffixes, PersistentDataType.STRING, affixStr.toString());
        
        item.setItemMeta(meta);
    }

    /**
     * Vérifie si un ItemStack est un item ZombieZ
     */
    public static boolean isZombieZItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey keyUuid = new NamespacedKey("zombiez", "uuid");
        
        return pdc.has(keyUuid, PersistentDataType.STRING);
    }

    /**
     * Obtient l'UUID d'un ItemStack ZombieZ
     */
    public static UUID getItemUUID(ItemStack item) {
        if (!isZombieZItem(item)) return null;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey keyUuid = new NamespacedKey("zombiez", "uuid");
        String uuidStr = pdc.get(keyUuid, PersistentDataType.STRING);
        
        return uuidStr != null ? UUID.fromString(uuidStr) : null;
    }

    /**
     * Obtient le score d'un ItemStack ZombieZ
     */
    public static int getItemScore(ItemStack item) {
        if (!isZombieZItem(item)) return 0;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey keyScore = new NamespacedKey("zombiez", "score");
        Integer score = pdc.get(keyScore, PersistentDataType.INTEGER);
        
        return score != null ? score : 0;
    }

    /**
     * Obtient la rareté d'un ItemStack ZombieZ
     */
    public static Rarity getItemRarity(ItemStack item) {
        if (!isZombieZItem(item)) return null;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey keyRarity = new NamespacedKey("zombiez", "rarity");
        String rarityStr = pdc.get(keyRarity, PersistentDataType.STRING);
        
        return rarityStr != null ? Rarity.valueOf(rarityStr) : Rarity.COMMON;
    }

    /**
     * Représente un affix avec ses stats déjà rollées
     */
    @Getter
    @Builder
    public static class RolledAffix {
        private final Affix affix;
        private final Map<StatType, Double> rolledStats;
    }

    /**
     * Calcule l'Item Score basé sur toutes les stats
     */
    public static int calculateItemScore(Rarity rarity, Map<StatType, Double> stats, List<RolledAffix> affixes) {
        double score = 0;
        
        // Score de base selon la rareté
        score += rarity.rollItemScore();
        
        // Bonus par stat (pondéré par importance)
        for (var entry : stats.entrySet()) {
            StatType stat = entry.getKey();
            double value = entry.getValue();
            
            double weight = switch (stat.getCategory()) {
                case OFFENSIVE -> 2.0;
                case DEFENSIVE -> 1.5;
                case ELEMENTAL -> 1.8;
                case RESISTANCE -> 1.2;
                case UTILITY -> 1.0;
                case MOMENTUM -> 1.3;
                case GROUP -> 1.2;
            };
            
            score += value * weight;
        }
        
        // Bonus par affix (tier)
        for (RolledAffix ra : affixes) {
            score += ra.getAffix().getTier().ordinal() * 50;
            
            // Bonus pour effets spéciaux
            if (ra.getAffix().getSpecialEffect() != null) {
                score += 100;
            }
        }
        
        return (int) Math.max(1, score);
    }
    
    /**
     * Reconstruit un ZombieZItem depuis un ItemStack
     * Note: Ne reconstruit pas les affixes complets, juste les données de base
     */
    public static ZombieZItem fromItemStack(ItemStack item) {
        if (!isZombieZItem(item)) return null;
        
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        
        UUID uuid = getItemUUID(item);
        Rarity rarity = getItemRarity(item);
        int score = getItemScore(item);
        
        NamespacedKey keyType = new NamespacedKey("zombiez", "type");
        NamespacedKey keyZone = new NamespacedKey("zombiez", "zone");
        NamespacedKey keyCreated = new NamespacedKey("zombiez", "created");
        NamespacedKey keyItemLevel = new NamespacedKey("zombiez", "item_level");
        NamespacedKey keyPowerId = new NamespacedKey("zombiez", "power_id");

        String typeStr = pdc.get(keyType, PersistentDataType.STRING);
        ItemType type = typeStr != null ? ItemType.valueOf(typeStr) : ItemType.SWORD;

        Integer zone = pdc.get(keyZone, PersistentDataType.INTEGER);
        Long created = pdc.get(keyCreated, PersistentDataType.LONG);
        Integer ilvl = pdc.get(keyItemLevel, PersistentDataType.INTEGER);
        String power = pdc.get(keyPowerId, PersistentDataType.STRING);

        return ZombieZItem.builder()
            .uuid(uuid)
            .itemType(type)
            .material(item.getType())
            .rarity(rarity)
            .tier(0)
            .zoneLevel(zone != null ? zone : 1)
            .baseName(item.getType().name())
            .generatedName(item.hasItemMeta() && item.getItemMeta().hasDisplayName() ?
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName()) : item.getType().name())
            .baseStats(new HashMap<>())
            .affixes(new ArrayList<>())
            .itemScore(score)
            .createdAt(created != null ? created : System.currentTimeMillis())
            .identified(true)
            .itemLevel(ilvl != null ? ilvl : 1)
            .powerId(power)
            .build();
    }
}
