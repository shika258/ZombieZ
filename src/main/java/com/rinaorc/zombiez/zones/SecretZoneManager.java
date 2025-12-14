package com.rinaorc.zombiez.zones;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.utils.MessageUtils;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système de zones secrètes et événements spéciaux
 */
public class SecretZoneManager {

    private final ZombieZPlugin plugin;
    
    @Getter
    private final Map<String, SecretZone> secretZones = new ConcurrentHashMap<>();
    
    @Getter
    private final Map<UUID, Set<String>> discoveredZones = new ConcurrentHashMap<>();

    public SecretZoneManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        initSecretZones();
    }

    /**
     * Initialise les zones secrètes
     */
    private void initSecretZones() {
        // Zone 1: Crypte Cachée
        registerZone(new SecretZone(
            "hidden_crypt",
            "Crypte Cachée",
            "§7Une ancienne crypte remplie de trésors",
            3, // Zone minimum
            500, // Points requis
            null // Location définie en jeu
        ));
        
        // Zone 2: Laboratoire Abandonné
        registerZone(new SecretZone(
            "abandoned_lab",
            "Laboratoire Abandonné",
            "§7L'origine du virus...",
            5,
            1000,
            null
        ));
        
        // Zone 3: Sanctuaire des Ombres
        registerZone(new SecretZone(
            "shadow_sanctuary",
            "Sanctuaire des Ombres",
            "§7Un lieu de pouvoir obscur",
            7,
            2000,
            null
        ));
        
        // Zone 4: Nexus Dimensionnel
        registerZone(new SecretZone(
            "dimensional_nexus",
            "Nexus Dimensionnel",
            "§7Le cœur de l'apocalypse",
            9,
            5000,
            null
        ));
    }

    /**
     * Enregistre une zone secrète
     */
    public void registerZone(SecretZone zone) {
        secretZones.put(zone.getId(), zone);
    }

    /**
     * Vérifie si un joueur peut entrer dans une zone secrète
     */
    public boolean canEnter(Player player, String zoneId) {
        SecretZone zone = secretZones.get(zoneId);
        if (zone == null) return false;
        
        // Vérifier le niveau de zone
        int playerZone = plugin.getPlayerDataManager().getPlayer(player).getCurrentZone().get();
        if (playerZone < zone.getRequiredZone()) return false;
        
        // Vérifier les points
        long points = plugin.getPlayerDataManager().getPlayer(player).getPoints().get();
        if (points < zone.getRequiredPoints()) return false;
        
        return true;
    }

    /**
     * Fait entrer un joueur dans une zone secrète
     */
    public boolean enterZone(Player player, String zoneId) {
        SecretZone zone = secretZones.get(zoneId);
        if (zone == null || !canEnter(player, zoneId)) return false;
        
        // Coût d'entrée
        plugin.getEconomyManager().removePoints(player, zone.getRequiredPoints(), "Zone secrète");
        
        // Marquer comme découverte
        discoveredZones.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(zoneId);
        
        // Téléporter
        if (zone.getEntrance() != null) {
            player.teleport(zone.getEntrance());
        }
        
        // Effets
        MessageUtils.sendTitle(player, zone.getColor() + zone.getDisplayName(), zone.getDescription(), 20, 60, 20);
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.5f);
        
        return true;
    }

    /**
     * Vérifie si un joueur a découvert une zone
     */
    public boolean hasDiscovered(Player player, String zoneId) {
        Set<String> discovered = discoveredZones.get(player.getUniqueId());
        return discovered != null && discovered.contains(zoneId);
    }

    /**
     * Obtient les zones découvertes par un joueur
     */
    public Set<String> getDiscoveredZones(Player player) {
        return discoveredZones.getOrDefault(player.getUniqueId(), Collections.emptySet());
    }

    /**
     * Définit l'entrée d'une zone secrète (admin)
     */
    public void setZoneEntrance(String zoneId, Location location) {
        SecretZone zone = secretZones.get(zoneId);
        if (zone != null) {
            zone.setEntrance(location);
        }
    }

    /**
     * Représente une zone secrète
     */
    @Getter
    public static class SecretZone {
        private final String id;
        private final String displayName;
        private final String description;
        private final int requiredZone;
        private final long requiredPoints;
        private final String color;
        private Location entrance;

        public SecretZone(String id, String displayName, String description, int requiredZone, long requiredPoints, Location entrance) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.requiredZone = requiredZone;
            this.requiredPoints = requiredPoints;
            this.entrance = entrance;
            this.color = getColorForZone(requiredZone);
        }

        private static String getColorForZone(int zone) {
            return switch (zone) {
                case 1, 2, 3 -> "§a";
                case 4, 5, 6 -> "§e";
                case 7, 8 -> "§6";
                case 9, 10 -> "§c";
                default -> "§7";
            };
        }

        public void setEntrance(Location entrance) {
            this.entrance = entrance;
        }
    }
}
