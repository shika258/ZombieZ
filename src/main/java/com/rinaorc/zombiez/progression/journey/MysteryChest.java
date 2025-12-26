package com.rinaorc.zombiez.progression.journey;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Représente les coffres mystères à découvrir dans le cadre du parcours
 *
 * Chaque coffre:
 * - A une position fixe dans le monde
 * - Est associé à une étape du parcours
 * - Affiche un hologramme indiquant s'il a été découvert par le joueur
 */
@Getter
public enum MysteryChest {

    // ==================== CHAPITRE 1 ====================

    CHAPTER_1_CHEST(
        "chest_1_1",
        JourneyChapter.CHAPTER_1,
        1, // Zone ID
        "Coffre Mystérieux de la Zone 1",
        625, 93, 9853,
        3.0 // Rayon de détection en blocs
    ),

    // ==================== CHAPITRE 2 ====================

    CHAPTER_2_ZONE2_CHEST(
        "chest_2_zone2",
        JourneyChapter.CHAPTER_2,
        2, // Zone ID
        "Coffre Mystérieux de la Zone 2",
        373, 94, 9767,
        3.0
    ),

    CHAPTER_2_ZONE3_CHEST(
        "chest_2_zone3",
        JourneyChapter.CHAPTER_2,
        3, // Zone ID
        "Coffre Mystérieux de la Zone 3",
        463, 121, 9440,
        3.0
    );

    private final String id;
    private final JourneyChapter chapter;
    private final int zoneId;
    private final String displayName;
    private final int x;
    private final int y;
    private final int z;
    private final double detectionRadius;

    MysteryChest(String id, JourneyChapter chapter, int zoneId, String displayName,
                 int x, int y, int z, double detectionRadius) {
        this.id = id;
        this.chapter = chapter;
        this.zoneId = zoneId;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.detectionRadius = detectionRadius;
    }

    /**
     * Obtient un coffre par son ID de zone
     */
    public static MysteryChest getByZoneId(int zoneId) {
        for (MysteryChest chest : values()) {
            if (chest.zoneId == zoneId) return chest;
        }
        return null;
    }

    /**
     * Obtient la Location du coffre dans un monde donné
     */
    public Location getLocation(World world) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    /**
     * Vérifie si un joueur est à portée de détection du coffre
     */
    public boolean isPlayerInRange(Location playerLocation) {
        if (playerLocation == null || playerLocation.getWorld() == null) return false;

        double dx = playerLocation.getX() - (x + 0.5);
        double dy = playerLocation.getY() - y;
        double dz = playerLocation.getZ() - (z + 0.5);

        // Distance 3D
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        return distanceSquared <= (detectionRadius * detectionRadius);
    }

    /**
     * Obtient un coffre par son ID
     */
    public static MysteryChest getById(String id) {
        for (MysteryChest chest : values()) {
            if (chest.id.equals(id)) return chest;
        }
        return null;
    }

    /**
     * Obtient tous les coffres d'un chapitre
     */
    public static java.util.List<MysteryChest> getChestsForChapter(JourneyChapter chapter) {
        java.util.List<MysteryChest> chests = new java.util.ArrayList<>();
        for (MysteryChest chest : values()) {
            if (chest.chapter == chapter) {
                chests.add(chest);
            }
        }
        return chests;
    }
}
