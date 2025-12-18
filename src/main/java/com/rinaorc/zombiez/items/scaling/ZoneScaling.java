package com.rinaorc.zombiez.items.scaling;

/**
 * Classe utilitaire centrale pour le scaling par zone
 *
 * PHILOSOPHIE:
 * - La ZONE est le facteur PRINCIPAL de puissance (valeurs numériques)
 * - La RARETE définit la COMPLEXITE (nombre d'affixes, attributs, proc chances)
 *
 * La zone contrôle:
 * - Dégâts, DOT, AoE
 * - Chances et durées des effets
 * - Valeurs des stats
 *
 * La rareté contrôle:
 * - Nombre d'affixes
 * - Nombre d'attributs
 * - Accès aux types d'affixes/pouvoirs
 * - Chances de proc des skills
 */
public final class ZoneScaling {

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONSTANTES DE SCALING
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Nombre total de zones dans le jeu
     */
    public static final int MAX_ZONES = 50;

    /**
     * Multiplicateur linéaire par zone pour les stats
     * Zone 1: 1.0, Zone 50: 1.0 + (50 * 0.05) = 3.5
     * Réduit de 0.08 à 0.05 pour éviter l'inflation de stats
     */
    private static final double LINEAR_SCALING_PER_ZONE = 0.05;

    /**
     * Base du scaling exponentiel (pour les valeurs qui doivent croître plus vite)
     * Zone 1: 1.0^0.02 ≈ 1.0, Zone 50: 1.0 + exponential growth
     */
    private static final double EXPONENTIAL_BASE = 1.04;

    /**
     * Multiplicateur pour les dégâts des pouvoirs
     */
    private static final double POWER_DAMAGE_SCALING = 0.12;

    /**
     * Multiplicateur pour les effets (rayon, durée)
     */
    private static final double POWER_EFFECT_SCALING = 0.04;

    // ═══════════════════════════════════════════════════════════════════════════════
    // MÉTHODES DE SCALING PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Calcule le multiplicateur de zone linéaire standard
     * Utilisé pour: stats de base, valeurs d'affixes
     *
     * @param zoneId ID de la zone (1-50)
     * @return Multiplicateur (1.0 à ~5.0)
     */
    public static double getLinearMultiplier(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        return 1.0 + (safeZoneId * LINEAR_SCALING_PER_ZONE);
    }

    /**
     * Calcule le multiplicateur de zone exponentiel
     * Utilisé pour: dégâts late-game, effets puissants
     *
     * @param zoneId ID de la zone (1-50)
     * @return Multiplicateur (1.0 à ~7.0)
     */
    public static double getExponentialMultiplier(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        return Math.pow(EXPONENTIAL_BASE, safeZoneId);
    }

