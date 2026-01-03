package com.rinaorc.zombiez.ascension;

import lombok.Getter;

/**
 * Tiers de puissance des mutations
 * Détermine à quel stade une mutation peut apparaître
 */
@Getter
public enum MutationTier {

    TIER_1(1, 2, 30, "§7", "Basique"),
    TIER_2(3, 4, 60, "§a", "Avancée"),
    TIER_3(5, 6, 100, "§9", "Puissante"),
    TIER_4(7, 8, 150, "§5", "Supérieure"),
    TIER_5(9, 10, 250, "§6", "Ultime");

    private final int minStage;
    private final int maxStage;
    private final int insuranceCost; // Coût en gemmes pour l'assurance
    private final String color;
    private final String displayName;

    MutationTier(int minStage, int maxStage, int insuranceCost, String color, String displayName) {
        this.minStage = minStage;
        this.maxStage = maxStage;
        this.insuranceCost = insuranceCost;
        this.color = color;
        this.displayName = displayName;
    }

    /**
     * Vérifie si cette mutation peut apparaître au stade donné
     */
    public boolean isAvailableAtStage(int stage) {
        return stage >= minStage && stage <= maxStage + 2; // +2 pour permettre des mutations légèrement inférieures
    }

    /**
     * Obtient le tier correspondant à un stade
     */
    public static MutationTier getTierForStage(int stage) {
        for (MutationTier tier : values()) {
            if (stage >= tier.minStage && stage <= tier.maxStage) {
                return tier;
            }
        }
        return TIER_5; // Par défaut au max
    }

    /**
     * Retourne le nombre d'étoiles pour l'affichage
     */
    public String getStars() {
        return "⭐".repeat(ordinal() + 1);
    }
}
