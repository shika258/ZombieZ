package com.rinaorc.zombiez.zones;

import lombok.Builder;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Représente une zone de jeu
 * Chaque zone a ses propres caractéristiques de difficulté et de loot
 */
@Data
@Builder
public class Zone {

    // Identification
    private final int id;
    private final String name;
    private final String displayName;
    private final String description;

    // Limites géographiques (axe Z)
    private final int minZ;
    private final int maxZ;

    // Difficulté (1-10)
    private final int difficulty;
    private final int stars; // Étoiles affichées (1-7)

    // Thème visuel
    private final String biomeType;
    private final String theme;

    // Multiplicateurs de la zone
    private final double xpMultiplier;
    private final double lootMultiplier;
    private final double spawnRateMultiplier;
    private final double zombieHealthMultiplier;
    private final double zombieDamageMultiplier;
    private final double zombieSpeedMultiplier;

    // Caractéristiques spéciales
    private final boolean pvpEnabled;
    private final boolean safeZone;
    private final boolean bossZone;

    // Effets environnementaux
    private final String environmentalEffect; // "NONE", "HEAT", "COLD", "TOXIC", "RADIATION", "FIRE"
    private final double environmentalDamage; // Dégâts par seconde
    private final int environmentalInterval; // Intervalle en ticks

    // Refuge associé (si existe)
    private final int refugeId;
    private final Location refugeLocation;

    // Spawn de zombies
    private final int minZombieLevel;
    private final int maxZombieLevel;
    private final String[] allowedZombieTypes;

    // Couleur pour l'affichage
    private final String color; // Code couleur Minecraft (§a, §c, etc.)

    /**
     * Vérifie si une coordonnée Z est dans cette zone
     */
    public boolean containsZ(int z) {
        return z >= minZ && z < maxZ;
    }

    /**
     * Vérifie si une location est dans cette zone
     */
    public boolean contains(Location location) {
        return containsZ(location.getBlockZ());
    }

    /**
     * Obtient le niveau de zombie basé sur la position Z dans la zone
     * IMPORTANT: La progression va de Z élevé (début) vers Z faible (fin)
     */
    public int getZombieLevelAt(int z) {
        if (!containsZ(z)) return minZombieLevel;

        // Interpolation linéaire entre min et max
        // Plus le Z est faible (nord), plus le niveau est élevé
        double progress = (double) (maxZ - z) / (maxZ - minZ);
        return minZombieLevel + (int) (progress * (maxZombieLevel - minZombieLevel));
    }

    /**
     * Obtient le pourcentage de progression dans la zone
     * IMPORTANT: La progression va de Z élevé (0%) vers Z faible (100%)
     */
    public double getProgressPercent(int z) {
        if (!containsZ(z)) return z < minZ ? 0 : 100;
        // Plus le Z est faible (nord), plus la progression est élevée
        return (double) (maxZ - z) / (maxZ - minZ) * 100;
    }

    /**
     * Obtient le nom d'affichage coloré
     */
    public String getColoredName() {
        return color + displayName;
    }

    /**
     * Obtient la représentation en étoiles de la difficulté
     */
    public String getStarsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            sb.append("§e★");
        }
        for (int i = stars; i < 7; i++) {
            sb.append("§7☆");
        }
        return sb.toString();
    }

    /**
     * Obtient le message d'entrée dans la zone
     */
    public String getEntryMessage() {
        return String.format(
            "\n§8§m                              \n" +
            "   %s§l%s\n" +
            "   §7%s\n" +
            "   §7Difficulté: %s\n" +
            "§8§m                              \n",
            color, displayName.toUpperCase(),
            description,
            getStarsDisplay()
        );
    }

    /**
     * Vérifie si c'est une zone dangereuse (effets environnementaux)
     */
    public boolean isDangerous() {
        return !environmentalEffect.equals("NONE") && environmentalDamage > 0;
    }

    /**
     * Crée une zone de spawn par défaut (au sud de la map, Z élevé)
     * Note: Le spawn est situé entre Z=10000 et Z=10200, juste avant la zone 1
     */
    public static Zone createSpawnZone() {
        return Zone.builder()
            .id(0)
            .name("spawn")
            .displayName("Zone de Spawn")
            .description("Zone sécurisée pour les nouveaux survivants")
            .minZ(10000)
            .maxZ(10200)
            .difficulty(0)
            .stars(0)
            .biomeType("PLAINS")
            .theme("safe_village")
            .xpMultiplier(1.0)
            .lootMultiplier(1.0)
            .spawnRateMultiplier(0.0) // Pas de spawn
            .zombieHealthMultiplier(1.0)
            .zombieDamageMultiplier(1.0)
            .zombieSpeedMultiplier(1.0)
            .pvpEnabled(false)
            .safeZone(true)
            .bossZone(false)
            .environmentalEffect("NONE")
            .environmentalDamage(0)
            .environmentalInterval(0)
            .refugeId(0)
            .refugeLocation(null)
            .minZombieLevel(0)
            .maxZombieLevel(0)
            .allowedZombieTypes(new String[]{})
            .color("§a")
            .build();
    }

    /**
     * Builder helper pour créer rapidement une zone standard
     * Adapté pour 50 zones avec scaling progressif
     *
     * @param id ID de la zone (1-50)
     * @param name Nom de la zone
     * @param minZ Coordonnée Z minimale
     * @param maxZ Coordonnée Z maximale
     * @param difficulty Difficulté de la zone (1-10)
     */
    public static ZoneBuilder standardZone(int id, String name, int minZ, int maxZ, int difficulty) {
        // Progression basée sur l'ID de zone (1-50)
        double zoneProgress = (double) id / 50.0; // 0.02 à 1.0

        return Zone.builder()
            .id(id)
            .name(name.toLowerCase().replace(" ", "_"))
            .displayName(name)
            .minZ(minZ)
            .maxZ(maxZ)
            .difficulty(difficulty)
            .stars(Math.min(7, (id / 7) + 1)) // 1-7 étoiles basées sur la progression
            // Multiplicateurs progressifs basés sur la zone (id 1 à 50)
            .xpMultiplier(1.0 + (zoneProgress * 2.5))           // 1.0 → 3.5
            .lootMultiplier(1.0 + (zoneProgress * 2.5))         // 1.0 → 3.5
            .spawnRateMultiplier(0.8 + (zoneProgress * 1.5))    // 0.8 → 2.3
            .zombieHealthMultiplier(1.0 + (zoneProgress * 2.5)) // 1.0 → 3.5
            .zombieDamageMultiplier(1.0 + (zoneProgress * 2.5)) // 1.0 → 3.5
            .zombieSpeedMultiplier(1.0 + (zoneProgress * 0.5))  // 1.0 → 1.5
            .pvpEnabled(false)
            .safeZone(false)
            .bossZone(false)
            .environmentalEffect("NONE")
            .environmentalDamage(0)
            .environmentalInterval(0);
    }
}
