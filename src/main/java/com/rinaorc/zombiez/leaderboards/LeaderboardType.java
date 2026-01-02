package com.rinaorc.zombiez.leaderboards;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Types de leaderboards disponibles dans ZombieZ
 * Organisés par catégorie pour le GUI
 */
@Getter
public enum LeaderboardType {

    // ═══════════════════════════════════════════════════════════════════════════
    // PRINCIPAUX (Permanents)
    // ═══════════════════════════════════════════════════════════════════════════

    KILLS_TOTAL("Kills Totaux", "kills", "§c⚔", Material.DIAMOND_SWORD, LeaderboardCategory.PRINCIPAL,
        "Nombre total de zombies tués"),
    LEVEL("Niveau", "level", "§a✦", Material.EXPERIENCE_BOTTLE, LeaderboardCategory.PRINCIPAL,
        "Niveau du joueur (prestige × 100 + level)"),
    MAX_ZONE("Zone Maximale", "max_zone", "§2🗺", Material.FILLED_MAP, LeaderboardCategory.PRINCIPAL,
        "Zone la plus avancée atteinte"),
    POINTS_EARNED("Points Gagnés", "total_points_earned", "§e⛃", Material.GOLD_INGOT, LeaderboardCategory.PRINCIPAL,
        "Total des points accumulés"),
    PLAYTIME("Temps de Jeu", "playtime", "§b⏱", Material.CLOCK, LeaderboardCategory.PRINCIPAL,
        "Temps total passé en jeu"),
    KILL_STREAK("Meilleur Streak", "best_kill_streak", "§c🔥", Material.BLAZE_POWDER, LeaderboardCategory.PRINCIPAL,
        "Plus longue série de kills sans mourir"),

    // ═══════════════════════════════════════════════════════════════════════════
    // COMBAT
    // ═══════════════════════════════════════════════════════════════════════════

    ZOMBIE_KILLS("Zombies Tués", "zombie_kills", "§7💀", Material.ZOMBIE_HEAD, LeaderboardCategory.COMBAT,
        "Kills de zombies normaux"),
    ELITE_KILLS("Élites Tuées", "elite_kills", "§6⚔", Material.GOLDEN_SWORD, LeaderboardCategory.COMBAT,
        "Kills de zombies élites"),
    BOSS_KILLS("Boss Tués", "boss_kills", "§4👹", Material.WITHER_SKELETON_SKULL, LeaderboardCategory.COMBAT,
        "Kills de boss (zone + world)"),
    HEADSHOTS("Headshots", "headshot_kills", "§c🎯", Material.BOW, LeaderboardCategory.COMBAT,
        "Kills par tir à la tête"),
    CRIT_KILLS("Kills Critiques", "crit_kills", "§e⚡", Material.NETHER_STAR, LeaderboardCategory.COMBAT,
        "Kills avec coup critique"),
    DEATHS("Morts", "deaths", "§4☠", Material.SKELETON_SKULL, LeaderboardCategory.COMBAT,
        "Nombre de morts (moins = mieux)"),

    // ═══════════════════════════════════════════════════════════════════════════
    // EXPLORATION
    // ═══════════════════════════════════════════════════════════════════════════

    CHUNKS_EXPLORED("Chunks Explorés", "chunks_explored", "§a🗺", Material.MAP, LeaderboardCategory.EXPLORATION,
        "Nombre de chunks découverts"),
    DISTANCE_TRAVELED("Distance Parcourue", "distance_traveled", "§b🏃", Material.LEATHER_BOOTS, LeaderboardCategory.EXPLORATION,
        "Distance totale en blocs"),
    NIGHTS_SURVIVED("Nuits Survivant", "nights_survived", "§9🌙", Material.BLACK_BED, LeaderboardCategory.EXPLORATION,
        "Cycles nuit complets survécus"),
    MYSTERY_CHESTS("Coffres Mystères", "mystery_chests_found", "§d🎁", Material.CHEST, LeaderboardCategory.EXPLORATION,
        "Coffres secrets découverts"),

    // ═══════════════════════════════════════════════════════════════════════════
    // PROGRESSION
    // ═══════════════════════════════════════════════════════════════════════════

    PRESTIGE("Prestige", "prestige", "§d★", Material.DRAGON_EGG, LeaderboardCategory.PROGRESSION,
        "Niveau de prestige"),
    CLASS_LEVEL_WARRIOR("Niveau Guerrier", "class_level_warrior", "§c⚔", Material.IRON_CHESTPLATE, LeaderboardCategory.PROGRESSION,
        "Niveau de la classe Guerrier"),
    CLASS_LEVEL_HUNTER("Niveau Chasseur", "class_level_hunter", "§a🏹", Material.BOW, LeaderboardCategory.PROGRESSION,
        "Niveau de la classe Chasseur"),
    CLASS_LEVEL_OCCULTIST("Niveau Occultiste", "class_level_occultist", "§5✦", Material.ENDER_EYE, LeaderboardCategory.PROGRESSION,
        "Niveau de la classe Occultiste"),
    ACHIEVEMENTS("Achievements", "achievement_count", "§6🏆", Material.GOLDEN_APPLE, LeaderboardCategory.PROGRESSION,
        "Nombre d'achievements débloqués"),
    JOURNEY_PROGRESS("Progression Journey", "journey_progress", "§e📜", Material.WRITTEN_BOOK, LeaderboardCategory.PROGRESSION,
        "Étapes du Journey complétées"),

