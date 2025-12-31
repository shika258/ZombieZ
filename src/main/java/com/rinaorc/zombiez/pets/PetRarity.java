package com.rinaorc.zombiez.pets;

import lombok.Getter;
import org.bukkit.Color;

/**
 * Raretés des Pets
 * Définit les caractéristiques de progression et drop
 */
@Getter
public enum PetRarity {

    // ÉQUILIBRAGE: Plus de copies nécessaires MAIS plus de fragments en récompense
    // Ratio copies/fragments maintenu pour que le grind soit long mais satisfaisant

    COMMON(
        "Commun",
        "§f",           // Blanc (cohérent avec Rarity.java armes)
        45.0,           // dropRate
        150,            // copiesForMax (x3 original - grind raisonnable)
        11,             // maxLevel
        8,              // fragmentsPerDuplicate (x1.6 original - compense le grind)
        40,             // fragmentCost (prix pour acheter 1 copie via wild card)
        Color.WHITE
    ),

    UNCOMMON(
        "Peu Commun",
        "§a",
        30.0,
        300,            // x3 original
        11,
        20,             // x2 original
        100,            // wild card cost
        Color.LIME
    ),

    RARE(
        "Rare",
        "§9",           // Bleu (cohérent avec Rarity.java armes)
        15.0,
        600,            // x3 original
        11,
        50,             // x2 original
        300,            // wild card cost
        Color.BLUE
    ),

    EPIC(
        "Épique",
        "§5",           // Violet foncé (cohérent avec Rarity.java armes)
        7.0,
        1200,           // x3 original
        11,
        150,            // x1.5 original
        800,            // wild card cost
        Color.PURPLE
    ),

    LEGENDARY(
        "Légendaire",
        "§6",
        2.5,
        2500,           // x3.1 original
        11,
        600,            // x1.2 original
        3000,           // wild card cost
        Color.ORANGE
    ),

    MYTHIC(
        "Mythique",
        "§d",           // Rose/Magenta (cohérent avec Rarity.java armes)
        0.5,
        5000,           // x3.3 original
        11,
        2000,           // identique - mythique reste rare
        -1,             // Non achetable via wild card
        Color.FUCHSIA
    ),

    EXALTED(
        "Exalté",
        "§c§l",         // Rouge gras (cohérent avec Rarity.java armes)
        0.1,            // 0.1% de chance - extrêmement rare
        10000,          // 10000 copies pour max - grind massif
        15,             // 15 niveaux max (4 de plus que les autres)
        5000,           // 5000 fragments par duplicate
        -1,             // Non achetable via wild card
        Color.RED       // Rouge
    );

    private final String displayName;
    private final String color;
    private final double dropRate;
    private final int copiesForMax;
    private final int maxLevel;
    private final int fragmentsPerDuplicate;
    private final int fragmentCost;
    private final Color glowColor;

    PetRarity(String displayName, String color, double dropRate, int copiesForMax,
              int maxLevel, int fragmentsPerDuplicate, int fragmentCost, Color glowColor) {
        this.displayName = displayName;
        this.color = color;
        this.dropRate = dropRate;
        this.copiesForMax = copiesForMax;
        this.maxLevel = maxLevel;
        this.fragmentsPerDuplicate = fragmentsPerDuplicate;
        this.fragmentCost = fragmentCost;
        this.glowColor = glowColor;
    }

    /**
     * Obtient le nom coloré de la rareté
     */
    public String getColoredName() {
        return color + displayName;
    }

    /**
     * Obtient les copies requises pour un niveau donné
     * La progression s'adapte à la rareté (copiesForMax)
     * Progression exponentielle style Clash Royale/Brawl Stars
     */
    public int getCopiesForLevel(int level) {
        if (level <= 1) return 1;
        if (level > maxLevel) return 0;

        // Progression exponentielle: chaque niveau coûte significativement plus
        // Les premiers niveaux sont rapides, les derniers très longs
        double[] percentages = {
            0.005,  // Level 1: 0.5% (facile au début)
            0.01,   // Level 2: 1%
            0.02,   // Level 3: 2%
            0.03,   // Level 4: 3%
            0.05,   // Level 5: 5%
            0.07,   // Level 6: 7%
            0.10,   // Level 7: 10%
            0.13,   // Level 8: 13%
            0.17,   // Level 9: 17%
            0.20,   // Level 10: 20% (mur de progression)
            0.225,  // Level 11: 22.5% (prestige/max pour non-Exalted)
            // Niveaux Exalted uniquement (12-15)
            0.25,   // Level 12: 25%
            0.30,   // Level 13: 30%
            0.35,   // Level 14: 35%
            0.40    // Level 15: 40% (max Exalted)
        };

        // Calculer le nombre de copies pour ce niveau
        int copiesNeeded = Math.max(1, (int) Math.ceil(copiesForMax * percentages[level - 1]));

        // S'assurer que le dernier niveau récupère exactement le reste
        if (level == maxLevel) {
            int totalUsed = 0;
            for (int i = 1; i < maxLevel; i++) {
                totalUsed += Math.max(1, (int) Math.ceil(copiesForMax * percentages[i - 1]));
            }
            copiesNeeded = Math.max(1, copiesForMax - totalUsed);
        }

        return copiesNeeded;
    }

    /**
     * Obtient le total de copies requises pour atteindre un niveau
     */
    public int getTotalCopiesForLevel(int level) {
        int total = 0;
        for (int i = 1; i <= level; i++) {
            total += getCopiesForLevel(i);
        }
        return total;
    }

    /**
     * Obtient les copies requises pour un Star Power
     */
    public int getCopiesForStarPower(int starPower) {
        return switch (starPower) {
            case 1 -> (int) (copiesForMax * 0.5);
            case 2 -> copiesForMax;
            case 3 -> (int) (copiesForMax * 2.0);
            default -> 0;
        };
    }

    /**
     * Vérifie si cette rareté est au moins égale à une autre
     */
    public boolean isAtLeast(PetRarity other) {
        return ordinal() >= other.ordinal();
    }

    /**
     * Obtient la rareté suivante
     */
    public PetRarity getNext() {
        int next = ordinal() + 1;
        if (next >= values().length) return this;
        return values()[next];
    }

    /**
     * Obtient les étoiles visuelles
     */
    public String getStars() {
        int stars = ordinal() + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            sb.append("★");
        }
        return color + sb.toString();
    }

    /**
     * Tire une rareté au sort
     */
    public static PetRarity roll(double luckBonus) {
        double roll = Math.random() * 100;
        double cumulative = 0;

        PetRarity[] rarities = values();
        for (int i = rarities.length - 1; i >= 0; i--) {
            PetRarity rarity = rarities[i];
            double chance = rarity.dropRate * (1 + luckBonus);
            cumulative += chance;

            if (roll < cumulative) {
                return rarity;
            }
        }

        return COMMON;
    }

    /**
     * Obtient une rareté depuis son nom
     */
    public static PetRarity fromName(String name) {
        if (name == null) return null;
        for (PetRarity r : values()) {
            if (r.name().equalsIgnoreCase(name) || r.displayName.equalsIgnoreCase(name)) {
                return r;
            }
        }
        return null;
    }
}