    /**
     * Calcule le multiplicateur hybride (linéaire + léger exponentiel)
     * Bon équilibre entre progression early et late game
     *
     * @param zoneId ID de la zone (1-50)
     * @return Multiplicateur (1.0 à ~6.0)
     */
    public static double getHybridMultiplier(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        double linear = 1.0 + (safeZoneId * 0.06);
        double exponential = Math.pow(1.02, safeZoneId);
        return linear * exponential;
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SCALING SPÉCIFIQUES
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Multiplicateur pour les stats de base des items (dégâts, armure)
     * Scaling modéré pour que les différences de tier comptent encore
     */
    public static double getBaseStatMultiplier(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        // Zone 1: 1.0, Zone 25: 2.5, Zone 50: 4.0
        return 1.0 + (safeZoneId * 0.06);
    }

    /**
     * Multiplicateur pour les valeurs d'affixes
     * Scaling modéré pour éviter l'inflation de stats
     */
    public static double getAffixMultiplier(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        // Zone 1: 1.0, Zone 25: 2.25, Zone 50: 3.5
        // Réduit de 0.08 à 0.05 pour éviter les stats absurdes en late-game
        return 1.0 + (safeZoneId * 0.05);
    }

    /**
     * Multiplicateur pour les dégâts des pouvoirs
     * Scaling agressif pour que les pouvoirs restent pertinents
     */
    public static double getPowerDamageMultiplier(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        // Zone 1: 1.0, Zone 25: 4.0, Zone 50: 7.0
        return 1.0 + (safeZoneId * POWER_DAMAGE_SCALING);
    }

    /**
     * Multiplicateur pour les effets des pouvoirs (rayon, durée)
     * Scaling modéré pour éviter des effets trop grands
     */
    public static double getPowerEffectMultiplier(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        // Zone 1: 1.0, Zone 25: 2.0, Zone 50: 3.0
        return 1.0 + (safeZoneId * POWER_EFFECT_SCALING);
    }

    /**
     * Multiplicateur pour les chances de proc des pouvoirs
     * Scaling léger car les chances sont déjà définies par rareté
     */
    public static double getPowerProcMultiplier(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        // Zone 1: 1.0, Zone 50: 1.5 (max +50%)
        return 1.0 + (safeZoneId * 0.01);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ITEM SCORE CALCULATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Calcule le score de base pour une zone
     * C'est le score minimum qu'un item de cette zone devrait avoir
     *
     * @param zoneId ID de la zone (1-50)
     * @return Score de base (100 à 15000)
     */
    public static int getBaseScoreForZone(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        // Progression exponentielle douce
        // Zone 1: ~100, Zone 10: ~500, Zone 25: ~2500, Zone 50: ~15000
        return (int) (100 * Math.pow(1.1, safeZoneId));
    }

    /**
     * Calcule le multiplicateur de score pour la zone
     * Utilisé pour pondérer l'item score selon la zone
     *
     * @param zoneId ID de la zone (1-50)
     * @return Multiplicateur (1.0 à 6.0)
     */
    public static double getScoreMultiplier(int zoneId) {
        int safeZoneId = clampZone(zoneId);
        // Zone 1: 1.0, Zone 25: 3.5, Zone 50: 6.0
        return 1.0 + (safeZoneId * 0.1);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MÉTHODES D'APPLICATION
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Applique le scaling de zone à une valeur de stat
     *
     * @param baseValue Valeur de base de la stat
     * @param zoneId ID de la zone
     * @return Valeur scalée
     */
    public static double scaleStatValue(double baseValue, int zoneId) {
        return baseValue * getAffixMultiplier(zoneId);
    }

    /**
     * Applique le scaling de zone à une valeur de dégât
     *
     * @param baseDamage Dégâts de base
     * @param zoneId ID de la zone
     * @return Dégâts scalés
     */
    public static double scaleDamage(double baseDamage, int zoneId) {
        return baseDamage * getPowerDamageMultiplier(zoneId);
    }

    /**
     * Applique le scaling de zone à un rayon
     *
     * @param baseRadius Rayon de base
     * @param zoneId ID de la zone
     * @return Rayon scalé
     */
    public static double scaleRadius(double baseRadius, int zoneId) {
        return baseRadius * getPowerEffectMultiplier(zoneId);
    }

    /**
     * Applique le scaling de zone à une durée (en ticks)
     *
     * @param baseDuration Durée de base en ticks
     * @param zoneId ID de la zone
     * @return Durée scalée en ticks
     */
    public static int scaleDuration(int baseDuration, int zoneId) {
        return (int) (baseDuration * getPowerEffectMultiplier(zoneId));
    }

    /**
     * Applique le scaling de zone à une chance de proc
     *
     * @param baseChance Chance de base (0.0 à 1.0)
     * @param zoneId ID de la zone
     * @return Chance scalée (cappée à 1.0)
     */
    public static double scaleProcChance(double baseChance, int zoneId) {
        return Math.min(1.0, baseChance * getPowerProcMultiplier(zoneId));
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Limite l'ID de zone à la plage valide
     */
    private static int clampZone(int zoneId) {
        return Math.max(1, Math.min(MAX_ZONES, zoneId));
    }

    /**
     * Obtient la progression de zone en pourcentage (0.0 à 1.0)
     */
    public static double getZoneProgress(int zoneId) {
        return (double) clampZone(zoneId) / MAX_ZONES;
    }

    /**
     * Vérifie si une zone est considérée "early game"
     */
    public static boolean isEarlyGame(int zoneId) {
        return zoneId <= 15;
    }

    /**
     * Vérifie si une zone est considérée "mid game"
     */
    public static boolean isMidGame(int zoneId) {
        return zoneId > 15 && zoneId <= 35;
    }

    /**
     * Vérifie si une zone est considérée "late game"
     */
    public static boolean isLateGame(int zoneId) {
        return zoneId > 35;
    }

    /**
     * Obtient une description de la zone pour le debug/affichage
     */
    public static String getZoneDescription(int zoneId) {
        if (isEarlyGame(zoneId)) return "Early Game";
        if (isMidGame(zoneId)) return "Mid Game";
        return "Late Game";
    }

    /**
     * Calcule un exemple de valeur pour une stat donnée
     * Utile pour le debug et l'équilibrage
     *
     * @param baseMin Valeur min de base
     * @param baseMax Valeur max de base
     * @param zoneId Zone pour le calcul
     * @return String formatée "[min - max]"
     */
    public static String getScaledRangeExample(double baseMin, double baseMax, int zoneId) {
        double mult = getAffixMultiplier(zoneId);
        return String.format("[%.1f - %.1f]", baseMin * mult, baseMax * mult);
    }

    // Empêcher l'instanciation
    private ZoneScaling() {}
}
