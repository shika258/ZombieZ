package com.rinaorc.zombiez.progression.journey;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Représente les différentes "portes" (barrières) de progression
 * Chaque gate bloque l'accès à une fonctionnalité jusqu'à ce que le joueur
 * complète les prérequis du parcours
 */
@Getter
public enum JourneyGate {

    // ==================== ZONES ====================
    ZONE_2(GateType.ZONE, 2, "Zone 2 - Faubourgs Oubliés",
        "Complète le Chapitre 2 pour débloquer", Material.OAK_FENCE_GATE),

    ZONE_3(GateType.ZONE, 3, "Zone 3 - Champs du Silence",
        "Complète l'étape 3.4 pour débloquer", Material.OAK_FENCE_GATE),

    ZONE_4(GateType.ZONE, 4, "Zone 4 - Verger des Pendus",
        "Complète le Chapitre 4 pour débloquer", Material.SPRUCE_FENCE_GATE),

    ZONE_5(GateType.ZONE, 5, "Zone 5 - Route des Fuyards",
        "Complète le Chapitre 4 pour débloquer", Material.SPRUCE_FENCE_GATE),

    ZONE_6(GateType.ZONE, 6, "Zone 6 - Hameau Brisé",
        "Complète le Chapitre 6 pour débloquer", Material.BIRCH_FENCE_GATE),

    ZONE_7(GateType.ZONE, 7, "Zone 7 - Bois des Soupirs",
        "Complète le Chapitre 7 pour débloquer", Material.DARK_OAK_FENCE_GATE),

    ZONE_8(GateType.ZONE, 8, "Zone 8 - Ruines de Clairval",
        "Complète le Chapitre 9 pour débloquer", Material.DARK_OAK_FENCE_GATE),

    ZONE_9(GateType.ZONE, 9, "Zone 9 - Pont des Disparus",
        "Complète le Chapitre 9 pour débloquer", Material.CRIMSON_FENCE_GATE),

    ZONE_10(GateType.ZONE, 10, "Zone 10 - Avant-Poste Déserté",
        "Complète le Chapitre 10 pour débloquer", Material.CRIMSON_FENCE_GATE),

    ZONE_11(GateType.ZONE, 11, "Zone 11 - Forêt Putréfiée",
        "Complète le Chapitre 10 pour débloquer", Material.WARPED_FENCE_GATE),

    // Pour les zones avancées (12+), on les débloque par paliers
    ZONE_15(GateType.ZONE, 15, "Zones 15+",
        "Atteins le niveau 40 et complète le Chapitre 7", Material.NETHER_BRICK_FENCE),

    ZONE_20(GateType.ZONE, 20, "Zones 20+",
        "Atteins le niveau 50 et complète le Chapitre 8", Material.NETHER_BRICK_FENCE),

    ZONE_25(GateType.ZONE, 25, "Zones 25+",
        "Atteins le niveau 60 et complète le Chapitre 9", Material.IRON_BARS),

    ZONE_30(GateType.ZONE, 30, "Zones 30+",
        "Atteins le niveau 70 et complète le Chapitre 10", Material.IRON_BARS),

    ZONE_40(GateType.ZONE, 40, "Zones 40+",
        "Atteins le niveau 85 et complète le Chapitre 11", Material.CHAIN),

    ZONE_50(GateType.ZONE, 50, "Zone Finale - L'Origine",
        "Complète le Chapitre 11 et atteins le niveau 100", Material.END_PORTAL_FRAME),

    // ==================== FONCTIONNALITÉS ====================
    CLASS_SELECTION(GateType.FEATURE, 0, "Sélection de Classe",
        "Complète le Chapitre 1 (Niveau 5)", Material.NETHERITE_CHESTPLATE),

    TALENTS_TIER_1(GateType.FEATURE, 1, "Talents Tier 1",
        "Complète le Chapitre 3 (Niveau 15)", Material.ENCHANTED_BOOK),

    TALENTS_TIER_2(GateType.FEATURE, 2, "Talents Tier 2",
        "Complète le Chapitre 7 (Niveau 45)", Material.ENCHANTED_BOOK),

    TALENTS_TIER_3(GateType.FEATURE, 3, "Talents Tier 3",
        "Complète le Chapitre 8 (Niveau 55)", Material.ENCHANTED_BOOK),

    TALENTS_TIER_4(GateType.FEATURE, 4, "Talents Tier 4",
        "Complète le Chapitre 9 (Niveau 60)", Material.ENCHANTED_BOOK),

    SKILL_TREE(GateType.FEATURE, 0, "Arbre de Compétences",
        "Complète le Chapitre 5 (Niveau 30)", Material.OAK_SAPLING),

    DAILY_MISSIONS(GateType.FEATURE, 0, "Missions Quotidiennes",
        "Complète le Chapitre 4 (Niveau 25)", Material.WRITABLE_BOOK),

    WEEKLY_MISSIONS(GateType.FEATURE, 0, "Missions Hebdomadaires",
        "Complète le Chapitre 6 (Niveau 35)", Material.WRITTEN_BOOK),

    BATTLE_PASS(GateType.FEATURE, 0, "Battle Pass",
        "Complète le Chapitre 6 (Niveau 35)", Material.NETHER_STAR),

    PRESTIGE(GateType.FEATURE, 0, "Prestige",
        "Complète le Chapitre 11 (Niveau 100)", Material.ENCHANTED_GOLDEN_APPLE),

    TRADING(GateType.FEATURE, 0, "Échanges entre Joueurs",
        "Atteins le niveau 20", Material.EMERALD);

    private final GateType type;
    private final int value; // Zone ID ou Tier selon le type
    private final String displayName;
    private final String requirement;
    private final Material icon;

    JourneyGate(GateType type, int value, String displayName, String requirement, Material icon) {
        this.type = type;
        this.value = value;
        this.displayName = displayName;
        this.requirement = requirement;
        this.icon = icon;
    }

    /**
     * Obtient la gate de zone pour un ID donné
     */
    public static JourneyGate getZoneGate(int zoneId) {
        // Retourne la gate appropriée pour cette zone
        if (zoneId >= 50) return ZONE_50;
        if (zoneId >= 40) return ZONE_40;
        if (zoneId >= 30) return ZONE_30;
        if (zoneId >= 25) return ZONE_25;
        if (zoneId >= 20) return ZONE_20;
        if (zoneId >= 15) return ZONE_15;
        if (zoneId >= 11) return ZONE_11;
        if (zoneId >= 10) return ZONE_10;
        if (zoneId >= 9) return ZONE_9;
        if (zoneId >= 8) return ZONE_8;
        if (zoneId >= 7) return ZONE_7;
        if (zoneId >= 6) return ZONE_6;
        if (zoneId >= 5) return ZONE_5;
        if (zoneId >= 4) return ZONE_4;
        if (zoneId >= 3) return ZONE_3;
        if (zoneId >= 2) return ZONE_2;
        return null; // Zone 1 toujours accessible
    }

    /**
     * Obtient la gate de talent pour un tier donné
     */
    public static JourneyGate getTalentGate(int tier) {
        return switch (tier) {
            case 1 -> TALENTS_TIER_1;
            case 2 -> TALENTS_TIER_2;
            case 3 -> TALENTS_TIER_3;
            case 4 -> TALENTS_TIER_4;
            default -> null;
        };
    }

    /**
     * Types de gates
     */
    public enum GateType {
        ZONE,      // Bloque l'accès à une zone
        FEATURE    // Bloque l'accès à une fonctionnalité
    }
}
