package com.rinaorc.zombiez.items.types;

import lombok.Getter;
import org.bukkit.Color;

/**
 * Raretés des items
 *
 * PHILOSOPHIE DU SYSTÈME:
 * - La RARETÉ définit la COMPLEXITÉ de l'item (nombre d'affixes, attributs, accès aux pouvoirs)
 * - La ZONE définit la PUISSANCE de l'item (valeurs numériques des stats)
 *
 * La rareté contrôle:
 * - Nombre d'affixes garantis
 * - Nombre d'attributs bonus
 * - Chances de proc des pouvoirs
 * - Accès aux tiers d'affixes supérieurs
 * - Bonus de qualité (légère variation, PAS de puissance brute)
 *
 * La rareté NE contrôle PAS:
 * - La valeur des stats (c'est la zone qui gère ça)
 * - L'item score de base (c'est la zone qui gère ça)
 */
@Getter
public enum Rarity {

    COMMON(
        "Commun",
        "&f",
        "§f",
        60.0,
        0,           // affixCount: pas d'affix
        0,           // bonusAttributes: pas d'attribut bonus
        0.0,         // procChanceBonus: pas de bonus proc
        1,           // maxAffixTier: tier 1 max
        0.0,         // qualityBonus: pas de bonus qualité
        Color.WHITE,
        false
    ),

    UNCOMMON(
        "Peu Commun",
        "&a",
        "§a",
        25.0,
        1,           // affixCount: 1 affix
        0,           // bonusAttributes: pas d'attribut bonus
        0.05,        // procChanceBonus: +5% proc
        2,           // maxAffixTier: tier 2 max
        0.05,        // qualityBonus: +5% qualité
        Color.LIME,
        false
    ),

    RARE(
        "Rare",
        "&9",
        "§9",
        10.0,
        2,           // affixCount: 2 affixes
        1,           // bonusAttributes: 1 attribut bonus
        0.10,        // procChanceBonus: +10% proc
        3,           // maxAffixTier: tier 3 max
        0.10,        // qualityBonus: +10% qualité
        Color.BLUE,
        true
    ),

    EPIC(
        "Épique",
        "&5",
        "§5",
        4.0,
        3,           // affixCount: 3 affixes
        1,           // bonusAttributes: 1 attribut bonus
        0.15,        // procChanceBonus: +15% proc
        4,           // maxAffixTier: tier 4 max
        0.15,        // qualityBonus: +15% qualité
        Color.PURPLE,
        true
    ),

    LEGENDARY(
        "Légendaire",
        "&6",
        "§6",
        0.9,
        4,           // affixCount: 4 affixes
        2,           // bonusAttributes: 2 attributs bonus
        0.20,        // procChanceBonus: +20% proc
        5,           // maxAffixTier: tier 5 max
        0.20,        // qualityBonus: +20% qualité
        Color.ORANGE,
        true
    ),

    MYTHIC(
        "Mythique",
        "&d",
        "§d",
        0.1,
        5,           // affixCount: 5 affixes
        2,           // bonusAttributes: 2 attributs bonus
        0.30,        // procChanceBonus: +30% proc
        5,           // maxAffixTier: tier 5 max
        0.25,        // qualityBonus: +25% qualité
        Color.FUCHSIA,
        true
    ),

    EXALTED(
        "EXALTED",
        "&c&l",
        "§c§l",
        0.01,
        6,           // affixCount: 6 affixes
        3,           // bonusAttributes: 3 attributs bonus
        0.40,        // procChanceBonus: +40% proc
        5,           // maxAffixTier: tier 5 max
        0.30,        // qualityBonus: +30% qualité
        Color.RED,
        true
    );

    private final String displayName;
    private final String colorCode;        // Pour Adventure/Legacy
    private final String chatColor;        // Pour le chat
    private final double baseChance;       // Chance de drop de base (%)
    private final int affixCount;          // Nombre d'affixes garantis
    private final int bonusAttributes;     // Nombre d'attributs bonus
    private final double procChanceBonus;  // Bonus sur les chances de proc des pouvoirs
    private final int maxAffixTier;        // Tier maximum d'affix accessible
    private final double qualityBonus;     // Léger bonus de qualité (variation, PAS puissance)
    private final Color glowColor;         // Couleur pour les effets visuels
    private final boolean hasLightBeam;    // Affiche un beam de lumière au drop

