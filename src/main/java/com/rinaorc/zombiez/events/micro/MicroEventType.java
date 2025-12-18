package com.rinaorc.zombiez.events.micro;

import lombok.Getter;
import org.bukkit.Sound;

/**
 * Types de micro-evenements disponibles
 * Les micro-evenements sont des evenements rapides (20-60s) qui spawnent frequemment
 * pour maintenir l'engagement des joueurs entre les gros evenements.
 */
@Getter
public enum MicroEventType {

    /**
     * Elite Chasseur - Un zombie elite fuit le joueur
     * Le joueur doit le traquer et le tuer avant qu'il ne s'echappe
     */
    ELITE_HUNTER(
        "Elite Chasseur",
        "elite_hunter",
        "\uD83D\uDC80", // 💀
        "§c",
        "Un zombie elite s'enfuit! Traquez-le!",
        Sound.ENTITY_ENDER_DRAGON_FLAP,
        45 * 20,        // 45 secondes
        300,            // Points de base
        100,            // XP de base
        25              // Poids de spawn
    ),

    /**
     * Faille Temporelle - Un portail crache des zombies
     * Tuer tous les zombies pour fermer le portail
     */
    TEMPORAL_RIFT(
        "Faille Temporelle",
        "temporal_rift",
        "\u26A1", // ⚡
        "§d",
        "Une faille temporelle s'ouvre! Fermez-la!",
        Sound.BLOCK_PORTAL_TRIGGER,
        30 * 20,        // 30 secondes
        200,            // Points
        75,             // XP
        25              // Poids
    ),

    /**
     * Pinata Zombie - Un zombie dore rempli de loot
     * Le tuer fait exploser du loot partout
     */
    PINATA_ZOMBIE(
        "Pinata Zombie",
        "pinata_zombie",
        "\uD83C\uDF81", // 🎁
        "§e",
        "Un zombie dore scintille! Frappez-le!",
        Sound.ENTITY_PLAYER_LEVELUP,
        20 * 20,        // 20 secondes
        150,            // Points
        50,             // XP
        20              // Poids (un peu plus rare)
    ),

    /**
     * Course Mortelle - Zombies alignes a eliminer rapidement
     * Timer demarre au premier kill, record par zone
     */
    DEATH_RACE(
        "Course Mortelle",
        "death_race",
        "\uD83C\uDFC3", // 🏃
        "§b",
        "Course mortelle! Eliminez-les tous!",
        Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
        30 * 20,        // 30 secondes max
        200,            // Points de base
        60,             // XP
        15              // Poids (moins frequent car skill-based)
    ),

    /**
     * Jackpot Zombie - Zombie slot machine
     * Frapper pour stopper les rouleaux, 3 identiques = jackpot
     */
    JACKPOT_ZOMBIE(
        "Jackpot Zombie",
        "jackpot_zombie",
        "\uD83C\uDFB0", // 🎰
        "§6",
        "Un zombie Jackpot apparait! Tentez votre chance!",
        Sound.BLOCK_NOTE_BLOCK_BELL,
        45 * 20,        // 45 secondes (augmente de 25s)
        100,            // Points de base (le jackpot donne plus)
        40,             // XP
        15              // Poids
    );

    private final String displayName;
    private final String configKey;
    private final String icon;
    private final String color;
    private final String description;
    private final Sound startSound;
    private final int defaultDuration;
    private final int basePointsReward;
    private final int baseXpReward;
    private final int spawnWeight;

    MicroEventType(String displayName, String configKey, String icon, String color,
                   String description, Sound startSound, int defaultDuration,
                   int basePointsReward, int baseXpReward, int spawnWeight) {
        this.displayName = displayName;
        this.configKey = configKey;
        this.icon = icon;
        this.color = color;
        this.description = description;
        this.startSound = startSound;
        this.defaultDuration = defaultDuration;
        this.basePointsReward = basePointsReward;
        this.baseXpReward = baseXpReward;
        this.spawnWeight = spawnWeight;
    }

    /**
     * Calcule le poids total de tous les types
     */
    public static int getTotalWeight() {
        int total = 0;
        for (MicroEventType type : values()) {
            total += type.spawnWeight;
        }
        return total;
    }

    /**
     * Obtient un type par sa cle de config
     */
    public static MicroEventType fromConfigKey(String key) {
        for (MicroEventType type : values()) {
            if (type.configKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
