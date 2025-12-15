package com.rinaorc.zombiez.zombies.types;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Types de zombies disponibles dans ZombieZ
 * Chaque type est lié à un mob MythicMobs
 */
@Getter
public enum ZombieType {

    // ═══════════════════════════════════════════════════════════════════
    // TIER 1 - Les Débutants (Jeux de mots faciles)
    // ═══════════════════════════════════════════════════════════════════
    WALKER("ZZ_Walker", "Mortpiné", 1, 25, 4, 0.18,               // Mort + traîner pieds
        new int[]{1, 2, 3, 4, 5, 6, 7}, ZombieCategory.BASIC),

    CRAWLER("ZZ_Crawler", "Rampitoyable", 1, 15, 3, 0.28,         // Ramper + pitoyable
        new int[]{1, 2, 3, 4, 5, 6, 7}, ZombieCategory.BASIC),

    RUNNER("ZZ_Runner", "Sprintomb", 1, 20, 5, 0.32,              // Sprint + tombe
        new int[]{2, 3, 4, 5, 6, 7}, ZombieCategory.BASIC),

    SHAMBLER("ZZ_Shambler", "Titubeurk", 1, 35, 3, 0.14,          // Tituber + beurk
        new int[]{1, 2, 3}, ZombieCategory.BASIC),

    MUMMY("ZZ_Mummy", "Bandelétale", 1, 30, 5, 0.16,              // Bandelette + létale
        new int[]{3}, ZombieCategory.BASIC),

    DROWNER("ZZ_Drowner", "Flottacide", 1, 25, 4, 0.22,           // Flotter + homicide
        new int[]{5}, ZombieCategory.BASIC),

    // ═══════════════════════════════════════════════════════════════════
    // TIER 2 - Les Intermédiaires (Jeux de mots élaborés)
    // ═══════════════════════════════════════════════════════════════════
    ARMORED("ZZ_Armored", "Blindépouille", 2, 50, 6, 0.14,        // Blindé + dépouille
        new int[]{3, 4, 5, 6, 7, 8, 9}, ZombieCategory.TANK),

    ARMORED_ELITE("ZZ_Armored_Elite", "Cuirassassin", 2, 80, 8, 0.14,  // Cuirasse + assassin
        new int[]{5, 6, 7, 8, 9}, ZombieCategory.ELITE),

    SPITTER("ZZ_Spitter", "Glaviotoxik", 2, 30, 3, 0.20,          // Glaviot + toxique
        new int[]{3, 4, 5, 6, 7, 8, 9}, ZombieCategory.RANGED),

    SCREAMER("ZZ_Screamer", "Criardagonie", 2, 25, 4, 0.22,       // Criard + agonie
        new int[]{3, 4, 5, 6, 7, 8}, ZombieCategory.SUPPORT),

    LURKER("ZZ_Lurker", "Guettombie", 2, 28, 8, 0.26,             // Guetter + zombie
        new int[]{4, 5, 6, 7, 8}, ZombieCategory.STEALTH),

    BLOATER("ZZ_Bloater", "Gonflétide", 2, 45, 2, 0.12,           // Gonflé + fétide
        new int[]{5, 6, 7}, ZombieCategory.EXPLOSIVE),

    SHADOW("ZZ_Shadow", "Ombrévenant", 2, 32, 7, 0.28,            // Ombre + revenant
        new int[]{4, 5, 6, 7, 8}, ZombieCategory.STEALTH),

    TOXIC("ZZ_Toxic", "Putrescent", 2, 35, 4, 0.18,               // Putrescence
        new int[]{5, 6, 7}, ZombieCategory.HAZARD),

    // ═══════════════════════════════════════════════════════════════════
    // TIER 3 - Les Dangereux (Jeux de mots mémorables)
    // ═══════════════════════════════════════════════════════════════════
    BERSERKER("ZZ_Berserker", "Rageputride", 3, 60, 8, 0.24,      // Rage + putride
        new int[]{5, 6, 7, 8, 9, 10}, ZombieCategory.MELEE),

    NECROMANCER("ZZ_Necromancer", "Nécrosorcier", 3, 45, 5, 0.16, // Nécro + sorcier
        new int[]{5, 6, 7, 8, 9, 10}, ZombieCategory.SUMMONER),

    EXPLOSIVE("ZZ_Explosive", "Kaboombie", 3, 35, 3, 0.30,        // Kaboom + zombie
        new int[]{5, 6, 7, 8, 9, 10}, ZombieCategory.EXPLOSIVE),

    GIANT("ZZ_Giant", "Mastodonte", 3, 120, 15, 0.10,             // Mastodonte (géant préhistorique)
        new int[]{6, 7, 8, 9}, ZombieCategory.TANK),

