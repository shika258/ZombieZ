package com.rinaorc.zombiez.items.awaken;

import lombok.Getter;

/**
 * Types de modificateurs d'éveil
 *
 * Chaque type définit un effet générique qui peut être appliqué à un talent.
 * Les éveils sont générés à partir de templates basés sur ces types.
 *
 * BUDGET D'ÉQUILIBRAGE:
 * - Chaque type a un budget de puissance de 1.0 (100%)
 * - Les valeurs sont ajustées pour équilibrer l'impact
 */
@Getter
public enum AwakenModifierType {

    // ==================== INVOCATIONS ====================
    /**
     * +N invocations supplémentaires (bêtes, clones, projectiles)
     * Budget: +1 = 100%, +2 = rare
     */
    EXTRA_SUMMON(
        "Invocation Supplémentaire",
        "Invoque %s créature(s) supplémentaire(s)",
        1.0,
        1.0, 2.0 // min, max
    ),

    // ==================== DÉGÂTS ====================
    /**
     * +X% dégâts du talent
     * Budget: +25% = 100%
     */
    DAMAGE_BONUS(
        "Dégâts Amplifiés",
        "+%.0f%% de dégâts",
        1.0,
        15.0, 35.0 // 15% à 35%
    ),

    /**
     * +X% dégâts critiques du talent
     * Budget: +40% = 100%
     */
    CRIT_DAMAGE_BONUS(
        "Coups Dévastateurs",
        "+%.0f%% de dégâts critiques",
        1.0,
        25.0, 50.0
    ),

    /**
     * +X% chance de critique du talent
     * Budget: +15% = 100%
     */
    CRIT_CHANCE_BONUS(
        "Précision Fatale",
        "+%.0f%% de chance de critique",
        1.0,
        10.0, 20.0
    ),

    // ==================== COOLDOWNS ====================
    /**
     * -X% cooldown du talent
     * Budget: -20% = 100%
     */
    COOLDOWN_REDUCTION(
        "Récupération Accélérée",
        "-%.0f%% de temps de recharge",
        1.0,
        15.0, 25.0 // réduction de 15% à 25%
    ),

    // ==================== DURÉE ====================
    /**
     * +X% durée des effets du talent
     * Budget: +30% = 100%
     */
    DURATION_EXTENSION(
        "Effet Prolongé",
        "+%.0f%% de durée",
        1.0,
        20.0, 40.0
    ),

    // ==================== ZONE ====================
    /**
     * +X% rayon/zone d'effet
     * Budget: +25% = 100%
     */
    RADIUS_BONUS(
        "Zone Étendue",
        "+%.0f%% de rayon",
        1.0,
        15.0, 35.0
    ),

    // ==================== PROJECTILES ====================
    /**
     * +N projectiles supplémentaires
     * Budget: +1 = 100%, +2 = rare
     */
    EXTRA_PROJECTILE(
        "Projectiles Supplémentaires",
        "+%s projectile(s)",
        1.0,
        1.0, 2.0
    ),

    /**
     * +N rebonds supplémentaires
     * Budget: +1 = 100%
     */
    EXTRA_BOUNCE(
        "Rebonds Supplémentaires",
        "+%s rebond(s)",
        1.0,
        1.0, 2.0
    ),

    // ==================== EFFETS SPÉCIAUX ====================
    /**
     * +X% chance de proc d'un effet secondaire
     * Budget: +20% = 100%
     */
    PROC_CHANCE_BONUS(
        "Déclenchement Fréquent",
        "+%.0f%% de chance d'activation",
        1.0,
        15.0, 25.0
    ),

    /**
     * +X stacks/charges supplémentaires générées
     * Budget: +2 = 100%
     */
    EXTRA_STACKS(
        "Accumulation Accrue",
        "+%s stack(s) généré(s)",
        1.0,
        1.0, 3.0
    ),

    /**
     * -X stacks/charges requis pour déclencher
     * Budget: -1 = 100%
     */
    REDUCED_THRESHOLD(
        "Seuil Réduit",
        "-%s stack(s) requis",
        1.0,
        1.0, 2.0
    ),

