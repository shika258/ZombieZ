package com.rinaorc.zombiez.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utilitaires pour les opérations sur les locations
 */
public class LocationUtils {

    /**
     * Calcule la distance entre deux locations (même monde)
     */
    public static double distance(Location loc1, Location loc2) {
        if (loc1.getWorld() != loc2.getWorld()) return Double.MAX_VALUE;
        return loc1.distance(loc2);
    }

    /**
     * Calcule la distance horizontale (ignore Y)
     */
    public static double distanceHorizontal(Location loc1, Location loc2) {
        if (loc1.getWorld() != loc2.getWorld()) return Double.MAX_VALUE;
        double dx = loc1.getX() - loc2.getX();
        double dz = loc1.getZ() - loc2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Calcule la distance au carré (plus rapide, pour comparaisons)
     */
    public static double distanceSquared(Location loc1, Location loc2) {
        if (loc1.getWorld() != loc2.getWorld()) return Double.MAX_VALUE;
        return loc1.distanceSquared(loc2);
    }

    /**
     * Vérifie si deux locations sont dans le même bloc
     */
    public static boolean isSameBlock(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ() &&
               loc1.getWorld() == loc2.getWorld();
    }

    /**
     * Vérifie si une location est dans un rayon donné d'une autre
     */
    public static boolean isWithinRadius(Location center, Location target, double radius) {
        if (center.getWorld() != target.getWorld()) return false;
        return center.distanceSquared(target) <= radius * radius;
    }

    /**
     * Obtient une location sûre (au-dessus du sol)
     */
    public static Location getSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return location;

        Location safe = location.clone();
        int highestY = world.getHighestBlockYAt(safe);
        safe.setY(highestY + 1);
        
        return safe;
    }

    /**
     * Obtient une location aléatoire dans un rayon
     */
    public static Location getRandomLocationInRadius(Location center, double minRadius, double maxRadius) {
        double angle = Math.random() * 2 * Math.PI;
        double distance = minRadius + Math.random() * (maxRadius - minRadius);
        
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        
        Location loc = new Location(center.getWorld(), x, center.getY(), z);
        return getSafeLocation(loc);
    }

    /**
     * Obtient plusieurs locations aléatoires dans un rayon
     */
    public static List<Location> getRandomLocationsInRadius(Location center, double minRadius, 
                                                            double maxRadius, int count) {
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            locations.add(getRandomLocationInRadius(center, minRadius, maxRadius));
        }
        return locations;
    }

    /**
     * Obtient le joueur le plus proche d'une location
     */
    public static Player getNearestPlayer(Location location, double maxDistance) {
        World world = location.getWorld();
        if (world == null) return null;

        Player nearest = null;
        double nearestDist = maxDistance * maxDistance;

        for (Player player : world.getPlayers()) {
            double dist = player.getLocation().distanceSquared(location);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }

        return nearest;
    }

    /**
     * Obtient tous les joueurs dans un rayon
     */
    public static List<Player> getPlayersInRadius(Location center, double radius) {
        List<Player> players = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) return players;

        double radiusSq = radius * radius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radiusSq) {
                players.add(player);
            }
        }

        return players;
    }

    /**
     * Obtient toutes les entités dans un rayon
     */
    public static Collection<Entity> getEntitiesInRadius(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) return List.of();
        return world.getNearbyEntities(center, radius, radius, radius);
    }

    /**
     * Vérifie si un bloc est solide
     */
    public static boolean isSolidBlock(Location location) {
        Block block = location.getBlock();
        return block.getType().isSolid();
    }

    /**
     * Vérifie si une location est sûre (pas dans un bloc solide, pas en l'air)
     */
    public static boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);

        return !feet.getType().isSolid() && 
               !head.getType().isSolid() && 
               ground.getType().isSolid();
    }

    /**
     * Cherche une location sûre proche
     */
    public static Location findSafeLocationNear(Location location, int maxAttempts) {
        if (isSafeLocation(location)) return location;

        for (int i = 0; i < maxAttempts; i++) {
            int dx = (int) ((Math.random() - 0.5) * 10);
            int dz = (int) ((Math.random() - 0.5) * 10);
            
            Location test = location.clone().add(dx, 0, dz);
            test = getSafeLocation(test);
            
            if (isSafeLocation(test)) {
                return test;
            }
        }

        return getSafeLocation(location);
    }

    /**
     * Convertit une location en string lisible
     */
    public static String toString(Location location) {
        return String.format("%.1f, %.1f, %.1f", 
            location.getX(), location.getY(), location.getZ());
    }

    /**
     * Convertit une location en string avec bloc
     */
    public static String toBlockString(Location location) {
        return String.format("%d, %d, %d", 
            location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Parse une string en location (format: "world,x,y,z")
     */
    public static Location fromString(String str, org.bukkit.Server server) {
        String[] parts = str.split(",");
        if (parts.length < 4) return null;

        World world = server.getWorld(parts[0].trim());
        if (world == null) return null;

        try {
            double x = Double.parseDouble(parts[1].trim());
            double y = Double.parseDouble(parts[2].trim());
            double z = Double.parseDouble(parts[3].trim());
            
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5].trim()) : 0;
            
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Sérialise une location en string
     */
    public static String serialize(Location location) {
        return String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f",
            location.getWorld().getName(),
            location.getX(), location.getY(), location.getZ(),
            location.getYaw(), location.getPitch());
    }

    /**
     * Calcule la direction entre deux points
     */
    public static org.bukkit.util.Vector getDirection(Location from, Location to) {
        return to.toVector().subtract(from.toVector()).normalize();
    }

    /**
     * Obtient la location regardée par le joueur
     */
    public static Location getTargetLocation(Player player, int maxDistance) {
        Block block = player.getTargetBlockExact(maxDistance);
        return block != null ? block.getLocation() : null;
    }

    /**
     * Vérifie s'il y a une ligne de vue entre deux locations
     */
    public static boolean hasLineOfSight(Location from, Location to) {
        if (from.getWorld() != to.getWorld()) return false;
        
        org.bukkit.util.Vector direction = getDirection(from, to);
        double distance = from.distance(to);
        
        Location current = from.clone();
        for (double d = 0; d < distance; d += 0.5) {
            current.add(direction.clone().multiply(0.5));
            if (current.getBlock().getType().isSolid()) {
                return false;
            }
        }
        
        return true;
    }
}