    CLIMBER("ZZ_Climber", "Escaladavre", 3, 40, 7, 0.28,          // Escalade + cadavre
        new int[]{7, 8}, ZombieCategory.MELEE),

    FROZEN("ZZ_Frozen", "Frigorifique", 3, 50, 6, 0.16,           // Frigorifique (mort + froid)
        new int[]{8}, ZombieCategory.ELEMENTAL),

    YETI("ZZ_Yeti", "Abominaneige", 3, 80, 12, 0.22,              // Abominable + neige
        new int[]{8}, ZombieCategory.ELITE),

    WENDIGO("ZZ_Wendigo", "Affamortis", 3, 70, 14, 0.32,          // Affamé + mortis
        new int[]{8}, ZombieCategory.ELITE),

    // ═══════════════════════════════════════════════════════════════════
    // TIER 4 - Les Terrifiants (Jeux de mots épiques)
    // ═══════════════════════════════════════════════════════════════════
    COLOSSUS("ZZ_Colossus", "Titanécrose", 4, 150, 18, 0.08,      // Titan + nécrose
        new int[]{7, 8, 9, 10}, ZombieCategory.TANK),

    SPECTRE("ZZ_Spectre", "Spectramort", 4, 60, 12, 0.30,         // Spectre + mort
        new int[]{7, 8, 9, 10}, ZombieCategory.STEALTH),

    RAVAGER("ZZ_Ravager", "Dévastateur", 4, 100, 16, 0.28,        // Dévastateur
        new int[]{7, 8, 9, 10}, ZombieCategory.MELEE),

    CREAKING("ZZ_Creaking", "Craquosseux", 4, 80, 14, 0.05,       // Craquer + os
        new int[]{9}, ZombieCategory.SPECIAL),

    MUTANT("ZZ_Mutant", "Mutanomalie", 4, 90, 13, 0.24,           // Mutant + anomalie
        new int[]{9}, ZombieCategory.HAZARD),

    // ═══════════════════════════════════════════════════════════════════
    // TIER 5 - Les Légendaires (Jeux de mots majestueux)
    // ═══════════════════════════════════════════════════════════════════
    CORRUPTED_WARDEN("ZZ_CorruptedWarden", "Gardinfernal", 5, 250, 25, 0.22,  // Gardien + infernal
        new int[]{9, 10}, ZombieCategory.ELITE),

    ARCHON("ZZ_Archon", "Archonécros", 5, 200, 22, 0.26,          // Archon + nécros
        new int[]{10}, ZombieCategory.ELITE),

    DEMON("ZZ_Demon", "Démonécrose", 4, 70, 12, 0.28,             // Démon + nécrose
        new int[]{10}, ZombieCategory.ELEMENTAL),

    INFERNAL("ZZ_Infernal", "Brasimort", 4, 100, 15, 0.24,        // Brasier + mort
        new int[]{10}, ZombieCategory.ELITE),

    // ═══════════════════════════════════════════════════════════════════
    // MINI-BOSS - Les Redoutés (Noms épiques avec titres)
    // ═══════════════════════════════════════════════════════════════════
    BUTCHER("ZZ_Butcher", "L'Équarisseur", 0, 200, 12, 0.20,      // Boucher -> Équarisseur
        new int[]{3, 4, 5, 6, 7, 8, 9, 10}, ZombieCategory.MINIBOSS),

    WIDOW("ZZ_Widow", "La Veuvénéneuse", 0, 180, 10, 0.28,        // Veuve + venimeuse
        new int[]{4, 5, 6, 7, 8, 9, 10}, ZombieCategory.MINIBOSS),

    THE_GIANT("ZZ_TheGiant", "Le Colossal", 0, 300, 20, 0.08,     // Le Colossal
        new int[]{5, 6, 7, 8, 9, 10}, ZombieCategory.MINIBOSS),

    THE_PHANTOM("ZZ_ThePhantom", "L'Éthéré", 0, 150, 14, 0.32,    // L'Éthéré (fantomatique)
        new int[]{6, 7, 8, 9, 10}, ZombieCategory.MINIBOSS),

    // ═══════════════════════════════════════════════════════════════════
    // BOSS DE ZONE - Les Seigneurs (Titres épiques)
    // ═══════════════════════════════════════════════════════════════════
    BOSS_GUARDIAN("ZZ_Boss_Guardian", "Le Gardien Maudit", 0, 300, 10, 0.18,
        new int[]{1}, ZombieCategory.ZONE_BOSS),

    BOSS_SHADOW_ELDER("ZZ_Boss_ShadowElder", "L'Ancien des Ténèbres", 0, 500, 15, 0.22,
        new int[]{4}, ZombieCategory.ZONE_BOSS),

