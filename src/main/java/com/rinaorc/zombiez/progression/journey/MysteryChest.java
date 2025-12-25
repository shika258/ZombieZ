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
        "Coffre Mystérieux de la Zone 1",
        625, 93, 9853,
        3.0 // Rayon de détection en blocs
    );

    // ==================== CHAPITRE 2+ (à ajouter plus tard) ====================

    private final String id;
    private final JourneyChapter chapter;
    private final String displayName;
    private final int x;
    private final int y;
    private final int z;
    private final double detectionRadius;

    MysteryChest(String id, JourneyChapter chapter, String displayName,
                 int x, int y, int z, double detectionRadius) {
        this.id = id;
        this.chapter = chapter;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.detectionRadius = detectionRadius;
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
