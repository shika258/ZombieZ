package com.rinaorc.zombiez.classes;

import lombok.Getter;

/**
 * Système de calcul de stats avec soft caps et diminishing returns
 *
 * PHILOSOPHIE:
 * - Les premiers % sont toujours efficaces (reward early investment)
 * - Au-delà du soft cap, rendements décroissants (prevent stacking abuse)
 * - Hard cap absolu pour éviter les builds cassés
 *
 * FORMULE DIMINISHING RETURNS:
 * effectiveValue = softCap + (rawValue - softCap) * diminishingFactor
 * où diminishingFactor = 1 / (1 + (rawValue - softCap) / softCap)
 */
@Getter
public class StatCalculator {

    // ==================== SOFT CAPS ====================
    // Au-delà de ces valeurs, diminishing returns s'appliquent

    public static final double LIFESTEAL_SOFT_CAP = 15.0;      // 15% efficace à 100%
    public static final double LIFESTEAL_HARD_CAP = 30.0;      // Maximum absolu

    public static final double DAMAGE_REDUCTION_SOFT_CAP = 20.0;
    public static final double DAMAGE_REDUCTION_HARD_CAP = 50.0;

    public static final double CDR_SOFT_CAP = 25.0;
    public static final double CDR_HARD_CAP = 40.0;            // Réduit de 50%
    public static final double ULTIMATE_CDR_CAP = 15.0;        // Les ultimes ont un cap séparé

    public static final double CRIT_CHANCE_SOFT_CAP = 40.0;
    public static final double CRIT_CHANCE_HARD_CAP = 75.0;

    public static final double CRIT_DAMAGE_SOFT_CAP = 100.0;   // +100% = x2
    public static final double CRIT_DAMAGE_HARD_CAP = 200.0;   // +200% = x3 max

    public static final double REGEN_SOFT_CAP = 30.0;
    public static final double REGEN_HARD_CAP = 60.0;

    public static final double SPEED_SOFT_CAP = 20.0;
    public static final double SPEED_HARD_CAP = 35.0;

    public static final double DODGE_SOFT_CAP = 15.0;
    public static final double DODGE_HARD_CAP = 30.0;

    public static final double DAMAGE_BONUS_SOFT_CAP = 50.0;   // +50% dégâts
    public static final double DAMAGE_BONUS_HARD_CAP = 100.0;  // +100% max

    // ==================== BUFF CATEGORY CAPS ====================
    // Limite le nombre de buffs par catégorie pour forcer la diversité

    public static final int MAX_OFFENSE_BUFF_STACKS = 15;
    public static final int MAX_DEFENSE_BUFF_STACKS = 15;
    public static final int MAX_UTILITY_BUFF_STACKS = 15;

    // ==================== CALCULS ====================

    /**
     * Applique les diminishing returns sur une valeur brute
     *
     * @param rawValue Valeur brute (somme de tous les bonus)
     * @param softCap  Seuil où les diminishing returns commencent
     * @param hardCap  Valeur maximum absolue
     * @return Valeur effective après diminishing returns
     */
    public static double applyDiminishingReturns(double rawValue, double softCap, double hardCap) {
        if (rawValue <= 0) return 0;
        if (rawValue <= softCap) return rawValue;

        // Formule: softCap + (excess * diminishingFactor)
        double excess = rawValue - softCap;
        double diminishingFactor = softCap / (softCap + excess);
        double effectiveExcess = excess * diminishingFactor;

        double result = softCap + effectiveExcess;
        return Math.min(result, hardCap);
    }

    /**
     * Calcule le vol de vie effectif
     */
    public static double calculateEffectiveLifesteal(double rawLifesteal) {
        return applyDiminishingReturns(rawLifesteal, LIFESTEAL_SOFT_CAP, LIFESTEAL_HARD_CAP);
    }

