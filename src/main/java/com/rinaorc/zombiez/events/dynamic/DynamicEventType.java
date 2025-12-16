package com.rinaorc.zombiez.events.dynamic;

import lombok.Getter;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;

/**
 * Types d'événements dynamiques disponibles
 */
@Getter
public enum DynamicEventType {

    /**
     * Largage Aérien - Un crate rare tombe du ciel
     * Les joueurs doivent le trouver et le défendre pendant l'ouverture
     */
    AIRDROP(
        "Largage Aérien",
        "airdrop",
        "📦",
        "§b",
        "Un largage de ravitaillement vient d'atterrir!",
        BarColor.BLUE,
        Sound.ENTITY_WITHER_SHOOT,
        20 * 60 * 5,    // 5 minutes
        150,            // Points de base
        100,            // XP de base
        100,            // Rayon d'annonce
        80,             // Rayon de visibilité
        15,             // Poids de spawn (chance relative)
        1               // Min joueurs requis
    ),

    /**
     * Nid de Zombies - Détruire un spawner temporaire
     * Les zombies spawn de plus en plus vite jusqu'à destruction
     */
    ZOMBIE_NEST(
        "Nid de Zombies",
        "zombie_nest",
        "🪺",
        "§c",
        "Un nid de zombies a été détecté! Détruisez-le avant qu'il ne soit trop tard!",
        BarColor.RED,
        Sound.ENTITY_RAVAGER_ROAR,
        20 * 60 * 4,    // 4 minutes
        200,            // Points
        150,            // XP
        120,            // Rayon d'annonce
        60,             // Rayon de visibilité
        20,             // Poids de spawn
        1               // Min joueurs
    ),

    /**
     * Invasion de Horde - Défendre un point pendant un temps donné
     * Vagues de zombies de plus en plus fortes
     */
    HORDE_INVASION(
        "Invasion de Horde",
        "horde_invasion",
        "💀",
        "§4",
        "Une horde massive approche! Défendez votre position!",
        BarColor.PURPLE,
        Sound.ENTITY_ENDER_DRAGON_GROWL,
        20 * 60 * 3,    // 3 minutes
        300,            // Points
        200,            // XP
        100,            // Rayon d'annonce
        70,             // Rayon de visibilité
        12,             // Poids de spawn
        2               // Min 2 joueurs recommandé
    ),

    /**
     * Convoi de Survivants - Escorter des PNJ d'un point A à un point B
     * Les PNJ doivent arriver vivants
     */
    SURVIVOR_CONVOY(
        "Convoi de Survivants",
        "survivor_convoy",
        "🚶",
        "§e",
        "Un groupe de survivants a besoin d'escorte! Protégez-les!",
        BarColor.YELLOW,
        Sound.ENTITY_VILLAGER_AMBIENT,
        20 * 60 * 6,    // 6 minutes max
        250,            // Points
        175,            // XP
        130,            // Rayon d'annonce
        100,            // Rayon de visibilité
        8,              // Poids de spawn (moins fréquent)
        1               // Min joueurs
    ),

    /**
     * Boss Errant - Un boss puissant traverse la zone
     * Le tuer avant qu'il n'atteigne sa destination
     */
    WANDERING_BOSS(
        "Boss Errant",
        "wandering_boss",
        "👹",
        "§5",
        "Un boss puissant a été repéré! Éliminez-le avant qu'il ne s'échappe!",
        BarColor.PINK,
        Sound.ENTITY_WITHER_SPAWN,
        20 * 60 * 5,    // 5 minutes
        500,            // Points (haute récompense)
        300,            // XP
        150,            // Rayon d'annonce (large)
        100,            // Rayon de visibilité
        5,              // Poids de spawn (rare)
        2               // Min 2 joueurs recommandé
    );

    private final String displayName;
    private final String configKey;
    private final String icon;
    private final String color;
    private final String description;
    private final BarColor barColor;
    private final Sound startSound;
    private final int defaultDuration;
    private final int basePointsReward;
    private final int baseXpReward;
    private final int announcementRadius;
    private final int visibilityRadius;
    private final int spawnWeight;
    private final int minPlayersRecommended;

    DynamicEventType(String displayName, String configKey, String icon, String color,
                     String description, BarColor barColor, Sound startSound,
                     int defaultDuration, int basePointsReward, int baseXpReward,
                     int announcementRadius, int visibilityRadius,
                     int spawnWeight, int minPlayersRecommended) {
        this.displayName = displayName;
        this.configKey = configKey;
        this.icon = icon;
        this.color = color;
        this.description = description;
        this.barColor = barColor;
        this.startSound = startSound;
        this.defaultDuration = defaultDuration;
        this.basePointsReward = basePointsReward;
        this.baseXpReward = baseXpReward;
        this.announcementRadius = announcementRadius;
        this.visibilityRadius = visibilityRadius;
        this.spawnWeight = spawnWeight;
        this.minPlayersRecommended = minPlayersRecommended;
    }

    /**
     * Obtient un type par sa clé de config
     */
    public static DynamicEventType fromConfigKey(String key) {
        for (DynamicEventType type : values()) {
            if (type.configKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Calcule le poids total de tous les types
     */
    public static int getTotalWeight() {
        int total = 0;
        for (DynamicEventType type : values()) {
            total += type.spawnWeight;
        }
        return total;
    }
}
