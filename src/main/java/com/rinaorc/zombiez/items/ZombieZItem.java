package com.rinaorc.zombiez.items;

import com.rinaorc.zombiez.items.affixes.Affix;
import com.rinaorc.zombiez.items.generator.ArmorTrimGenerator;
import com.rinaorc.zombiez.items.types.ItemType;
import com.rinaorc.zombiez.items.types.Rarity;
import com.rinaorc.zombiez.items.types.StatType;
import com.rinaorc.zombiez.utils.ItemBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
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

    // Armor Trim (optionnel, pour les armures uniquement)
    @Setter
    private String trimPatternKey;
    @Setter
    private String trimMaterialKey;

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

        // Supprimer les tooltips vanilla par défaut (dégâts d'attaque, vitesse)
        builder.hideDefaultAttributes();

        // Rendre l'item incassable (sans afficher le tag)
        builder.unbreakable();

        // Appliquer l'armor trim si c'est une armure et qu'un trim est défini
        if (itemType.isArmor() && trimPatternKey != null && trimMaterialKey != null) {
            ArmorTrimGenerator.TrimResult trimResult =
                ArmorTrimGenerator.getInstance().getTrimByKeys(trimPatternKey, trimMaterialKey);
            if (trimResult != null) {
                builder.trim(trimResult.pattern(), trimResult.material());
            }
        }

        // Appliquer ATTACK_SPEED pour les armes de mêlée (cooldown vanilla)
        if (itemType.isMeleeWeapon()) {
            // Récupérer la vitesse d'attaque de l'item (ou valeur par défaut)
            Map<StatType, Double> totalStats = getTotalStats();
            double attackSpeedStat = totalStats.getOrDefault(StatType.ATTACK_SPEED, 0.0);

            // Vitesse d'attaque de base selon le type d'arme
            // Minecraft base = 4.0, les armes appliquent des malus
            // Épée: -2.4 (1.6 coups/s), Hache: -3.0 (1.0 coups/s), Masse: -3.5 (0.5 coups/s)
            double baseAttackSpeed = getBaseAttackSpeedModifier();
            double finalAttackSpeed = baseAttackSpeed + attackSpeedStat;

            builder.attackSpeed(finalAttackSpeed);
        }

        // Stocker les données dans le PDC
        ItemStack item = builder.build();
        storeData(item);

        return item;
    }

    /**
     * Retourne le modificateur de vitesse d'attaque de base selon le type d'arme
     * Ces valeurs sont des malus appliqués à la vitesse de base de 4.0
     */
    private double getBaseAttackSpeedModifier() {
        return switch (itemType) {
            case SWORD -> -2.4;      // 1.6 coups/s (rapide)
            case AXE -> -3.0;        // 1.0 coups/s (lent mais puissant)
            case MACE -> -3.2;       // 0.8 coups/s (très lent, très puissant)
            default -> -2.4;         // Par défaut comme une épée
        };
    }

    /**
     * Convertit l'item en ItemStack avec le pouvoir appliqué
     * Cette méthode doit être utilisée avec PowerTriggerListener.applyPowerToItem
     */
    public ItemStack toItemStackWithPower(com.rinaorc.zombiez.items.power.PowerTriggerListener powerListener,
                                          com.rinaorc.zombiez.items.power.Power power) {
        ItemStack item = toItemStack();

        if (power != null && powerListener != null) {
            powerListener.applyPowerToItem(item, power, itemLevel, zoneLevel, rarity);
        }

        return item;
    }

    /**
     * Construit le lore de l'item (version simplifiée)
     */
    private List<String> buildLore() {
        List<String> lore = new ArrayList<>();

        // ═══════════════════════════════════════
        // EN-TÊTE: Rareté puis Item Score + Zone
        // ═══════════════════════════════════════
        lore.add(rarity.getChatColor() + "§l" + rarity.getDisplayName().toUpperCase() + " " + rarity.getStars());
        lore.add("§7Item Score: " + getItemScoreColor() + "§l" + itemScore);
        lore.add("§8Requiert: §eZone " + zoneLevel);
        lore.add("");

        // ═══════════════════════════════════════
        // STATS DE BASE (filtrer pour n'afficher QUE les vraies stats de base)
        // Cela corrige la duplication pour les anciens items qui avaient
        // incorrectement stocké les stats d'affixes dans baseStats
        // ═══════════════════════════════════════
        // Filtrer pour ne garder que les vraies stats de base (isBaseStat == true)
        Map<StatType, Double> filteredBaseStats = new LinkedHashMap<>();
        for (var entry : baseStats.entrySet()) {
            if (entry.getKey().isBaseStat()) {
                filteredBaseStats.put(entry.getKey(), entry.getValue());
            }
        }

        if (!filteredBaseStats.isEmpty()) {
            // Header "Stats de base"
            lore.add("§6✧ STATS DE BASE");

            // Grouper par catégorie pour un affichage organisé
            Map<StatType.StatCategory, List<Map.Entry<StatType, Double>>> statsByCategory = new LinkedHashMap<>();
            for (var entry : filteredBaseStats.entrySet()) {
                statsByCategory.computeIfAbsent(entry.getKey().getCategory(), k -> new ArrayList<>()).add(entry);
            }

            // Afficher par catégorie
            for (var categoryEntry : statsByCategory.entrySet()) {
                StatType.StatCategory category = categoryEntry.getKey();
                List<Map.Entry<StatType, Double>> stats = categoryEntry.getValue();

                // Mini header de catégorie
                lore.add(category.getColor() + getCategoryIcon(category) + " " + category.getDisplayName());

                for (var stat : stats) {
                    StatType type = stat.getKey();
                    double value = stat.getValue();
                    String valueColor = value >= 0 ? "§a" : "§c";
                    String formattedValue = type.formatValue(value);

                    // Format compact
                    lore.add("  §7" + type.getDisplayName() + ": " + valueColor + formattedValue);
                }
            }
            lore.add("");
        }

        // ═══════════════════════════════════════
        // AFFIXES (version compacte)
        // ═══════════════════════════════════════
        if (!affixes.isEmpty()) {
            // Séparateur avant les affixes
            lore.add("§8§m                    ");
            lore.add("");
            lore.add("§d⚜ AFFIXES §8(" + affixes.size() + ")");

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
                    String godRollIndicator = type.isGodRoll(value) ? " §6✦" : "";
                    lore.add("  §7" + type.getDisplayName() + ": " + valueColor + type.formatValue(value) + godRollIndicator);
                }
            }
        }

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
     *
     * Nouveaux seuils adaptés au système de scaling par zone:
     * - Zone 1 EXALTED: ~5 000
     * - Zone 25 EXALTED: ~10 000-15 000
     * - Zone 50 EXALTED: ~20 000-30 000+
     */
    private String getItemScoreColor() {
        if (itemScore >= 20000) return "§c§l";    // End-game, zone 50
        if (itemScore >= 12000) return "§d§l";    // Late-game, zone 35+
        if (itemScore >= 7000) return "§6§l";     // Mid-late game, zone 25+
        if (itemScore >= 4000) return "§5";       // Mid-game, zone 15+
        if (itemScore >= 2000) return "§9";       // Early-mid game, zone 10+
        if (itemScore >= 800) return "§a";        // Early game, zone 5+
        if (itemScore >= 300) return "§f";        // Starter
        return "§7";                              // Very basic
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
        NamespacedKey keyBaseStats = new NamespacedKey("zombiez", "base_stats");
        NamespacedKey keyAffixStats = new NamespacedKey("zombiez", "affix_stats");

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

        // Sérialiser les stats de base (format: "STAT_TYPE:value;STAT_TYPE:value")
        StringBuilder baseStatsStr = new StringBuilder();
        for (var entry : baseStats.entrySet()) {
            if (baseStatsStr.length() > 0) baseStatsStr.append(";");
            baseStatsStr.append(entry.getKey().name()).append(":").append(entry.getValue());
        }
        pdc.set(keyBaseStats, PersistentDataType.STRING, baseStatsStr.toString());

        // Sérialiser les affixes avec leurs stats complètes
        // Format: "affixId:tier|STAT:value;STAT:value,affixId:tier|STAT:value"
        StringBuilder affixStr = new StringBuilder();
        for (RolledAffix ra : affixes) {
            if (affixStr.length() > 0) affixStr.append(",");
            affixStr.append(ra.getAffix().getId()).append(":").append(ra.getAffix().getTier().ordinal());
            affixStr.append("|");
            StringBuilder statsStr = new StringBuilder();
            for (var entry : ra.getRolledStats().entrySet()) {
                if (statsStr.length() > 0) statsStr.append(";");
                statsStr.append(entry.getKey().name()).append(":").append(entry.getValue());
            }
            affixStr.append(statsStr);
        }
        pdc.set(keyAffixes, PersistentDataType.STRING, affixStr.toString());

        // Stocker l'armor trim si présent
        if (trimPatternKey != null && trimMaterialKey != null) {
            NamespacedKey keyTrimPattern = new NamespacedKey("zombiez", "trim_pattern");
            NamespacedKey keyTrimMaterial = new NamespacedKey("zombiez", "trim_material");
            pdc.set(keyTrimPattern, PersistentDataType.STRING, trimPatternKey);
            pdc.set(keyTrimMaterial, PersistentDataType.STRING, trimMaterialKey);
        }

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
     * Obtient le niveau de zone d'un ItemStack ZombieZ
     */
    public static int getItemZoneLevel(ItemStack item) {
        if (!isZombieZItem(item)) return 0;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey keyZone = new NamespacedKey("zombiez", "zone");
        Integer zone = pdc.get(keyZone, PersistentDataType.INTEGER);

        return zone != null ? zone : 1;
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
     * Calcule l'Item Score avec la ZONE comme facteur PRINCIPAL
     *
     * PHILOSOPHIE DU SCORING:
     * - La ZONE est le facteur PRINCIPAL (détermine le score de base)
     * - La puissance réelle des stats contribue au score
     * - La rareté ajoute un multiplicateur de COMPLEXITÉ (secondaire)
     *
     * RÉSULTAT ATTENDU:
     * - EXALTED zone 1 → ~5 000
     * - EXALTED zone 50 → 15 000 – 30 000+
     * - Deux items même rareté, zones différentes = scores TRÈS différents
     *
     * @param zoneId Zone où l'item a été dropé (1-50)
     * @param rarity Rareté de l'item
     * @param stats Toutes les stats de l'item
     * @param affixes Liste des affixes
     */
    public static int calculateItemScore(int zoneId, Rarity rarity, Map<StatType, Double> stats, List<RolledAffix> affixes) {
        // Import statique ou référence à ZoneScaling
        double zoneMultiplier = com.rinaorc.zombiez.items.scaling.ZoneScaling.getScoreMultiplier(zoneId);
        int zoneBaseScore = com.rinaorc.zombiez.items.scaling.ZoneScaling.getBaseScoreForZone(zoneId);

        // 1. SCORE DE BASE SELON LA ZONE (facteur PRINCIPAL)
        double score = zoneBaseScore;

        // 2. CONTRIBUTION DES STATS (pondérée par catégorie)
        double statsContribution = 0;
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

            statsContribution += value * weight;
        }
        // Les stats contribuent mais sont déjà scalées par la zone via la génération
        score += statsContribution;

        // 3. BONUS PAR AFFIX (tier et effets spéciaux)
        for (RolledAffix ra : affixes) {
            // Bonus de tier (50-250 par affix selon le tier)
            score += ra.getAffix().getTier().ordinal() * 50;

            // Bonus pour effets spéciaux
            if (ra.getAffix().getSpecialEffect() != null) {
                score += 100;
            }
        }

        // 4. MULTIPLICATEUR DE COMPLEXITÉ (rareté = secondaire)
        // La rareté ajoute un bonus de complexité (max +30%)
        double complexityMultiplier = rarity.getScoreComplexityMultiplier();
        score *= complexityMultiplier;

        // 5. MULTIPLICATEUR FINAL DE ZONE (pour amplifier la différence)
        score *= zoneMultiplier;

        return (int) Math.max(1, score);
    }

    /**
     * @deprecated Utiliser calculateItemScore(int zoneId, Rarity rarity, ...) à la place
     * Cette méthode est conservée pour compatibilité temporaire
     */
    @Deprecated
    public static int calculateItemScore(Rarity rarity, Map<StatType, Double> stats, List<RolledAffix> affixes) {
        // Fallback: utiliser zone 1 si appelé avec l'ancienne signature
        return calculateItemScore(1, rarity, stats, affixes);
    }
    
    /**
     * Reconstruit un ZombieZItem depuis un ItemStack
     * Restaure les stats de base et les affixes depuis le PDC
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
        NamespacedKey keyBaseStats = new NamespacedKey("zombiez", "base_stats");
        NamespacedKey keyAffixes = new NamespacedKey("zombiez", "affixes");

        String typeStr = pdc.get(keyType, PersistentDataType.STRING);
        ItemType type = typeStr != null ? ItemType.valueOf(typeStr) : ItemType.SWORD;

        Integer zone = pdc.get(keyZone, PersistentDataType.INTEGER);
        Long created = pdc.get(keyCreated, PersistentDataType.LONG);
        Integer ilvl = pdc.get(keyItemLevel, PersistentDataType.INTEGER);
        String power = pdc.get(keyPowerId, PersistentDataType.STRING);

        // Lire les données d'armor trim
        NamespacedKey keyTrimPattern = new NamespacedKey("zombiez", "trim_pattern");
        NamespacedKey keyTrimMaterial = new NamespacedKey("zombiez", "trim_material");
        String trimPattern = pdc.get(keyTrimPattern, PersistentDataType.STRING);
        String trimMaterial = pdc.get(keyTrimMaterial, PersistentDataType.STRING);

        // Désérialiser les stats de base (format: "STAT_TYPE:value;STAT_TYPE:value")
        Map<StatType, Double> baseStats = new EnumMap<>(StatType.class);
        String baseStatsStr = pdc.get(keyBaseStats, PersistentDataType.STRING);
        if (baseStatsStr != null && !baseStatsStr.isEmpty()) {
            for (String statPair : baseStatsStr.split(";")) {
                String[] parts = statPair.split(":");
                if (parts.length == 2) {
                    try {
                        StatType statType = StatType.valueOf(parts[0]);
                        double value = Double.parseDouble(parts[1]);
                        baseStats.put(statType, value);
                    } catch (Exception ignored) {}
                }
            }
        }

        // Désérialiser les affixes (format: "affixId:tier|STAT:value;STAT:value,...")
        List<RolledAffix> affixes = new ArrayList<>();
        String affixesStr = pdc.get(keyAffixes, PersistentDataType.STRING);
        if (affixesStr != null && !affixesStr.isEmpty()) {
            var affixRegistry = com.rinaorc.zombiez.items.affixes.AffixRegistry.getInstance();
            for (String affixData : affixesStr.split(",")) {
                if (affixData.isEmpty()) continue;
                String[] mainParts = affixData.split("\\|");
                if (mainParts.length >= 1) {
                    String[] idTier = mainParts[0].split(":");
                    if (idTier.length >= 1) {
                        String affixId = idTier[0];
                        var affix = affixRegistry.getAffix(affixId);
                        if (affix != null) {
                            Map<StatType, Double> rolledStats = new EnumMap<>(StatType.class);
                            // Lire les stats si présentes
                            if (mainParts.length == 2 && !mainParts[1].isEmpty()) {
                                for (String statPair : mainParts[1].split(";")) {
                                    String[] parts = statPair.split(":");
                                    if (parts.length == 2) {
                                        try {
                                            StatType statType = StatType.valueOf(parts[0]);
                                            double value = Double.parseDouble(parts[1]);
                                            rolledStats.put(statType, value);
                                        } catch (Exception ignored) {}
                                    }
                                }
                            }
                            affixes.add(RolledAffix.builder()
                                .affix(affix)
                                .rolledStats(rolledStats)
                                .build());
                        }
                    }
                }
            }
        }

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
            .baseStats(baseStats)
            .affixes(affixes)
            .itemScore(score)
            .createdAt(created != null ? created : System.currentTimeMillis())
            .identified(true)
            .itemLevel(ilvl != null ? ilvl : 1)
            .powerId(power)
            .trimPatternKey(trimPattern)
            .trimMaterialKey(trimMaterial)
            .build();
    }
}