    /**
     * Calcule la réduction de dégâts effective
     */
    public static double calculateEffectiveDamageReduction(double rawReduction) {
        return applyDiminishingReturns(rawReduction, DAMAGE_REDUCTION_SOFT_CAP, DAMAGE_REDUCTION_HARD_CAP);
    }

    /**
     * Calcule la réduction de cooldown effective
     * Note: Les ultimes ont leur propre cap
     */
    public static double calculateEffectiveCDR(double rawCDR, boolean isUltimate) {
        if (isUltimate) {
            return Math.min(rawCDR, ULTIMATE_CDR_CAP);
        }
        return applyDiminishingReturns(rawCDR, CDR_SOFT_CAP, CDR_HARD_CAP);
    }

    /**
     * Calcule la chance de critique effective
     */
    public static double calculateEffectiveCritChance(double rawCrit) {
        return applyDiminishingReturns(rawCrit, CRIT_CHANCE_SOFT_CAP, CRIT_CHANCE_HARD_CAP);
    }

    /**
     * Calcule les dégâts critiques effectifs
     */
    public static double calculateEffectiveCritDamage(double rawCritDmg) {
        return applyDiminishingReturns(rawCritDmg, CRIT_DAMAGE_SOFT_CAP, CRIT_DAMAGE_HARD_CAP);
    }

    /**
     * Calcule la régénération effective
     */
    public static double calculateEffectiveRegen(double rawRegen) {
        return applyDiminishingReturns(rawRegen, REGEN_SOFT_CAP, REGEN_HARD_CAP);
    }

    /**
     * Calcule la vitesse effective
     */
    public static double calculateEffectiveSpeed(double rawSpeed) {
        return applyDiminishingReturns(rawSpeed, SPEED_SOFT_CAP, SPEED_HARD_CAP);
    }

    /**
     * Calcule l'esquive effective
     */
    public static double calculateEffectiveDodge(double rawDodge) {
        return applyDiminishingReturns(rawDodge, DODGE_SOFT_CAP, DODGE_HARD_CAP);
    }

    /**
     * Calcule le bonus de dégâts effectif (additif)
     */
    public static double calculateEffectiveDamageBonus(double rawBonus) {
        return applyDiminishingReturns(rawBonus, DAMAGE_BONUS_SOFT_CAP, DAMAGE_BONUS_HARD_CAP);
    }

    // ==================== UTILITAIRES D'AFFICHAGE ====================

    /**
     * Génère un texte montrant la valeur effective vs brute
     */
    public static String formatStatWithCap(String statName, double rawValue, double effectiveValue) {
        if (Math.abs(rawValue - effectiveValue) < 0.1) {
            return String.format("§7%s: §f%.1f%%", statName, effectiveValue);
        }
        return String.format("§7%s: §f%.1f%% §8(brut: %.1f%%)", statName, effectiveValue, rawValue);
    }

    /**
     * Vérifie si une valeur est au soft cap
     */
    public static boolean isAtSoftCap(double value, double softCap) {
        return value >= softCap;
    }

    /**
     * Vérifie si une valeur est au hard cap
     */
    public static boolean isAtHardCap(double value, double hardCap) {
        return value >= hardCap - 0.1;
    }

    /**
     * Calcule l'efficacité marginale d'un point de stat supplémentaire
     * Utile pour guider les choix du joueur
     */
    public static double getMarginalEfficiency(double currentRaw, double softCap) {
        if (currentRaw <= softCap) return 1.0;

        // Au-delà du soft cap, chaque point vaut moins
        double excess = currentRaw - softCap;
        return softCap / (softCap + excess + 1);
    }

    /**
     * Génère un indicateur visuel de l'efficacité
     */
    public static String getEfficiencyIndicator(double efficiency) {
        if (efficiency >= 0.9) return "§a●●●"; // Excellent
        if (efficiency >= 0.6) return "§e●●○"; // Bon
        if (efficiency >= 0.3) return "§6●○○"; // Faible
        return "§c○○○"; // Très faible
    }
}
