package com.rinaorc.zombiez.weather;

import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.potion.PotionEffectType;

/**
 * Types de météo dynamique disponibles dans ZombieZ
 *
 * Chaque type de météo a des effets uniques sur le gameplay:
 * - Effets visuels (particules, fog/brouillard natif Minecraft)
 * - Effets sur les joueurs (BONUS ou légers malus, jamais de debuffs pénibles)
 * - Effets sur les zombies (buffs/debuffs)
 * - Modification du spawn rate
 * - Bonus de loot et XP
 *
 * PHILOSOPHIE: La météo doit créer de l'ambiance, pas frustrer les joueurs!
 * - Pas de Blindness (aveuglement)
 * - Pas de Weakness (faiblesse)
 * - Utilisation du fog natif Minecraft pour la visibilité
 * - Plusieurs météos offrent des BONUS aux joueurs
 */
@Getter
public enum WeatherType {

    // ==================== MÉTÉOS NEUTRES/STANDARD ====================

    /**
     * Temps Clair - Météo par défaut, légèrement bénéfique
     * Conditions idéales pour l'exploration
     */
    CLEAR(
        "Temps Clair",
        "clear",
        "☀",
        "§e",
        "Le ciel est dégagé, conditions idéales pour chasser!",
        BarColor.YELLOW,
        null,
        Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
        20 * 60 * 8,        // 8 minutes min
        20 * 60 * 15,       // 15 minutes max
        1.0,                // Spawn rate normal
        1.0,                // Dégâts zombies normal
        1.0,                // Vitesse zombies normal
        0.0,                // Pas de dégâts environnementaux
        0,                  // Pas d'intervalle
        0,                  // Fog distance normal (0 = pas de changement)
        // Bonus joueurs
        1.05,               // +5% XP
        1.0,                // Loot normal
        1.02,               // +2% vitesse
        0.0,                // Pas de régénération
        null,               // Pas de buff de potion
        30,                 // Poids élevé (fréquent)
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Pluie - Pluie légère, ambiance calme
     * Léger boost de régénération (pluie rafraîchissante)
     */
    RAIN(
        "Pluie",
        "rain",
        "🌧",
        "§9",
        "La pluie rafraîchit l'atmosphère... Moment de calme relatif.",
        BarColor.BLUE,
        Particle.RAIN,
        Sound.WEATHER_RAIN,
        20 * 60 * 5,        // 5 minutes min
        20 * 60 * 12,       // 12 minutes max
        1.1,                // +10% spawn
        1.0,                // Dégâts normaux
        1.0,                // Vitesse normale
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        0,                  // Fog normal
        // Bonus joueurs
        1.0,                // XP normal
        1.0,                // Loot normal
        1.0,                // Vitesse normale
        0.25,               // +0.25 HP régénération toutes les 5s
        null,               // Pas de buff
        25,                 // Poids moyen
        true,               // Pluie Minecraft
        false               // Pas de tonnerre
    ),

    // ==================== MÉTÉOS DANGEREUSES ====================

    /**
     * Tempête - Orage violent avec éclairs
     * Zombies plus agressifs mais +15% loot en récompense du risque
     */
    STORM(
        "Tempête",
        "storm",
        "⛈",
        "§5",
        "Une tempête violente éclate! Les zombies sont agités mais le loot est meilleur!",
        BarColor.PURPLE,
        Particle.RAIN,
        Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
        20 * 60 * 3,        // 3 minutes min
        20 * 60 * 7,        // 7 minutes max
        1.4,                // +40% spawn
        1.15,               // +15% dégâts zombies
        1.1,                // +10% vitesse zombies
        0.0,                // Pas de dégâts directs
        0,                  // Pas d'intervalle
        0,                  // Fog normal (orage = ciel sombre naturellement)
        // Bonus joueurs (récompense du risque)
        1.1,                // +10% XP
        1.15,               // +15% loot!
        1.0,                // Vitesse normale
        0.0,                // Pas de régénération
        null,               // Pas de buff
        15,                 // Moins fréquent
        true,               // Pluie Minecraft
        true                // Tonnerre Minecraft
    ),

    /**
     * Blizzard - Tempête de neige
     * Ralentit les zombies (gelés), joueurs gardent leur vitesse
     */
    BLIZZARD(
        "Blizzard",
        "blizzard",
        "❄",
        "§b",
        "Un blizzard glacial s'abat! Les zombies sont ralentis par le froid!",
        BarColor.WHITE,
        Particle.SNOWFLAKE,
        Sound.ITEM_ELYTRA_FLYING,
        20 * 60 * 3,        // 3 minutes min
        20 * 60 * 8,        // 8 minutes max
        1.2,                // +20% spawn
        1.0,                // Dégâts normaux
        0.7,                // -30% vitesse zombies (GELÉS!)
        0.0,                // Pas de dégâts (on retire le froid qui fait mal)
        0,                  // Pas d'intervalle
        6,                  // Fog léger (neige réduit visibilité)
        // Bonus joueurs
        1.0,                // XP normal
        1.1,                // +10% loot (zombies ralentis = plus facile)
        1.0,                // Vitesse normale (joueurs pas affectés)
        0.0,                // Pas de régénération
        null,               // PAS de slowness - juste l'ambiance fog
        12,                 // Assez rare
        true,               // Pluie (neige)
        false               // Pas de tonnerre
    ),

    /**
     * Brouillard - Brouillard dense
     * Utilise le fog natif Minecraft, pas de Blindness!
     * Spawn augmenté mais +20% XP (danger = récompense)
     */
    FOG(
        "Brouillard",
        "fog",
        "🌫",
        "§7",
        "Un brouillard dense envahit la zone... Mais chaque kill rapporte plus d'XP!",
        BarColor.WHITE,
        Particle.CAMPFIRE_COSY_SMOKE,
        Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD,
        20 * 60 * 4,        // 4 minutes min
        20 * 60 * 10,       // 10 minutes max
        1.35,               // +35% spawn
        1.1,                // +10% dégâts zombies
        1.05,               // +5% vitesse zombies
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        4,                  // FOG DENSE via render distance!
        // Bonus joueurs (récompense du danger)
        1.2,                // +20% XP!
        1.05,               // +5% loot
        1.0,                // Vitesse normale
        0.0,                // Pas de régénération
        null,               // PAS de Blindness! Juste le fog natif
        18,                 // Moyennement fréquent
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Pluie de Cendres - Cendres volcaniques
     * Légers dégâts mais GROS bonus loot (+25%)
     */
    ASHFALL(
        "Pluie de Cendres",
        "ashfall",
        "🌋",
        "§8",
        "Des cendres volcaniques tombent... Mais le loot est exceptionnel!",
        BarColor.RED,
        Particle.ASH,
        Sound.BLOCK_FIRE_AMBIENT,
        20 * 60 * 2,        // 2 minutes min (court)
        20 * 60 * 5,        // 5 minutes max
        1.15,               // +15% spawn
        1.15,               // +15% dégâts zombies
        1.0,                // Vitesse normale
        0.5,                // 0.5 dégâts (réduit de 1.0)
        20 * 5,             // Toutes les 5 secondes (plus lent)
        3,                  // Léger fog (cendres)
        // GROS bonus loot pour compenser les dégâts
        1.15,               // +15% XP
        1.25,               // +25% LOOT!
        1.0,                // Vitesse normale
        0.0,                // Pas de régénération
        null,               // Pas de debuff
        10,                 // Rare
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Lune de Sang - Événement nocturne épique
     * TRÈS dangereux mais ÉNORMES récompenses
     */
    BLOOD_MOON(
        "Lune de Sang",
        "blood_moon",
        "🌑",
        "§4",
        "§4§lLUNE DE SANG! §cDanger extrême mais récompenses légendaires!",
        BarColor.RED,
        Particle.CRIMSON_SPORE,
        Sound.ENTITY_WITHER_AMBIENT,
        20 * 60 * 4,        // 4 minutes min
        20 * 60 * 8,        // 8 minutes max
        2.5,                // x2.5 spawn (réduit de x3)
        1.4,                // +40% dégâts zombies
        1.2,                // +20% vitesse zombies
        0.0,                // Pas de dégâts environnementaux
        0,                  // Pas d'intervalle
        0,                  // Pas de fog (ciel rouge)
        // ÉNORMES BONUS (récompense du risque)
        1.5,                // +50% XP!
        1.5,                // +50% LOOT!
        1.05,               // +5% vitesse joueurs (adrénaline)
        0.0,                // Pas de régénération
        PotionEffectType.STRENGTH, // Force I pour aider les joueurs!
        5,                  // Très rare
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Pluie Acide - Dangereuse mais courte
     * Dégâts réduits, gros bonus pour survivre
     */
    ACID_RAIN(
        "Pluie Acide",
        "acid_rain",
        "☢",
        "§a",
        "§a§lPLUIE ACIDE! §7Trouvez un abri ou récoltez les bonus de survie!",
        BarColor.GREEN,
        Particle.FALLING_SPORE_BLOSSOM,
        Sound.BLOCK_LAVA_AMBIENT,
        20 * 60 * 1,        // 1 minute min (TRÈS COURT)
        20 * 60 * 3,        // 3 minutes max
        1.2,                // +20% spawn
        1.2,                // +20% dégâts zombies
        1.0,                // Vitesse normale
        0.75,               // 0.75 dégâts (réduit)
        20 * 3,             // Toutes les 3 secondes
        2,                  // Léger fog vert
        // Bonus survie
        1.3,                // +30% XP
        1.35,               // +35% LOOT!
        1.0,                // Vitesse normale
        0.0,                // Pas de régénération
        null,               // Pas de debuff
        6,                  // Rare
        true,               // Pluie Minecraft
        false               // Pas de tonnerre
    ),

    /**
     * Tempête de Sable - Ambiance désertique
     * Utilise fog, pas de Blindness!
     */
    SANDSTORM(
        "Tempête de Sable",
        "sandstorm",
        "🏜",
        "§6",
        "Une tempête de sable balaie la zone! Visibilité réduite mais zombies aussi affectés!",
        BarColor.YELLOW,
        Particle.FALLING_DUST,
        Sound.ITEM_ELYTRA_FLYING,
        20 * 60 * 3,        // 3 minutes min
        20 * 60 * 7,        // 7 minutes max
        1.15,               // +15% spawn
        1.0,                // Dégâts normaux
        0.85,               // -15% vitesse zombies (sable gênant)
        0.0,                // PAS de dégâts (supprimé)
        0,                  // Pas d'intervalle
        5,                  // FOG moyen (sable)
        // Léger bonus
        1.1,                // +10% XP
        1.1,                // +10% loot
        1.0,                // Vitesse normale
        0.0,                // Pas de régénération
        null,               // PAS de Blindness! Juste fog
        10,                 // Assez rare
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    // ==================== MÉTÉOS BÉNÉFIQUES ====================

    /**
     * Aurore Boréale - Phénomène rare très bénéfique
     * Gros bonus pour les joueurs, zombies affaiblis
     */
    AURORA(
        "Aurore Boréale",
        "aurora",
        "✨",
        "§d",
        "Une magnifique aurore illumine le ciel... Profitez de ce moment de grâce!",
        BarColor.PINK,
        Particle.END_ROD,
        Sound.BLOCK_AMETHYST_BLOCK_CHIME,
        20 * 60 * 3,        // 3 minutes min
        20 * 60 * 8,        // 8 minutes max
        0.5,                // -50% spawn (calme!)
        0.75,               // -25% dégâts zombies
        0.85,               // -15% vitesse zombies
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        0,                  // Pas de fog (ciel clair)
        // GROS BONUS
        1.25,               // +25% XP!
        1.2,                // +20% loot
        1.05,               // +5% vitesse
        0.5,                // Régénération légère!
        PotionEffectType.LUCK, // Chance!
        8,                  // Assez rare mais bénéfique
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Bénédiction Solaire - NOUVEAU - Événement très positif
     * XP et régénération boostés
     */
    SOLAR_BLESSING(
        "Bénédiction Solaire",
        "solar_blessing",
        "🌞",
        "§6",
        "Le soleil brille d'une lumière divine! XP et régénération amplifiés!",
        BarColor.YELLOW,
        Particle.GLOW,
        Sound.BLOCK_BEACON_ACTIVATE,
        20 * 60 * 3,        // 3 minutes min
        20 * 60 * 6,        // 6 minutes max
        0.7,                // -30% spawn (calme)
        0.8,                // -20% dégâts zombies
        0.9,                // -10% vitesse zombies
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        0,                  // Pas de fog
        // ÉNORMES BONUS
        1.4,                // +40% XP!
        1.1,                // +10% loot
        1.03,               // +3% vitesse
        1.0,                // Bonne régénération
        PotionEffectType.REGENERATION, // Régénération I
        6,                  // Rare
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Lune des Moissons - NOUVEAU - Loot boosté
     * Événement nocturne positif
     */
    HARVEST_MOON(
        "Lune des Moissons",
        "harvest_moon",
        "🌕",
        "§e",
        "La Lune des Moissons se lève! Le loot est exceptionnellement généreux!",
        BarColor.YELLOW,
        Particle.HAPPY_VILLAGER,
        Sound.ENTITY_PLAYER_LEVELUP,
        20 * 60 * 4,        // 4 minutes min
        20 * 60 * 8,        // 8 minutes max
        0.9,                // -10% spawn
        0.9,                // -10% dégâts zombies
        1.0,                // Vitesse normale
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        0,                  // Pas de fog
        // FOCUS LOOT
        1.2,                // +20% XP
        1.4,                // +40% LOOT!
        1.0,                // Vitesse normale
        0.25,               // Légère régénération
        PotionEffectType.LUCK, // Chance accrue
        7,                  // Assez rare
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Brise Légère - NOUVEAU - Vitesse et mobilité
     * Joueurs plus rapides, zombies plus lents
     */
    GENTLE_BREEZE(
        "Brise Légère",
        "gentle_breeze",
        "🍃",
        "§a",
        "Une brise agréable souffle... Vous vous sentez plus léger!",
        BarColor.GREEN,
        Particle.CLOUD,
        Sound.ENTITY_PHANTOM_FLAP,
        20 * 60 * 4,        // 4 minutes min
        20 * 60 * 10,       // 10 minutes max
        0.85,               // -15% spawn
        0.95,               // -5% dégâts zombies
        0.9,                // -10% vitesse zombies
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        0,                  // Pas de fog
        // FOCUS MOBILITÉ
        1.1,                // +10% XP
        1.05,               // +5% loot
        1.1,                // +10% VITESSE!
        0.0,                // Pas de régénération
        PotionEffectType.SPEED, // Speed I!
        12,                 // Moyennement fréquent
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Pluie d'Étoiles - NOUVEAU - Événement rare et magique
     * Chance de drops rares augmentée
     */
    STARFALL(
        "Pluie d'Étoiles",
        "starfall",
        "⭐",
        "§b",
        "Des étoiles filantes illuminent le ciel! Chance de drops légendaires!",
        BarColor.BLUE,
        Particle.FIREWORK,
        Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
        20 * 60 * 2,        // 2 minutes min (rare et court)
        20 * 60 * 5,        // 5 minutes max
        0.8,                // -20% spawn
        0.85,               // -15% dégâts zombies
        0.95,               // -5% vitesse zombies
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        0,                  // Pas de fog (ciel étoilé)
        // FOCUS DROPS RARES
        1.35,               // +35% XP
        1.5,                // +50% LOOT!
        1.0,                // Vitesse normale
        0.0,                // Pas de régénération
        PotionEffectType.LUCK, // CHANCE maximale
        4,                  // TRÈS RARE
        false,              // Pas de pluie
        false               // Pas de tonnerre
    );

    // ==================== PROPRIÉTÉS ====================

    private final String displayName;
    private final String configKey;
    private final String icon;
    private final String color;
    private final String description;
    private final BarColor barColor;
    private final Particle particle;
    private final Sound ambientSound;
    private final int minDuration;
    private final int maxDuration;
    private final double spawnMultiplier;
    private final double zombieDamageMultiplier;
    private final double zombieSpeedMultiplier;
    private final double environmentalDamage;
    private final int damageInterval;
    private final int fogLevel;              // 0 = normal, 1-10 = fog intensity

    // Bonus joueurs
    private final double xpMultiplier;       // Multiplicateur XP
    private final double lootMultiplier;     // Multiplicateur loot
    private final double playerSpeedBonus;   // Bonus vitesse joueurs
    private final double regenAmount;        // Régénération par tick
    private final PotionEffectType buffEffect; // Effet de buff (optionnel)

    private final int spawnWeight;
    private final boolean minecraftRain;
    private final boolean minecraftThunder;

    WeatherType(String displayName, String configKey, String icon, String color,
                String description, BarColor barColor, Particle particle, Sound ambientSound,
                int minDuration, int maxDuration, double spawnMultiplier,
                double zombieDamageMultiplier, double zombieSpeedMultiplier,
                double environmentalDamage, int damageInterval, int fogLevel,
                double xpMultiplier, double lootMultiplier, double playerSpeedBonus,
                double regenAmount, PotionEffectType buffEffect,
                int spawnWeight, boolean minecraftRain, boolean minecraftThunder) {
        this.displayName = displayName;
        this.configKey = configKey;
        this.icon = icon;
        this.color = color;
        this.description = description;
        this.barColor = barColor;
        this.particle = particle;
        this.ambientSound = ambientSound;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.spawnMultiplier = spawnMultiplier;
        this.zombieDamageMultiplier = zombieDamageMultiplier;
        this.zombieSpeedMultiplier = zombieSpeedMultiplier;
        this.environmentalDamage = environmentalDamage;
        this.damageInterval = damageInterval;
        this.fogLevel = fogLevel;
        this.xpMultiplier = xpMultiplier;
        this.lootMultiplier = lootMultiplier;
        this.playerSpeedBonus = playerSpeedBonus;
        this.regenAmount = regenAmount;
        this.buffEffect = buffEffect;
        this.spawnWeight = spawnWeight;
        this.minecraftRain = minecraftRain;
        this.minecraftThunder = minecraftThunder;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Obtient un type par sa clé de config
     */
    public static WeatherType fromConfigKey(String key) {
        for (WeatherType type : values()) {
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
        for (WeatherType type : values()) {
            total += type.spawnWeight;
        }
        return total;
    }

    /**
     * Vérifie si ce type de météo est dangereux (dégâts environnementaux)
     */
    public boolean isDangerous() {
        return environmentalDamage > 0;
    }

    /**
     * Vérifie si ce type de météo est bénéfique pour les joueurs
     */
    public boolean isBeneficial() {
        return xpMultiplier > 1.1 || lootMultiplier > 1.1 || regenAmount > 0 || buffEffect != null;
    }

    /**
     * Vérifie si ce type affecte les zombies positivement (les rend plus forts)
     */
    public boolean buffZombies() {
        return zombieDamageMultiplier > 1.05 || zombieSpeedMultiplier > 1.05;
    }

    /**
     * Vérifie si ce type affaiblit les zombies
     */
    public boolean debuffZombies() {
        return zombieDamageMultiplier < 0.95 || zombieSpeedMultiplier < 0.95;
    }

    /**
     * Vérifie si cette météo utilise le fog
     */
    public boolean hasFog() {
        return fogLevel > 0;
    }

    /**
     * Obtient la render distance modifiée pour le fog (en chunks)
     * Retourne -1 si pas de modification
     */
    public int getFogRenderDistance() {
        if (fogLevel == 0) return -1;
        // fogLevel 1-10 => render distance 10-2 chunks
        return Math.max(2, 12 - fogLevel);
    }

    /**
     * Vérifie si cette météo donne un buff de potion aux joueurs
     */
    public boolean hasPlayerBuff() {
        return buffEffect != null;
    }

    /**
     * Obtient l'amplificateur du buff (0 = niveau I, 1 = niveau II)
     */
    public int getBuffAmplifier() {
        // La plupart des buffs sont niveau I (amplifier 0)
        return 0;
    }

    /**
     * Obtient la durée du buff en ticks
     */
    public int getBuffDuration() {
        return 20 * 6; // 6 secondes (renouvelé régulièrement)
    }

    /**
     * Calcule la couleur du ciel selon le type (pour effets visuels)
     */
    public Color getSkyTintColor() {
        return switch (this) {
            case BLOOD_MOON -> Color.fromRGB(139, 0, 0);
            case ACID_RAIN -> Color.fromRGB(50, 150, 50);
            case AURORA -> Color.fromRGB(180, 100, 220);
            case STARFALL -> Color.fromRGB(100, 100, 180);
            case SOLAR_BLESSING -> Color.fromRGB(255, 215, 0);
            case HARVEST_MOON -> Color.fromRGB(255, 200, 100);
            default -> null;
        };
    }
}