    // ═══════════════════════════════════════════════════════════════════════════
    // PETS
    // ═══════════════════════════════════════════════════════════════════════════

    PETS_OWNED("Pets Possédés", "pets_owned", "§d🐾", Material.WOLF_SPAWN_EGG, LeaderboardCategory.PETS,
        "Nombre de pets uniques"),
    PETS_LEGENDARY("Pets Légendaires", "legendary_pets", "§6✦", Material.DRAGON_EGG, LeaderboardCategory.PETS,
        "Pets légendaires ou mieux"),
    PET_MAX_LEVEL("Pet Niveau Max", "pet_max_level", "§b⬆", Material.TURTLE_EGG, LeaderboardCategory.PETS,
        "Niveau le plus haut atteint par un pet"),

    // ═══════════════════════════════════════════════════════════════════════════
    // ÉCONOMIE
    // ═══════════════════════════════════════════════════════════════════════════

    CURRENT_POINTS("Fortune Actuelle", "points", "§e💰", Material.GOLD_BLOCK, LeaderboardCategory.ECONOMIE,
        "Points actuellement possédés"),
    GEMS("Gemmes", "gems", "§d💎", Material.DIAMOND, LeaderboardCategory.ECONOMIE,
        "Gemmes actuellement possédées"),
    FORGE_UPGRADES("Items Forgés +7", "forge_high_upgrades", "§6🔨", Material.ANVIL, LeaderboardCategory.ECONOMIE,
        "Items améliorés à +7 ou plus"),
    ITEMS_RECYCLED("Items Recyclés", "items_recycled", "§a♻", Material.COMPOSTER, LeaderboardCategory.ECONOMIE,
        "Items recyclés au total"),

    // ═══════════════════════════════════════════════════════════════════════════
    // ÉVÉNEMENTS
    // ═══════════════════════════════════════════════════════════════════════════

    EVENTS_COMPLETED("Événements Complétés", "events_completed", "§6🎮", Material.FIREWORK_ROCKET, LeaderboardCategory.EVENEMENTS,
        "Total d'événements réussis"),
    BLOOD_MOON_KILLS("Kills Blood Moon", "blood_moon_kills", "§4🌑", Material.REDSTONE, LeaderboardCategory.EVENEMENTS,
        "Zombies tués pendant Blood Moon"),
    HORDE_WAVES("Vagues de Horde", "horde_waves_survived", "§c💀", Material.BARRIER, LeaderboardCategory.EVENEMENTS,
        "Vagues de horde survécues"),
    MICRO_EVENTS("Micro-Événements", "micro_events_completed", "§e⚡", Material.LIGHTNING_ROD, LeaderboardCategory.EVENEMENTS,
        "Micro-événements complétés"),

    // ═══════════════════════════════════════════════════════════════════════════
    // MISSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    DAILY_MISSIONS("Missions Quotidiennes", "daily_missions_completed", "§a📋", Material.PAPER, LeaderboardCategory.MISSIONS,
        "Missions quotidiennes accomplies"),
    WEEKLY_MISSIONS("Missions Hebdo", "weekly_missions_completed", "§b📋", Material.BOOK, LeaderboardCategory.MISSIONS,
        "Missions hebdomadaires accomplies"),
    TOTAL_MISSIONS("Total Missions", "total_missions_completed", "§6📋", Material.KNOWLEDGE_BOOK, LeaderboardCategory.MISSIONS,
        "Total de missions accomplies");

    private final String displayName;
    private final String column;
    private final String icon;
    private final Material material;
    private final LeaderboardCategory category;
    private final String description;

    LeaderboardType(String displayName, String column, String icon, Material material,
                    LeaderboardCategory category, String description) {
        this.displayName = displayName;
        this.column = column;
        this.icon = icon;
        this.material = material;
        this.category = category;
        this.description = description;
    }

    /**
     * Vérifie si ce leaderboard utilise une colonne de la table stats
     * (au lieu de la table players)
     */
    public boolean isDetailedStat() {
        return switch (this) {
            case HEADSHOTS, CRIT_KILLS, NIGHTS_SURVIVED, CHUNKS_EXPLORED,
                 DISTANCE_TRAVELED, MYSTERY_CHESTS, CLASS_LEVEL_WARRIOR,
                 CLASS_LEVEL_HUNTER, CLASS_LEVEL_OCCULTIST, JOURNEY_PROGRESS,
                 PETS_OWNED, PETS_LEGENDARY, PET_MAX_LEVEL, FORGE_UPGRADES,
                 ITEMS_RECYCLED, EVENTS_COMPLETED, BLOOD_MOON_KILLS,
                 HORDE_WAVES, MICRO_EVENTS, DAILY_MISSIONS, WEEKLY_MISSIONS,
                 TOTAL_MISSIONS, ZOMBIE_KILLS, ELITE_KILLS, TOTAL_POINTS_EARNED -> true;
            default -> false;
        };
    }

    // Alias pour compatibilité
    private static final LeaderboardType TOTAL_POINTS_EARNED = POINTS_EARNED;
}