    Rarity(String displayName, String colorCode, String chatColor, double baseChance,
           int affixCount, int bonusAttributes, double procChanceBonus,
           int maxAffixTier, double qualityBonus, Color glowColor, boolean hasLightBeam) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.chatColor = chatColor;
        this.baseChance = baseChance;
        this.affixCount = affixCount;
        this.bonusAttributes = bonusAttributes;
        this.procChanceBonus = procChanceBonus;
        this.maxAffixTier = maxAffixTier;
        this.qualityBonus = qualityBonus;
        this.glowColor = glowColor;
        this.hasLightBeam = hasLightBeam;
    }

    /**
     * Obtient le nom coloré complet
     */
    public String getColoredName() {
        return chatColor + displayName;
    }
    
    /**
     * Obtient la couleur chat (alias)
     */
    public String getColor() {
        return chatColor;
    }

    /**
     * Obtient les étoiles de rareté pour l'affichage
     */
    public String getStars() {
        int stars = ordinal() + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            sb.append("★");
        }
        return chatColor + sb.toString();
    }

    /**
     * Obtient la rareté suivante (upgrade)
     */
    public Rarity getNext() {
        int nextOrdinal = ordinal() + 1;
        if (nextOrdinal >= values().length) {
            return this;
        }
        return values()[nextOrdinal];
    }

    /**
     * Obtient la rareté précédente
     */
    public Rarity getPrevious() {
        int prevOrdinal = ordinal() - 1;
        if (prevOrdinal < 0) {
            return this;
        }
        return values()[prevOrdinal];
    }

    /**
     * Vérifie si cette rareté est supérieure ou égale à une autre
     */
    public boolean isAtLeast(Rarity other) {
        return ordinal() >= other.ordinal();
    }

    /**
     * Génère un bonus de qualité aléatoire basé sur la rareté
     * NOTE: Ce n'est PAS un bonus de puissance, juste un léger bonus de qualité
     * La puissance vient de la ZONE, pas de la rareté
     *
     * @return Bonus de qualité (0.0 à qualityBonus)
     */
    public double rollQualityBonus() {
        return Math.random() * qualityBonus;
    }

    /**
     * Calcule le multiplicateur de score basé sur la complexité de la rareté
     * Ce multiplicateur est appliqué APRÈS le score de zone pour refléter la complexité
     *
     * @return Multiplicateur de complexité (1.0 à 1.5)
     */
    public double getScoreComplexityMultiplier() {
        // La complexité ajoute un bonus au score, mais la zone reste le facteur principal
        // COMMON: 1.0, EXALTED: 1.3 (max +30% bonus de complexité)
        return 1.0 + (ordinal() * 0.05);
    }

    /**
     * Calcule la chance ajustée de proc pour les pouvoirs
     *
     * @param baseProcChance Chance de proc de base du pouvoir
     * @return Chance de proc ajustée avec le bonus de rareté
     */
    public double getAdjustedProcChance(double baseProcChance) {
        return Math.min(1.0, baseProcChance * (1.0 + procChanceBonus));
    }

    /**
     * Obtient une rareté depuis son nom
     */
    public static Rarity fromName(String name) {
        for (Rarity r : values()) {
            if (r.name().equalsIgnoreCase(name) || r.displayName.equalsIgnoreCase(name)) {
                return r;
            }
        }
        return COMMON;
    }

    /**
     * Tire une rareté au sort selon les chances de base
     */
    public static Rarity roll(double luckBonus) {
        double roll = Math.random() * 100;
        double cumulative = 0;

        // Parcourir de la plus rare à la moins rare
        Rarity[] rarities = values();
        for (int i = rarities.length - 1; i >= 0; i--) {
            Rarity rarity = rarities[i];
            double chance = rarity.baseChance * (1 + luckBonus);
            cumulative += chance;
            
            if (roll < cumulative) {
                return rarity;
            }
        }

        return COMMON;
    }

    /**
     * Tire une rareté avec une chance minimum garantie
     */
    public static Rarity rollWithMinimum(double luckBonus, Rarity minimum) {
        Rarity rolled = roll(luckBonus);
        if (rolled.ordinal() < minimum.ordinal()) {
            return minimum;
        }
        return rolled;
    }
}
