package com.rinaorc.zombiez.consumables;

import lombok.Getter;

/**
 * Raretés des consommables
 * Scale les stats en fonction de la zone
 */
@Getter
public enum ConsumableRarity {
    COMMON("§f", "Commun", 1.0, 0.60),
    UNCOMMON("§a", "Peu Commun", 1.25, 0.25),
    RARE("§9", "Rare", 1.5, 0.10),
    EPIC("§5", "Épique", 2.0, 0.04),
    LEGENDARY("§6", "Légendaire", 3.0, 0.01);

    private final String color;
    private final String displayName;
    private final double statMultiplier;
    private final double dropChance;

    ConsumableRarity(String color, String displayName, double statMultiplier, double dropChance) {
        this.color = color;
        this.displayName = displayName;
        this.statMultiplier = statMultiplier;
        this.dropChance = dropChance;
    }

    /**
     * Obtient une rareté aléatoire basée sur les chances de drop
     */
    public static ConsumableRarity rollRarity(double luckBonus) {
        double roll = Math.random();
        double adjustedRoll = roll * (1 - Math.min(0.3, luckBonus)); // Luck diminue le roll

        double cumulative = 0;
        for (ConsumableRarity rarity : values()) {
            cumulative += rarity.dropChance;
            if (adjustedRoll <= cumulative) {
                return rarity;
            }
        }
        return COMMON;
    }

    /**
     * Obtient la rareté minimale pour une zone donnée
     */
    public static ConsumableRarity getMinRarityForZone(int zoneId) {
        if (zoneId >= 40) return RARE;
        if (zoneId >= 25) return UNCOMMON;
        return COMMON;
    }
}
