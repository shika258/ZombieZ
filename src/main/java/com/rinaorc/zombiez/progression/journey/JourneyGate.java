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

    // ==================== ZONES (2-50) ====================
    // Débloquées par Chapitre 1 (zones 2-3)
    ZONE_2(GateType.ZONE, 2, "Zone 2", "Complète le Chapitre 1", Material.OAK_FENCE_GATE),
    ZONE_3(GateType.ZONE, 3, "Zone 3", "Complète le Chapitre 1", Material.OAK_FENCE_GATE),

    // Débloquées par Chapitre 2 (zones 4-6)
    ZONE_4(GateType.ZONE, 4, "Zone 4", "Complète le Chapitre 2", Material.OAK_FENCE_GATE),
    ZONE_5(GateType.ZONE, 5, "Zone 5", "Complète le Chapitre 2", Material.OAK_FENCE_GATE),
    ZONE_6(GateType.ZONE, 6, "Zone 6", "Complète le Chapitre 2", Material.OAK_FENCE_GATE),

    // Débloquées par Chapitre 3 (zones 7-8)
    ZONE_7(GateType.ZONE, 7, "Zone 7", "Complète le Chapitre 3", Material.SPRUCE_FENCE_GATE),
    ZONE_8(GateType.ZONE, 8, "Zone 8", "Complète le Chapitre 3", Material.SPRUCE_FENCE_GATE),

    // Débloquées par Chapitre 4 (zones 9-10)
    ZONE_9(GateType.ZONE, 9, "Zone 9", "Complète le Chapitre 4", Material.SPRUCE_FENCE_GATE),
    ZONE_10(GateType.ZONE, 10, "Zone 10", "Complète le Chapitre 4", Material.SPRUCE_FENCE_GATE),

    // Débloquées par Chapitre 5 (zones 11-12)
    ZONE_11(GateType.ZONE, 11, "Zone 11", "Complète le Chapitre 5", Material.BIRCH_FENCE_GATE),
    ZONE_12(GateType.ZONE, 12, "Zone 12", "Complète le Chapitre 5", Material.BIRCH_FENCE_GATE),

    // Débloquées par Chapitre 6 (zones 13-15)
    ZONE_13(GateType.ZONE, 13, "Zone 13", "Complète le Chapitre 6", Material.BIRCH_FENCE_GATE),
    ZONE_14(GateType.ZONE, 14, "Zone 14", "Complète le Chapitre 6", Material.BIRCH_FENCE_GATE),
    ZONE_15(GateType.ZONE, 15, "Zone 15", "Complète le Chapitre 6", Material.BIRCH_FENCE_GATE),

    // Débloquées par Chapitre 7 (zones 16-17)
    ZONE_16(GateType.ZONE, 16, "Zone 16", "Complète le Chapitre 7", Material.DARK_OAK_FENCE_GATE),
    ZONE_17(GateType.ZONE, 17, "Zone 17", "Complète le Chapitre 7", Material.DARK_OAK_FENCE_GATE),

    // Débloquées par Chapitre 8 (zones 18-20)
    ZONE_18(GateType.ZONE, 18, "Zone 18", "Complète le Chapitre 8", Material.DARK_OAK_FENCE_GATE),
    ZONE_19(GateType.ZONE, 19, "Zone 19", "Complète le Chapitre 8", Material.DARK_OAK_FENCE_GATE),
    ZONE_20(GateType.ZONE, 20, "Zone 20", "Complète le Chapitre 8", Material.DARK_OAK_FENCE_GATE),

    // Débloquées par Chapitre 9 (zones 21-22)
    ZONE_21(GateType.ZONE, 21, "Zone 21", "Complète le Chapitre 9", Material.CRIMSON_FENCE_GATE),
    ZONE_22(GateType.ZONE, 22, "Zone 22", "Complète le Chapitre 9", Material.CRIMSON_FENCE_GATE),

    // Débloquées par Chapitre 10 (zones 23-25)
    ZONE_23(GateType.ZONE, 23, "Zone 23", "Complète le Chapitre 10", Material.CRIMSON_FENCE_GATE),
    ZONE_24(GateType.ZONE, 24, "Zone 24", "Complète le Chapitre 10", Material.CRIMSON_FENCE_GATE),
    ZONE_25(GateType.ZONE, 25, "Zone 25", "Complète le Chapitre 10", Material.CRIMSON_FENCE_GATE),

    // Débloquées par Chapitre 11 (zones 26-27)
    ZONE_26(GateType.ZONE, 26, "Zone 26", "Complète le Chapitre 11", Material.WARPED_FENCE_GATE),
    ZONE_27(GateType.ZONE, 27, "Zone 27", "Complète le Chapitre 11", Material.WARPED_FENCE_GATE),

    // Débloquées par Chapitre 12 (zones 28-29)
    ZONE_28(GateType.ZONE, 28, "Zone 28", "Complète le Chapitre 12", Material.WARPED_FENCE_GATE),
    ZONE_29(GateType.ZONE, 29, "Zone 29", "Complète le Chapitre 12", Material.WARPED_FENCE_GATE),

    // Débloquées par Chapitre 13 (zones 30-32)
    ZONE_30(GateType.ZONE, 30, "Zone 30", "Complète le Chapitre 13", Material.NETHER_BRICK_FENCE),
    ZONE_31(GateType.ZONE, 31, "Zone 31", "Complète le Chapitre 13", Material.NETHER_BRICK_FENCE),
    ZONE_32(GateType.ZONE, 32, "Zone 32", "Complète le Chapitre 13", Material.NETHER_BRICK_FENCE),

    // Débloquées par Chapitre 14 (zones 33-34)
    ZONE_33(GateType.ZONE, 33, "Zone 33", "Complète le Chapitre 14", Material.NETHER_BRICK_FENCE),
    ZONE_34(GateType.ZONE, 34, "Zone 34", "Complète le Chapitre 14", Material.NETHER_BRICK_FENCE),

    // Débloquées par Chapitre 15 (zones 35-37)
    ZONE_35(GateType.ZONE, 35, "Zone 35", "Complète le Chapitre 15", Material.IRON_BARS),
    ZONE_36(GateType.ZONE, 36, "Zone 36", "Complète le Chapitre 15", Material.IRON_BARS),
    ZONE_37(GateType.ZONE, 37, "Zone 37", "Complète le Chapitre 15", Material.IRON_BARS),

    // Débloquées par Chapitre 16 (zones 38-40)
    ZONE_38(GateType.ZONE, 38, "Zone 38", "Complète le Chapitre 16", Material.IRON_BARS),
    ZONE_39(GateType.ZONE, 39, "Zone 39", "Complète le Chapitre 16", Material.IRON_BARS),
    ZONE_40(GateType.ZONE, 40, "Zone 40", "Complète le Chapitre 16", Material.IRON_BARS),

    // Débloquées par Chapitre 17 (zones 41-43)
    ZONE_41(GateType.ZONE, 41, "Zone 41", "Complète le Chapitre 17", Material.CHAIN),
    ZONE_42(GateType.ZONE, 42, "Zone 42", "Complète le Chapitre 17", Material.CHAIN),
    ZONE_43(GateType.ZONE, 43, "Zone 43", "Complète le Chapitre 17", Material.CHAIN),

    // Débloquées par Chapitre 18 (zones 44-46)
    ZONE_44(GateType.ZONE, 44, "Zone 44", "Complète le Chapitre 18", Material.CHAIN),
    ZONE_45(GateType.ZONE, 45, "Zone 45", "Complète le Chapitre 18", Material.CHAIN),
    ZONE_46(GateType.ZONE, 46, "Zone 46", "Complète le Chapitre 18", Material.CHAIN),

    // Débloquées par Chapitre 19 (zones 47-48)
    ZONE_47(GateType.ZONE, 47, "Zone 47", "Complète le Chapitre 19", Material.END_PORTAL_FRAME),
    ZONE_48(GateType.ZONE, 48, "Zone 48", "Complète le Chapitre 19", Material.END_PORTAL_FRAME),

    // Débloquées par Chapitre 20 (zones 49-50)
    ZONE_49(GateType.ZONE, 49, "Zone 49", "Complète le Chapitre 20", Material.END_PORTAL_FRAME),
    ZONE_50(GateType.ZONE, 50, "Zone 50 - L'Origine", "Complète le Chapitre 20", Material.DRAGON_EGG),

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

    PRESTIGE(GateType.FEATURE, 1, "Prestige",
        "Complète le Chapitre 11 (Niveau 100)", Material.ENCHANTED_GOLDEN_APPLE),

    PRESTIGE_2(GateType.FEATURE, 2, "Prestige 2",
        "Complète le Chapitre 12 (Niveau 125)", Material.ENCHANTED_GOLDEN_APPLE),

    PRESTIGE_3(GateType.FEATURE, 3, "Prestige 3",
        "Complète le Chapitre 16 (Niveau 190)", Material.ENCHANTED_GOLDEN_APPLE),

    PRESTIGE_4(GateType.FEATURE, 4, "Prestige 4",
        "Complète le Chapitre 19 (Niveau 250)", Material.ENCHANTED_GOLDEN_APPLE),

    TALENTS_TIER_5(GateType.FEATURE, 5, "Talents Tier 5 - Légendaires",
        "Complète le Chapitre 14 (Niveau 160)", Material.ENCHANTED_BOOK),

    DIMENSION_CORRUPTED(GateType.FEATURE, 0, "Dimension Corrompue",
        "Complète le Chapitre 18 (Niveau 225)", Material.END_PORTAL_FRAME),

    SANCTUARY(GateType.FEATURE, 0, "Sanctuaire",
        "Complète le Chapitre 20 (Niveau 275)", Material.RESPAWN_ANCHOR),

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
        // Zone 1 est toujours accessible
        if (zoneId <= 1) return null;
        // Zone 50 est le maximum
        if (zoneId > 50) return ZONE_50;

        // Retourne la gate correspondant exactement à la zone
        return switch (zoneId) {
            case 2 -> ZONE_2;
            case 3 -> ZONE_3;
            case 4 -> ZONE_4;
            case 5 -> ZONE_5;
            case 6 -> ZONE_6;
            case 7 -> ZONE_7;
            case 8 -> ZONE_8;
            case 9 -> ZONE_9;
            case 10 -> ZONE_10;
            case 11 -> ZONE_11;
            case 12 -> ZONE_12;
            case 13 -> ZONE_13;
            case 14 -> ZONE_14;
            case 15 -> ZONE_15;
            case 16 -> ZONE_16;
            case 17 -> ZONE_17;
            case 18 -> ZONE_18;
            case 19 -> ZONE_19;
            case 20 -> ZONE_20;
            case 21 -> ZONE_21;
            case 22 -> ZONE_22;
            case 23 -> ZONE_23;
            case 24 -> ZONE_24;
            case 25 -> ZONE_25;
            case 26 -> ZONE_26;
            case 27 -> ZONE_27;
            case 28 -> ZONE_28;
            case 29 -> ZONE_29;
            case 30 -> ZONE_30;
            case 31 -> ZONE_31;
            case 32 -> ZONE_32;
            case 33 -> ZONE_33;
            case 34 -> ZONE_34;
            case 35 -> ZONE_35;
            case 36 -> ZONE_36;
            case 37 -> ZONE_37;
            case 38 -> ZONE_38;
            case 39 -> ZONE_39;
            case 40 -> ZONE_40;
            case 41 -> ZONE_41;
            case 42 -> ZONE_42;
            case 43 -> ZONE_43;
            case 44 -> ZONE_44;
            case 45 -> ZONE_45;
            case 46 -> ZONE_46;
            case 47 -> ZONE_47;
            case 48 -> ZONE_48;
            case 49 -> ZONE_49;
            case 50 -> ZONE_50;
            default -> null;
        };
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
            case 5 -> TALENTS_TIER_5;
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
