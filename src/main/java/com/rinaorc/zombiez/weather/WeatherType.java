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
 * - Effets visuels (particules, lumière)
 * - Effets sur les joueurs (debuffs, dégâts)
 * - Effets sur les zombies (buffs, comportement)
 * - Modification du spawn rate
 */
@Getter
public enum WeatherType {

    /**
     * Temps Clair - Météo par défaut sans effets spéciaux
     * Conditions idéales pour l'exploration
     */
    CLEAR(
        "Temps Clair",
        "clear",
        "☀",
        "§e",
        "Le ciel est dégagé, conditions idéales.",
        BarColor.YELLOW,
        null,
        null,
        20 * 60 * 10,       // 10 minutes
        20 * 60 * 20,       // 20 minutes
        1.0,                // Spawn rate normal
        1.0,                // Dégâts zombies normal
        1.0,                // Vitesse zombies normal
        0.0,                // Pas de dégâts environnementaux
        0,                  // Pas d'intervalle
        30,                 // Poids élevé (fréquent)
        false,              // Pas de pluie Minecraft
        false               // Pas de tonnerre Minecraft
    ),

    /**
     * Pluie - Pluie légère avec visibilité réduite
     * Légère augmentation du spawn de zombies
     */
    RAIN(
        "Pluie",
        "rain",
        "🌧",
        "§9",
        "La pluie commence à tomber, restez vigilants.",
        BarColor.BLUE,
        Particle.RAIN,
        Sound.WEATHER_RAIN,
        20 * 60 * 5,        // 5 minutes min
        20 * 60 * 12,       // 12 minutes max
        1.15,               // +15% spawn
        1.0,                // Dégâts normaux
        1.0,                // Vitesse normale
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        25,                 // Poids moyen
        true,               // Pluie Minecraft
        false               // Pas de tonnerre
    ),

    /**
     * Tempête - Pluie intense avec éclairs et tonnerre
     * Zombies plus agressifs, spawn rate augmenté
     */
    STORM(
        "Tempête",
        "storm",
        "⛈",
        "§5",
        "Une tempête violente approche! Les zombies deviennent plus agressifs!",
        BarColor.PURPLE,
        Particle.RAIN,
        Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
        20 * 60 * 3,        // 3 minutes min
        20 * 60 * 8,        // 8 minutes max
        1.5,                // +50% spawn
        1.2,                // +20% dégâts zombies
        1.15,               // +15% vitesse zombies
        0.0,                // Pas de dégâts directs
        0,                  // Pas d'intervalle
        15,                 // Moins fréquent
        true,               // Pluie Minecraft
        true                // Tonnerre Minecraft
    ),

    /**
     * Blizzard - Tempête de neige avec froid intense
     * Ralentit les joueurs, réduit la visibilité drastiquement
     */
    BLIZZARD(
        "Blizzard",
        "blizzard",
        "❄",
        "§b",
        "Un blizzard glacial s'abat sur la zone! Protégez-vous du froid!",
        BarColor.WHITE,
        Particle.SNOWFLAKE,
        Sound.ITEM_ELYTRA_FLYING,
        20 * 60 * 4,        // 4 minutes min
        20 * 60 * 10,       // 10 minutes max
        1.3,                // +30% spawn
        1.1,                // +10% dégâts zombies
        0.85,               // -15% vitesse zombies (gelés)
        0.5,                // 0.5 dégâts de froid
        20 * 3,             // Toutes les 3 secondes
        12,                 // Assez rare
        true,               // Pluie (neige dans les biomes froids)
        false               // Pas de tonnerre
    ),