    BOSS_FROST_LORD("ZZ_Boss_FrostLord", "Le Seigneur Givré", 0, 800, 20, 0.16,
        new int[]{8}, ZombieCategory.ZONE_BOSS),

    BOSS_ABOMINATION("ZZ_Boss_Abomination", "L'Innommable", 0, 1200, 25, 0.20,
        new int[]{9}, ZombieCategory.ZONE_BOSS),

    // ═══════════════════════════════════════════════════════════════════
    // BOSS FINAL - Le Mythe (Nom légendaire)
    // ═══════════════════════════════════════════════════════════════════
    PATIENT_ZERO("ZZ_PatientZero", "Patient Zéro - L'Origine", 0, 5000, 30, 0.24,
        new int[]{11}, ZombieCategory.FINAL_BOSS);

    private final String mythicMobId;    // ID MythicMobs
    private final String displayName;    // Nom d'affichage
    private final int tier;              // Tier de puissance (1-5, 0 pour boss)
    private final double baseHealth;     // Vie de base
    private final double baseDamage;     // Dégâts de base
    private final double baseSpeed;      // Vitesse de base
    private final int[] validZones;      // Zones où ce zombie peut spawn
    private final ZombieCategory category;

    ZombieType(String mythicMobId, String displayName, int tier, double baseHealth,
               double baseDamage, double baseSpeed, int[] validZones, ZombieCategory category) {
        this.mythicMobId = mythicMobId;
        this.displayName = displayName;
        this.tier = tier;
        this.baseHealth = baseHealth;
        this.baseDamage = baseDamage;
        this.baseSpeed = baseSpeed;
        this.validZones = validZones;
        this.category = category;
    }

    /**
     * Vérifie si ce zombie peut spawn dans une zone
     */
    public boolean canSpawnInZone(int zoneId) {
        for (int zone : validZones) {
            if (zone == zoneId) return true;
        }
        return false;
    }

    /**
     * Calcule la vie pour un niveau donné
     */
    public double calculateHealth(int level) {
        return baseHealth + (level * 5 * tier);
    }

    /**
     * Calcule les dégâts pour un niveau donné
     */
    public double calculateDamage(int level) {
        return baseDamage + (level * 1 * Math.max(1, tier));
    }

    /**
     * Obtient les points de récompense de base
     */
    public int getBasePoints() {
        return switch (category) {
            case BASIC -> 5;
            case TANK, MELEE -> 8;
            case RANGED, SUPPORT -> 7;
            case STEALTH -> 10;
            case EXPLOSIVE, HAZARD -> 9;
            case SUMMONER -> 12;
            case ELEMENTAL, SPECIAL -> 11;
            case ELITE -> 25;
            case MINIBOSS -> 100;
            case ZONE_BOSS -> 500;
            case FINAL_BOSS -> 2000;
        };
    }

    /**
     * Obtient l'XP de base
     */
    public int getBaseXP() {
        return getBasePoints() * 2;
    }

    /**
     * Vérifie si c'est un boss
     */
    public boolean isBoss() {
        return category == ZombieCategory.MINIBOSS || 
               category == ZombieCategory.ZONE_BOSS ||
               category == ZombieCategory.FINAL_BOSS;
    }

    /**
     * Obtient un type aléatoire pour une zone
     */
    public static ZombieType randomForZone(int zoneId) {
        List<ZombieType> valid = Arrays.stream(values())
            .filter(t -> t.canSpawnInZone(zoneId))
            .filter(t -> !t.isBoss())
            .toList();
        
        if (valid.isEmpty()) return WALKER;
        return valid.get((int) (Math.random() * valid.size()));
    }

    /**
     * Obtient tous les types d'un tier
     */
    public static List<ZombieType> getByTier(int tier) {
        return Arrays.stream(values())
            .filter(t -> t.tier == tier)
            .toList();
    }

    /**
     * Obtient un type depuis son ID MythicMobs
     */
    public static ZombieType fromMythicId(String mythicId) {
        for (ZombieType type : values()) {
            if (type.mythicMobId.equals(mythicId)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Catégories de zombies
     */
    public enum ZombieCategory {
        BASIC,       // Zombies de base
        TANK,        // Résistants, lents
        MELEE,       // Dégâts au corps à corps
        RANGED,      // Attaques à distance
        SUPPORT,     // Soutien (appel de renforts)
        STEALTH,     // Furtifs
        EXPLOSIVE,   // Explosent à la mort
        HAZARD,      // Zones de danger
        SUMMONER,    // Invocateurs
        ELEMENTAL,   // Pouvoirs élémentaires
        SPECIAL,     // Mécaniques spéciales
        ELITE,       // Élites très dangereux
        MINIBOSS,    // Mini-boss
        ZONE_BOSS,   // Boss de zone
        FINAL_BOSS   // Boss final
    }
}
