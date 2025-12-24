package com.rinaorc.zombiez.zones;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Représente un refuge (point de sauvegarde et téléportation)
 * Les refuges offrent une zone protégée contre les spawns de zombies
 * et permettent aux joueurs de se téléporter entre eux.
 */
@Getter
@Builder
public class Refuge {

    private final int id;
    private final String name;
    private final String description;

    // Zone protégée (aucun zombie ne spawn dans cette zone)
    private final int protectedMinX, protectedMaxX;
    private final int protectedMinY, protectedMaxY;
    private final int protectedMinZ, protectedMaxZ;

    // Position du beacon pour activer le checkpoint
    private final int beaconX, beaconY, beaconZ;

    // Point de spawn pour la téléportation
    private final double spawnX, spawnY, spawnZ;
    private final float spawnYaw, spawnPitch;

    // Configuration
    private final long cost;
    private final int requiredLevel;

    /**
     * Vérifie si une location est dans la zone protégée du refuge
     */
    public boolean isInProtectedArea(Location loc) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= protectedMinX && x <= protectedMaxX &&
               y >= protectedMinY && y <= protectedMaxY &&
               z >= protectedMinZ && z <= protectedMaxZ;
    }

    /**
     * Vérifie si une position (x, y, z) est dans la zone protégée
     */
    public boolean isInProtectedArea(int x, int y, int z) {
        return x >= protectedMinX && x <= protectedMaxX &&
               y >= protectedMinY && y <= protectedMaxY &&
               z >= protectedMinZ && z <= protectedMaxZ;
    }

    /**
     * Vérifie si un bloc est le beacon de ce refuge
     */
    public boolean isBeaconAt(Location loc) {
        return loc.getBlockX() == beaconX &&
               loc.getBlockY() == beaconY &&
               loc.getBlockZ() == beaconZ;
    }

    /**
     * Vérifie si un bloc est le beacon de ce refuge
     */
    public boolean isBeaconAt(int x, int y, int z) {
        return x == beaconX && y == beaconY && z == beaconZ;
    }

    /**
     * Obtient la location du point de spawn pour la téléportation
     */
    public Location getSpawnLocation(World world) {
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
    }

    /**
     * Obtient la location du point de spawn pour la téléportation
     * Utilise le monde par défaut
     */
    public Location getSpawnLocation() {
        World world = Bukkit.getWorlds().get(0);
        return getSpawnLocation(world);
    }

    /**
     * Obtient la location du beacon
     */
    public Location getBeaconLocation(World world) {
        return new Location(world, beaconX, beaconY, beaconZ);
    }

    /**
     * Obtient le nom d'affichage coloré du refuge
     */
    public String getColoredName() {
        return "§e" + name;
    }

    /**
     * Obtient les informations de la zone protégée sous forme de string
     */
    public String getProtectedAreaInfo() {
        return String.format("X[%d-%d] Y[%d-%d] Z[%d-%d]",
            protectedMinX, protectedMaxX,
            protectedMinY, protectedMaxY,
            protectedMinZ, protectedMaxZ);
    }
}
