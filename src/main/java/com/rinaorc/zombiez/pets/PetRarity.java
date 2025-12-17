package com.rinaorc.zombiez.pets;

import lombok.Getter;
import org.bukkit.Color;

/**
 * Raretés des Pets
 * Définit les caractéristiques de progression et drop
 */
@Getter
public enum PetRarity {

    COMMON(
        "Commun",
        "§7",
        45.0,           // dropRate
        50,             // copiesForMax
        9,              // maxLevel
        5,              // fragmentsPerDuplicate
        50,             // fragmentCost
        Color.GRAY
    ),

    UNCOMMON(
        "Peu Commun",
        "§a",
        30.0,
        100,
        9,
        10,
        100,
        Color.LIME
    ),

    RARE(
        "Rare",
        "§b",
        15.0,
        200,
        9,
        25,
        300,
        Color.AQUA
    ),

    EPIC(
        "Épique",
        "§d",
        7.0,
        400,
        9,
        100,
        1000,
        Color.PURPLE
    ),

    LEGENDARY(
        "Légendaire",
        "§6",
        2.5,
        800,
        9,
        500,
        5000,
        Color.ORANGE
    ),

    MYTHIC(
        "Mythique",
        "§c",
        0.5,
        1500,
        9,
        2000,
        -1,             // Non achetable
        Color.RED
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
     */
    public int getCopiesForLevel(int level) {
        if (level <= 1) return 1;
        return switch (level) {
            case 2 -> 2;
            case 3 -> 4;
            case 4 -> 10;
            case 5 -> 20;
            case 6 -> 50;
            case 7 -> 100;
            case 8 -> 200;
            case 9 -> copiesForMax - 387; // Reste des copies
            default -> copiesForMax;
        };
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
