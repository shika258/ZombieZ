package com.rinaorc.zombiez.zombies.types;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * Types de zombies disponibles dans ZombieZ
 * Chaque type a ses propres stats, comportement et IA personnalisée
 */
@Getter
public enum ZombieType {

    // ═══════════════════════════════════════════════════════════════════
    // TIER 1 - Les Débutants (Jeux de mots faciles) - Zones 1-20
    // ═══════════════════════════════════════════════════════════════════
    WALKER("ZZ_Walker", "Mortpiné", 1, 25, 4, 0.18,               // Mort + traîner pieds
        new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 26}, ZombieCategory.BASIC),

    CRAWLER("ZZ_Crawler", "Rampitoyable", 1, 15, 3, 0.28,         // Ramper + pitoyable
        new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, ZombieCategory.BASIC),

    RUNNER("ZZ_Runner", "Sprintomb", 1, 20, 5, 0.32,              // Sprint + tombe
        new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26}, ZombieCategory.BASIC),

    SHAMBLER("ZZ_Shambler", "Titubeurk", 1, 35, 3, 0.14,          // Tituber + beurk
        new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, ZombieCategory.BASIC),

    MUMMY("ZZ_Mummy", "Bandelétale", 1, 30, 5, 0.16,              // Bandelette + létale
        new int[]{8, 9, 10, 11, 12, 13, 14, 15}, ZombieCategory.BASIC),

    DROWNER("ZZ_Drowner", "Flottacide", 1, 25, 4, 0.22,           // Flotter + homicide
        new int[]{3, 4, 5, 6, 7, 8, 9, 13, 14, 34, 35}, ZombieCategory.BASIC),

    HUSK("ZZ_Husk", "Desséchombie", 1, 30, 5, 0.20,               // Desséché + zombie
        new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 16, 17, 21, 22, 23, 24}, ZombieCategory.BASIC),

    DROWNED("ZZ_Drowned", "Noyatroz", 1, 28, 5, 0.24,             // Noyé + atroce
        new int[]{3, 4, 5, 6, 7, 8, 9, 13, 14, 34, 35, 36, 37}, ZombieCategory.BASIC),

    // ═══════════════════════════════════════════════════════════════════
    // NOUVEAUX MOBS - Variété du bestiaire
    // ═══════════════════════════════════════════════════════════════════
    SKELETON("ZZ_Skeleton", "Ossécuté", 1, 22, 4, 0.24,           // Os + exécuté
        new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40}, ZombieCategory.SKELETON),

    STRAY("ZZ_Stray", "Glaçosseux", 2, 28, 5, 0.22,               // Glacé + osseux
        new int[]{31, 32, 33, 34, 35, 36, 37, 38}, ZombieCategory.SKELETON),

    RABID_WOLF("ZZ_RabidWolf", "Croc Enragé", 2, 35, 8, 0.34,     // Loup enragé
        new int[]{5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 25, 30, 31, 32, 33, 35, 36}, ZombieCategory.PACK),

    // ═══════════════════════════════════════════════════════════════════
    // TIER 2 - Les Intermédiaires (Jeux de mots élaborés) - Zones 6-30
    // ═══════════════════════════════════════════════════════════════════
    ARMORED("ZZ_Armored", "Blindépouille", 2, 50, 6, 0.14,        // Blindé + dépouille
        new int[]{6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30}, ZombieCategory.TANK),

    ARMORED_ELITE("ZZ_Armored_Elite", "Cuirassassin", 2, 80, 8, 0.14,  // Cuirasse + assassin
        new int[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30}, ZombieCategory.ELITE),

    SPITTER("ZZ_Spitter", "Glaviotoxik", 2, 30, 3, 0.20,          // Glaviot + toxique
        new int[]{13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 43, 44, 47}, ZombieCategory.RANGED),

    SCREAMER("ZZ_Screamer", "Criardagonie", 2, 25, 4, 0.22,       // Criard + agonie
        new int[]{8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 32}, ZombieCategory.SUPPORT),

    LURKER("ZZ_Lurker", "Guettombie", 2, 28, 8, 0.26,             // Guetter + zombie
        new int[]{7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 42}, ZombieCategory.STEALTH),

    BLOATER("ZZ_Bloater", "Gonflétide", 2, 45, 2, 0.12,           // Gonflé + fétide
        new int[]{13, 14, 15, 16, 17, 18, 19, 20, 25, 44, 47}, ZombieCategory.EXPLOSIVE),

    SHADOW("ZZ_Shadow", "Ombrévenant", 2, 32, 7, 0.28,            // Ombre + revenant
        new int[]{7, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 42}, ZombieCategory.STEALTH),

    TOXIC("ZZ_Toxic", "Putrescent", 2, 35, 4, 0.18,               // Putrescence
        new int[]{11, 13, 14, 15, 25}, ZombieCategory.HAZARD),

    ZOMBIE_VILLAGER("ZZ_ZombieVillager", "Villagroin", 2, 35, 5, 0.18,  // Villageois + groin
        new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20}, ZombieCategory.SUPPORT),

    DROWNED_TRIDENT("ZZ_DrownedTrident", "Tridentombie", 2, 40, 7, 0.20, // Trident + zombie
        new int[]{13, 14, 34, 35, 36, 37, 38}, ZombieCategory.RANGED),

    PILLAGER("ZZ_Pillager", "Pillardécès", 2, 35, 6, 0.24,        // Pillard + décès
        new int[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30}, ZombieCategory.ILLAGER),

    VINDICATOR("ZZ_Vindicator", "Vengeosseur", 3, 45, 10, 0.26,   // Vengeur + fosseur
        new int[]{12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 35, 40}, ZombieCategory.ILLAGER),

    EVOKER("ZZ_Evoker", "Invocatrépas", 3, 50, 5, 0.20,           // Invocateur + trépas
        new int[]{15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 28, 30, 35, 38, 39, 40, 43, 45, 46, 48}, ZombieCategory.ILLAGER),

    CREEPER("ZZ_Creeper", "Explosécateur", 2, 20, 3, 0.22,        // Explosif + exécuteur (très rare)
        new int[]{15, 18, 20, 23, 25, 28, 30, 33, 35, 38, 40}, ZombieCategory.EXPLOSIVE),

    // ═══════════════════════════════════════════════════════════════════
    // TIER 3 - Les Dangereux (Jeux de mots mémorables) - Zones 12-40
    // ═══════════════════════════════════════════════════════════════════
    BERSERKER("ZZ_Berserker", "Rageputride", 3, 60, 8, 0.24,      // Rage + putride
        new int[]{12, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30}, ZombieCategory.MELEE),

    NECROMANCER("ZZ_Necromancer", "Nécrosorcier", 3, 45, 5, 0.16, // Nécro + sorcier
        new int[]{15, 19, 38, 39, 43, 46, 48}, ZombieCategory.SUMMONER),

    EXPLOSIVE("ZZ_Explosive", "Kaboombie", 3, 35, 3, 0.30,        // Kaboom + zombie
        new int[]{18, 21, 22, 24, 27}, ZombieCategory.EXPLOSIVE),

    GIANT("ZZ_Giant", "Mastodonte", 3, 120, 15, 0.10,             // Mastodonte (géant préhistorique)
        new int[]{17, 20, 23, 29}, ZombieCategory.TANK),

    CLIMBER("ZZ_Climber", "Escaladavre", 3, 40, 7, 0.28,          // Escalade + cadavre
        new int[]{28, 36}, ZombieCategory.MELEE),

    FROZEN("ZZ_Frozen", "Frigorifique", 3, 50, 6, 0.16,           // Frigorifique (mort + froid)
        new int[]{31, 32, 33, 34, 35, 37}, ZombieCategory.ELEMENTAL),

    YETI("ZZ_Yeti", "Abominaneige", 3, 80, 12, 0.22,              // Abominable + neige
        new int[]{31, 32, 33, 34, 35, 36, 37, 38}, ZombieCategory.ELITE),

    WENDIGO("ZZ_Wendigo", "Affamortis", 3, 70, 14, 0.32,          // Affamé + mortis
        new int[]{31, 35, 36, 37, 38}, ZombieCategory.ELITE),

    ZOMBIFIED_PIGLIN("ZZ_ZombifiedPiglin", "Porcinfernal", 3, 55, 10, 0.22, // Porc + infernal
        new int[]{21, 22, 23, 24, 27, 28, 29, 30}, ZombieCategory.MELEE),

    ZOGLIN("ZZ_Zoglin", "Zoglinfurieux", 3, 70, 12, 0.26,         // Zoglin + furieux
        new int[]{21, 22, 23, 24, 25, 27, 28, 29, 30}, ZombieCategory.MELEE),

    // ═══════════════════════════════════════════════════════════════════
    // TIER 4 - Les Terrifiants (Jeux de mots épiques) - Zones 19-50
    // ═══════════════════════════════════════════════════════════════════
    COLOSSUS("ZZ_Colossus", "Titanécrose", 4, 150, 18, 0.08,      // Titan + nécrose
        new int[]{22, 24, 27, 29, 31, 33, 36, 48, 49}, ZombieCategory.TANK),

    SPECTRE("ZZ_Spectre", "Spectramort", 4, 60, 12, 0.30,         // Spectre + mort
        new int[]{19, 20, 28, 30, 34, 35, 37, 38, 39, 40, 42, 46}, ZombieCategory.STEALTH),

    RAVAGER("ZZ_Ravager", "Dévastateur", 4, 100, 16, 0.28,        // Dévastateur
        new int[]{22, 23, 28, 30, 33, 49}, ZombieCategory.MELEE),

    CREAKING("ZZ_Creaking", "Craquosseux", 4, 80, 14, 0.05,       // Craquer + os
        new int[]{39, 40, 41, 43, 45}, ZombieCategory.SPECIAL),

    MUTANT("ZZ_Mutant", "Mutanomalie", 4, 90, 13, 0.24,           // Mutant + anomalie
        new int[]{25, 30, 41, 43, 44, 45, 47}, ZombieCategory.HAZARD),

    RAVAGER_BEAST("ZZ_RavagerBeast", "Ravageur", 4, 120, 20, 0.18, // Ravageur
        new int[]{28, 29, 30, 35, 40, 45, 48, 49}, ZombieCategory.TANK),

    PIGLIN_BRUTE("ZZ_PiglinBrute", "Brutalin", 4, 85, 16, 0.20,   // Brute + piglin
        new int[]{21, 22, 23, 24, 27, 30, 40, 41, 42}, ZombieCategory.MELEE),

    GIANT_BOSS("ZZ_GiantBoss", "Titanomort", 5, 500, 35, 0.12,   // Titan + mort (ULTRA RARE)
        new int[]{25, 30, 35, 40, 45, 50}, ZombieCategory.ELITE),

    // ═══════════════════════════════════════════════════════════════════
    // TIER 5 - Les Légendaires (Jeux de mots majestueux) - Zones 39-50
    // ═══════════════════════════════════════════════════════════════════
    CORRUPTED_WARDEN("ZZ_CorruptedWarden", "Gardinfernal", 5, 250, 25, 0.22,  // Gardien + infernal
        new int[]{39, 40, 41, 42, 44, 45, 46, 47, 48, 49, 50}, ZombieCategory.ELITE),

    ARCHON("ZZ_Archon", "Archonécros", 5, 200, 22, 0.26,          // Archon + nécros
        new int[]{40, 41, 43, 45, 46, 47, 48, 49, 50}, ZombieCategory.ELITE),

    DEMON("ZZ_Demon", "Démonécrose", 4, 70, 12, 0.28,             // Démon + nécrose
        new int[]{21, 22, 24, 27}, ZombieCategory.ELEMENTAL),

    INFERNAL("ZZ_Infernal", "Brasimort", 4, 100, 15, 0.24,        // Brasier + mort
        new int[]{21, 23, 24, 27}, ZombieCategory.ELITE),

    // ═══════════════════════════════════════════════════════════════════
    // JOURNEY - Zombies spéciaux pour les étapes du parcours
    // ═══════════════════════════════════════════════════════════════════
    FIRE_ZOMBIE("ZZ_FireZombie", "Pyromort", 2, 40, 6, 0.22,     // Pyro + mort (Chapitre 2 Étape 6 - spawn exclusif via Chapter2Systems)
        new int[]{}, ZombieCategory.ELEMENTAL),  // Pas de zone: spawn uniquement via Chapter2Systems.spawnFireZombie()

    HORDE_ZOMBIE("ZZ_HordeZombie", "Zombie de Horde", 2, 40, 5, 0.25,  // Zombie spécial pour l'événement Horde
        new int[]{}, ZombieCategory.EVENT),  // Pas de zone: spawn uniquement via HordeInvasionEvent

    WANDERING_BOSS("ZZ_WanderingBoss", "Boss Errant", 0, 500, 15, 0.20,  // Boss pour l'événement Wandering Boss
        new int[]{}, ZombieCategory.EVENT_BOSS),  // Pas de zone: spawn uniquement via WanderingBossEvent

    MANOR_LORD("ZZ_ManorLord", "Seigneur du Manoir", 0, 500, 15, 0.22,  // Boss Chapitre 2 Étape 10
        new int[]{2}, ZombieCategory.JOURNEY_BOSS),

    MINE_OVERLORD("ZZ_MineOverlord", "Seigneur des Profondeurs", 0, 1200, 35, 0.24,  // Boss Chapitre 3 Étape 10
        new int[]{6}, ZombieCategory.JOURNEY_BOSS),

    GRAVEDIGGER_BOSS("ZZ_GravediggerBoss", "Le Premier Mort", 0, 2400, 25, 0.20,  // Boss Chapitre 4 Étape 2 - Wither Skeleton géant (x3 HP)
        new int[]{}, ZombieCategory.JOURNEY_BOSS),

    CREAKING_BOSS("ZZ_CreakingBoss", "Gardien de l'Arbre Maudit", 0, 1200, 12, 0.18,  // Boss Chapitre 4 Étape 8 - Creaking géant (nerfé /2.5)
        new int[]{}, ZombieCategory.JOURNEY_BOSS),

    DAMNED_SOUL("ZZ_DamnedSoul", "Âme Damnée", 2, 60, 8, 0.18,  // Chapitre 4 Étape 6 - Zombie du cimetière à purifier
        new int[]{}, ZombieCategory.ELEMENTAL),  // Pas de zone: spawn uniquement via Chapter4Systems

    // ═══════════════════════════════════════════════════════════════════
    // MINI-BOSS - Les Redoutés (Noms épiques avec titres)
    // ═══════════════════════════════════════════════════════════════════
    BUTCHER("ZZ_Butcher", "L'Équarisseur", 0, 200, 12, 0.20,      // Boucher -> Équarisseur
        new int[]{10, 15, 20, 25, 30, 35, 40, 45}, ZombieCategory.MINIBOSS),

    WIDOW("ZZ_Widow", "La Veuvénéneuse", 0, 180, 10, 0.28,        // Veuve + venimeuse
        new int[]{15, 20, 25, 30, 35, 40, 45}, ZombieCategory.MINIBOSS),

    THE_GIANT("ZZ_TheGiant", "Le Colossal", 0, 300, 20, 0.08,     // Le Colossal
        new int[]{20, 25, 30, 35, 40, 45}, ZombieCategory.MINIBOSS),

    THE_PHANTOM("ZZ_ThePhantom", "L'Éthéré", 0, 150, 14, 0.32,    // L'Éthéré (fantomatique)
        new int[]{25, 30, 35, 40, 45}, ZombieCategory.MINIBOSS),

    // ═══════════════════════════════════════════════════════════════════
    // BOSS DE ZONE - Les Seigneurs (Titres épiques) - Tous les 10 zones
    // ═══════════════════════════════════════════════════════════════════
    BOSS_GUARDIAN("ZZ_Boss_Guardian", "Le Gardien Maudit", 0, 500, 15, 0.18,
        new int[]{10}, ZombieCategory.ZONE_BOSS),

    BOSS_SHADOW_ELDER("ZZ_Boss_ShadowElder", "L'Ancien des Ténèbres", 0, 1000, 25, 0.22,
        new int[]{20}, ZombieCategory.ZONE_BOSS),

    BOSS_FROST_LORD("ZZ_Boss_FrostLord", "Le Seigneur Givré", 0, 2000, 35, 0.16,
        new int[]{30}, ZombieCategory.ZONE_BOSS),

    BOSS_ABOMINATION("ZZ_Boss_Abomination", "L'Innommable", 0, 3500, 45, 0.20,
        new int[]{40}, ZombieCategory.ZONE_BOSS),

    // ═══════════════════════════════════════════════════════════════════
    // BOSS FINAL - Le Mythe (Nom légendaire) - Zone 50
    // ═══════════════════════════════════════════════════════════════════
    PATIENT_ZERO("ZZ_PatientZero", "Patient Zéro - L'Origine", 0, 10000, 50, 0.24,
        new int[]{50}, ZombieCategory.FINAL_BOSS);

    private final String id;             // ID unique du zombie
    private final String displayName;    // Nom d'affichage
    private final int tier;              // Tier de puissance (1-5, 0 pour boss)
    private final double baseHealth;     // Vie de base
    private final double baseDamage;     // Dégâts de base
    private final double baseSpeed;      // Vitesse de base
    private final int[] validZones;      // Zones où ce zombie peut spawn
    private final ZombieCategory category;

    ZombieType(String id, String displayName, int tier, double baseHealth,
               double baseDamage, double baseSpeed, int[] validZones, ZombieCategory category) {
        this.id = id;
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
     * Formule: baseHealth * (1 + level * 0.08 + level² * 0.003)
     *
     * Exemples pour un Walker (baseHealth=25):
     * - Niveau 1: 25 * 1.083 = ~27 HP
     * - Niveau 50: 25 * 12.5 = ~312 HP
     * - Niveau 100: 25 * 39 = ~975 HP
     *
     * Pour un Giant (baseHealth=120):
     * - Niveau 100: 120 * 39 = ~4680 HP
     */
    public double calculateHealth(int level) {
        // Scaling exponentiel pour atteindre 1000+ HP au niveau 100
        double multiplier = 1.0 + (level * 0.08) + (level * level * 0.003);
        // Bonus de tier pour les mobs de haut niveau
        double tierBonus = 1.0 + (tier * 0.1);
        return baseHealth * multiplier * tierBonus;
    }

    /**
     * Calcule les dégâts pour un niveau donné
     * Formule: baseDamage * (1 + level * 0.04 + level² * 0.001)
     *
     * Exemples pour un Walker (baseDamage=4):
     * - Niveau 1: 4 * 1.041 = ~4.2 damage
     * - Niveau 50: 4 * 5.5 = ~22 damage
     * - Niveau 100: 4 * 15 = ~60 damage
     */
    public double calculateDamage(int level) {
        // Scaling progressif pour les dégâts
        double multiplier = 1.0 + (level * 0.04) + (level * level * 0.001);
        // Bonus de tier
        double tierBonus = 1.0 + (tier * 0.15);
        return baseDamage * multiplier * tierBonus;
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
            case JOURNEY_BOSS -> 150; // Boss de chapitre Journey
            case SKELETON -> 6;      // Squelettes - récompense modérée
            case PACK -> 7;          // Loups - récompense modérée
            case ILLAGER -> 10;      // Illagers - récompense élevée
            case EVENT -> 8;         // Zombies d'événements - récompense modérée
            case EVENT_BOSS -> 300;  // Boss d'événements - grosse récompense
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
               category == ZombieCategory.FINAL_BOSS ||
               category == ZombieCategory.JOURNEY_BOSS ||
               category == ZombieCategory.EVENT_BOSS;
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
     * Obtient un type depuis son ID
     */
    public static ZombieType fromId(String id) {
        for (ZombieType type : values()) {
            if (type.id.equals(id)) {
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
        FINAL_BOSS,  // Boss final
        JOURNEY_BOSS,// Boss du Journey (chapitres)
        SKELETON,    // Squelettes archers (nouveaux)
        PACK,        // Loups en meute (nouveaux)
        ILLAGER,     // Illagers (Evoker, Pillager, Vindicator)
        EVENT,       // Zombies d'événements dynamiques (Horde, etc.)
        EVENT_BOSS   // Boss d'événements dynamiques (Wandering Boss, etc.)
    }
}