    /**
     * Brouillard - Brouillard épais réduisant drastiquement la visibilité
     * Les zombies peuvent surprendre les joueurs plus facilement
     */
    FOG(
        "Brouillard",
        "fog",
        "🌫",
        "§7",
        "Un brouillard dense envahit la zone... Méfiez-vous des ombres.",
        BarColor.WHITE,
        Particle.CAMPFIRE_COSY_SMOKE,
        Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD,
        20 * 60 * 5,        // 5 minutes min
        20 * 60 * 15,       // 15 minutes max
        1.4,                // +40% spawn (danger accru)
        1.15,               // +15% dégâts zombies
        1.1,                // +10% vitesse zombies
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        18,                 // Moyennement fréquent
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Pluie de Cendres - Cendres volcaniques toxiques
     * Dégâts respiratoires aux joueurs sans protection
     */
    ASHFALL(
        "Pluie de Cendres",
        "ashfall",
        "🌋",
        "§8",
        "Des cendres volcaniques obscurcissent le ciel! L'air devient irrespirable!",
        BarColor.RED,
        Particle.ASH,
        Sound.BLOCK_FIRE_AMBIENT,
        20 * 60 * 3,        // 3 minutes min
        20 * 60 * 7,        // 7 minutes max
        1.25,               // +25% spawn
        1.25,               // +25% dégâts zombies (enragés)
        1.0,                // Vitesse normale
        1.0,                // 1 dégât toxique
        20 * 4,             // Toutes les 4 secondes
        10,                 // Rare
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Lune de Sang - Événement nocturne spécial
     * Spawn rate triplé, zombies beaucoup plus forts
     */
    BLOOD_MOON(
        "Lune de Sang",
        "blood_moon",
        "🌑",
        "§4",
        "§4§lLUNE DE SANG! §cLes morts se lèvent en masse!",
        BarColor.RED,
        Particle.CRIMSON_SPORE,
        Sound.ENTITY_WITHER_AMBIENT,
        20 * 60 * 5,        // 5 minutes min
        20 * 60 * 10,       // 10 minutes max (nuit entière)
        3.0,                // x3 spawn!
        1.5,                // +50% dégâts zombies
        1.25,               // +25% vitesse zombies
        0.0,                // Pas de dégâts environnementaux
        0,                  // Pas d'intervalle
        5,                  // Très rare
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Pluie Acide - Pluie corrosive dangereuse
     * Dégâts constants sans abri, zombies immunisés
     */
    ACID_RAIN(
        "Pluie Acide",
        "acid_rain",
        "☢",
        "§a",
        "§a§lPLUIE ACIDE! §7Trouvez un abri ou subissez des brûlures chimiques!",
        BarColor.GREEN,
        Particle.FALLING_SPORE_BLOSSOM,
        Sound.BLOCK_LAVA_AMBIENT,
        20 * 60 * 2,        // 2 minutes min
        20 * 60 * 6,        // 6 minutes max
        1.35,               // +35% spawn
        1.3,                // +30% dégâts zombies
        1.1,                // +10% vitesse zombies
        1.5,                // 1.5 dégâts acide
        20 * 2,             // Toutes les 2 secondes
        8,                  // Rare
        true,               // Pluie Minecraft (verte visuellement)
        false               // Pas de tonnerre
    ),

    /**
     * Aurore Boréale - Phénomène rare bénéfique
     * Bonus pour les joueurs, spawn rate réduit temporairement
     */
    AURORA(
        "Aurore Boréale",
        "aurora",
        "✨",
        "§d",
        "Une magnifique aurore illumine le ciel... Un moment de répit.",
        BarColor.PINK,
        Particle.END_ROD,
        Sound.BLOCK_AMETHYST_BLOCK_CHIME,
        20 * 60 * 3,        // 3 minutes min
        20 * 60 * 8,        // 8 minutes max
        0.5,                // -50% spawn (calme)
        0.8,                // -20% dégâts zombies
        0.9,                // -10% vitesse zombies
        0.0,                // Pas de dégâts
        0,                  // Pas d'intervalle
        6,                  // Rare mais bénéfique
        false,              // Pas de pluie
        false               // Pas de tonnerre
    ),

    /**
     * Tempête de Sable - Visibilité quasi nulle, dégâts de sable
     * Uniquement dans les zones désertiques
     */
    SANDSTORM(
        "Tempête de Sable",
        "sandstorm",
        "🏜",
        "§6",
        "Une tempête de sable aveuglante balaie la zone!",
        BarColor.YELLOW,
        Particle.FALLING_DUST,
        Sound.ITEM_ELYTRA_FLYING,
        20 * 60 * 3,        // 3 minutes min
        20 * 60 * 8,        // 8 minutes max
        1.2,                // +20% spawn
        1.1,                // +10% dégâts zombies
        0.9,                // -10% vitesse (sable gênant)
        0.3,                // 0.3 dégâts mineurs
        20 * 5,             // Toutes les 5 secondes
        10,                 // Assez rare
        false,              // Pas de pluie
        false               // Pas de tonnerre
    );

    private final String displayName;
    private final String configKey;
    private final String icon;
    private final String color;
    private final String description;
    private final BarColor barColor;
    private final Particle particle;
    private final Sound ambientSound;
    private final int minDuration;       // Durée minimum en ticks
    private final int maxDuration;       // Durée maximum en ticks
    private final double spawnMultiplier;     // Multiplicateur de spawn de zombies
    private final double zombieDamageMultiplier;  // Multiplicateur de dégâts des zombies
    private final double zombieSpeedMultiplier;   // Multiplicateur de vitesse des zombies
    private final double environmentalDamage;     // Dégâts environnementaux par tick
    private final int damageInterval;             // Intervalle entre les dégâts (ticks)
    private final int spawnWeight;                // Poids pour la sélection aléatoire
    private final boolean minecraftRain;          // Activer la pluie Minecraft
    private final boolean minecraftThunder;       // Activer le tonnerre Minecraft

    WeatherType(String displayName, String configKey, String icon, String color,
                String description, BarColor barColor, Particle particle, Sound ambientSound,
                int minDuration, int maxDuration, double spawnMultiplier,
                double zombieDamageMultiplier, double zombieSpeedMultiplier,
                double environmentalDamage, int damageInterval,
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
        this.spawnWeight = spawnWeight;
        this.minecraftRain = minecraftRain;
        this.minecraftThunder = minecraftThunder;
    }

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
     * Vérifie si ce type de météo est bénéfique
     */
    public boolean isBeneficial() {
        return spawnMultiplier < 1.0 || zombieDamageMultiplier < 1.0;
    }

    /**
     * Vérifie si ce type affecte les zombies positivement (les rend plus forts)
     */
    public boolean buffZombies() {
        return zombieDamageMultiplier > 1.0 || zombieSpeedMultiplier > 1.0;
    }

    /**
     * Obtient l'effet de potion associé au type de météo (pour les debuffs joueurs)
     */
    public PotionEffectType getPlayerDebuffEffect() {
        return switch (this) {
            case BLIZZARD -> PotionEffectType.SLOWNESS;
            case FOG -> PotionEffectType.BLINDNESS;
            case SANDSTORM -> PotionEffectType.BLINDNESS;
            case ACID_RAIN -> PotionEffectType.POISON;
            case ASHFALL -> PotionEffectType.WEAKNESS;
            default -> null;
        };
    }

    /**
     * Obtient l'intensité de l'effet debuff (0-2 généralement)
     */
    public int getDebuffAmplifier() {
        return switch (this) {
            case BLIZZARD -> 1;      // Slowness II
            case FOG -> 0;           // Blindness I (court)
            case SANDSTORM -> 0;     // Blindness I
            case ACID_RAIN -> 0;     // Poison I
            case ASHFALL -> 0;       // Weakness I
            default -> 0;
        };
    }

    /**
     * Obtient la durée de l'effet debuff en ticks
     */
    public int getDebuffDuration() {
        return switch (this) {
            case BLIZZARD -> 20 * 5;     // 5 secondes
            case FOG -> 20 * 3;          // 3 secondes
            case SANDSTORM -> 20 * 4;    // 4 secondes
            case ACID_RAIN -> 20 * 3;    // 3 secondes
            case ASHFALL -> 20 * 6;      // 6 secondes
            default -> 0;
        };
    }
}