    // ==================== DEBUFFS ====================
    /**
     * Applique un debuff léger en plus
     * Budget: Slow I 2s = 100%
     */
    APPLY_SLOW(
        "Entrave",
        "Ralentit les cibles %.0fs",
        0.8,
        1.5, 2.5 // durée en secondes
    ),

    /**
     * Applique une vulnérabilité
     * Budget: +10% dégâts subis 3s = 100%
     */
    APPLY_VULNERABILITY(
        "Vulnérabilité",
        "Cibles vulnérables (+%.0f%% dégâts subis)",
        0.9,
        8.0, 15.0 // % bonus dégâts
    ),

    // ==================== BUFFS JOUEUR ====================
    /**
     * Bonus de vitesse temporaire au joueur
     * Budget: +20% speed 3s = 100%
     */
    SPEED_BUFF(
        "Célérité",
        "+%.0f%% vitesse pendant 3s",
        0.7,
        15.0, 25.0
    ),

    /**
     * Soin à l'activation/proc du talent
     * Budget: 5% max HP = 100%
     */
    HEAL_ON_PROC(
        "Vitalité",
        "Soigne %.0f%% PV à l'activation",
        0.8,
        3.0, 7.0 // % des PV max
    ),

    /**
     * Bouclier/absorption temporaire
     * Budget: 10% max HP shield 5s = 100%
     */
    SHIELD_ON_PROC(
        "Protection",
        "Bouclier de %.0f%% PV pendant 5s",
        0.9,
        8.0, 15.0 // % des PV max
    ),

    // ==================== UTILITAIRES ====================
    /**
     * +X% XP bonus sur les kills affectés par le talent
     * Budget: +15% = 100%
     */
    XP_BONUS(
        "Sagesse",
        "+%.0f%% XP sur les kills",
        0.6,
        10.0, 20.0
    ),

    /**
     * +X% loot bonus sur les kills affectés par le talent
     * Budget: +10% = 100%
     */
    LOOT_BONUS(
        "Fortune",
        "+%.0f%% loot sur les kills",
        0.6,
        8.0, 15.0
    ),

    // ==================== SPÉCIAUX (UNIQUES PAR TALENT) ====================
    /**
     * Effet unique spécifique au talent
     * Les valeurs et descriptions sont définies par talent
     */
    UNIQUE_EFFECT(
        "Éveil Unique",
        "%s",
        1.0,
        1.0, 1.0 // Valeur fixe, le talent définit l'effet
    );

    private final String displayName;
    private final String descriptionFormat; // Utilise %s ou %.0f pour les valeurs
    private final double budgetWeight; // 1.0 = standard, <1 = faible impact
    private final double minValue;
    private final double maxValue;

    AwakenModifierType(String displayName, String descriptionFormat,
                       double budgetWeight, double minValue, double maxValue) {
        this.displayName = displayName;
        this.descriptionFormat = descriptionFormat;
        this.budgetWeight = budgetWeight;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Formate la description avec la valeur
     */
    public String formatDescription(double value) {
        if (descriptionFormat.contains("%.0f")) {
            return String.format(descriptionFormat, value);
        } else {
            return String.format(descriptionFormat, (int) value);
        }
    }

    /**
     * Génère une valeur aléatoire dans la plage
     */
    public double rollValue() {
        return minValue + Math.random() * (maxValue - minValue);
    }

    /**
     * Génère une valeur avec bonus de qualité (0.0 à 0.3)
     */
    public double rollValue(double qualityBonus) {
        double base = rollValue();
        // Le bonus de qualité peut pousser légèrement au-dessus du max
        return base * (1.0 + qualityBonus * 0.2);
    }

    /**
     * Vérifie si ce type utilise des valeurs entières
     */
    public boolean isIntegerValue() {
        return this == EXTRA_SUMMON || this == EXTRA_PROJECTILE ||
               this == EXTRA_BOUNCE || this == EXTRA_STACKS ||
               this == REDUCED_THRESHOLD;
    }
}
