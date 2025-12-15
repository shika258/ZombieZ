package com.rinaorc.zombiez.consumables;

import lombok.Getter;
import org.bukkit.Material;

/**
 * Types de consommables avec leurs propriétés de base
 * Les stats finales sont calculées avec zone + rarity scaling
 */
@Getter
public enum ConsumableType {
    // Explosifs
    TNT_GRENADE("Grenade TNT", Material.TNT, ConsumableCategory.EXPLOSIVE,
        "Une grenade explosive qui inflige des dégâts de zone",
        10.0, 4.0, 3.0), // baseDamage, baseRadius, baseDelay

    INCENDIARY_BOMB("Bombe Incendiaire", Material.FIRE_CHARGE, ConsumableCategory.EXPLOSIVE,
        "Embrase le sol et inflige des dégâts continus",
        3.0, 3.0, 5.0), // baseDPS, baseRadius, baseDuration

    STICKY_CHARGE("Charge Collante", Material.SLIME_BALL, ConsumableCategory.EXPLOSIVE,
        "Se colle aux zombies et explose après un délai",
        15.0, 2.0, 1.5), // baseDamage, splashRadius, fuseTime

    ACID_JAR("Bocal d'Acide", Material.FERMENTED_SPIDER_EYE, ConsumableCategory.EXPLOSIVE,
        "Crée une zone de poison affectant les zombies",
        2.0, 3.0, 5.0), // baseDPS, baseRadius, baseDuration

    // Mobilité
    JETPACK("Jetpack", Material.FIREWORK_ROCKET, ConsumableCategory.MOBILITY,
        "Un jetpack avec carburant limité pour s'envoler",
        5.0, 0.5, 0.0), // baseFuel (secondes), fuelConsumptionRate, unused

    GRAPPLING_HOOK("Grappin", Material.FISHING_ROD, ConsumableCategory.MOBILITY,
        "Permet de se propulser vers un point",
        30.0, 0.0, 10.0), // baseRange, unused, baseCooldown

    UNSTABLE_PEARL("Perle Instable", Material.ENDER_PEARL, ConsumableCategory.MOBILITY,
        "Téléportation courte avec dégâts réduits",
        15.0, 1.0, 5.0), // baseRange, baseSelfDamage, baseCooldown

    // Contrôle de foule
    COBWEB_TRAP("Piège à Toile", Material.COBWEB, ConsumableCategory.CROWD_CONTROL,
        "Place des toiles temporaires pour ralentir les zombies",
        1.0, 5.0, 0.0), // baseCount, baseDuration, unused

    DECOY("Leurre", Material.BELL, ConsumableCategory.CROWD_CONTROL,
        "Attire les zombies vers un point",
        5.0, 10.0, 0.0), // baseDuration, baseAggroRadius, unused

    TURRET("Tourelle Golem", Material.SNOWBALL, ConsumableCategory.SUMMON,
        "Invoque une mini tourelle qui tire sur les zombies",
        2.0, 15.0, 10.0), // baseDamage, baseDuration, baseRange

    // Soins et Survie
    BANDAGE("Bandage", Material.PAPER, ConsumableCategory.HEALING,
        "Soigne et applique une régénération courte",
        4.0, 5.0, 0.0), // baseHeal, baseRegenDuration, unused

    ANTIDOTE("Antidote", Material.GLASS_BOTTLE, ConsumableCategory.HEALING,
        "Purge les effets négatifs et donne une immunité courte",
        0.0, 5.0, 0.0), // unused, baseImmunityDuration, unused

    ADRENALINE_KIT("Kit d'Adrénaline", Material.BLAZE_POWDER, ConsumableCategory.HEALING,
        "Soin d'urgence avec boost de vitesse temporaire",
        6.0, 5.0, 3.0); // baseHeal, baseRegenDuration, baseSpeedDuration

    private final String displayName;
    private final Material material;
    private final ConsumableCategory category;
    private final String description;

    // Stats de base (interprétées différemment selon le type)
    private final double baseStat1;
    private final double baseStat2;
    private final double baseStat3;

    ConsumableType(String displayName, Material material, ConsumableCategory category,
                   String description, double baseStat1, double baseStat2, double baseStat3) {
        this.displayName = displayName;
        this.material = material;
        this.category = category;
        this.description = description;
        this.baseStat1 = baseStat1;
        this.baseStat2 = baseStat2;
        this.baseStat3 = baseStat3;
    }

    /**
     * Catégories de consommables
     */
    @Getter
    public enum ConsumableCategory {
        EXPLOSIVE("§c", "Explosif"),
        MOBILITY("§b", "Mobilité"),
        CROWD_CONTROL("§e", "Contrôle"),
        HEALING("§a", "Soin"),
        SUMMON("§d", "Invocation");

        private final String color;
        private final String displayName;

        ConsumableCategory(String color, String displayName) {
            this.color = color;
            this.displayName = displayName;
        }
    }

    /**
     * Calcule le scaling de zone (1.0 à 3.5 sur 50 zones)
     */
    public static double getZoneMultiplier(int zoneId) {
        return 1.0 + (zoneId * 0.05); // Zone 50 = 3.5x
    }

    /**
     * Calcule une stat scalée pour une zone et rareté données
     */
    public double calculateScaledStat1(int zoneId, ConsumableRarity rarity) {
        return baseStat1 * getZoneMultiplier(zoneId) * rarity.getStatMultiplier();
    }

    public double calculateScaledStat2(int zoneId, ConsumableRarity rarity) {
        return baseStat2 * getZoneMultiplier(zoneId) * rarity.getStatMultiplier();
    }

    public double calculateScaledStat3(int zoneId, ConsumableRarity rarity) {
        return baseStat3 * getZoneMultiplier(zoneId) * rarity.getStatMultiplier();
    }
}
