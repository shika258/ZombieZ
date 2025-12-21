package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener pour les déplacements des joueurs
 * Optimisé pour minimiser les calculs inutiles
 * 
 * Stratégie d'optimisation:
 * - Ne vérifie que si le bloc a changé (pas les mouvements de tête)
 * - Cache la dernière position Z pour éviter les recalculs
 * - Vérifie uniquement l'axe Z pour la détection de zone
 */
public class PlayerMoveListener implements Listener {

    private final ZombieZPlugin plugin;

    // Cache de la dernière position Z vérifiée par joueur
    private final Map<UUID, Integer> lastZCache = new ConcurrentHashMap<>();

    // Cache de la dernière position pour calcul de distance
    private final Map<UUID, Location> lastPositionCache = new ConcurrentHashMap<>();

    // Accumulateur de distance parcourue (en blocs)
    private final Map<UUID, Double> distanceAccumulator = new ConcurrentHashMap<>();

    // Seuil pour mettre à jour les missions/achievements (100 blocs)
    private static final double DISTANCE_UPDATE_THRESHOLD = 100.0;

    // Intervalle minimum entre les vérifications de zone (en blocs Z)
    private static final int Z_CHECK_THRESHOLD = 5;

    public PlayerMoveListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gère les déplacements des joueurs
     * Optimisé pour ne pas ralentir le serveur avec 200 joueurs
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Optimisation: Ignorer les mouvements de tête uniquement
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;
        
        // Si le joueur n'a pas changé de bloc, ignorer
        if (from.getBlockX() == to.getBlockX() && 
            from.getBlockY() == to.getBlockY() && 
            from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        int currentZ = to.getBlockZ();

        // ============ TRACKING DISTANCE PARCOURUE ============
        trackDistanceTraveled(player, uuid, from, to);

        // Vérifier si on doit faire une vérification de zone
        Integer lastZ = lastZCache.get(uuid);

        if (lastZ == null || Math.abs(currentZ - lastZ) >= Z_CHECK_THRESHOLD) {
            // Mettre à jour le cache
            lastZCache.put(uuid, currentZ);

            // Vérifier les limites de la map
            checkMapBounds(player, to);

            // La vérification de zone est faite périodiquement par ZoneManager
            // On peut forcer une vérification immédiate si nécessaire
            // plugin.getZoneManager().checkPlayerZone(player);
        }
    }

    /**
     * Track la distance parcourue par le joueur
     * Optimisé pour ne mettre à jour les missions/achievements que tous les 100 blocs
     */
    private void trackDistanceTraveled(Player player, UUID uuid, Location from, Location to) {
        Location lastPos = lastPositionCache.get(uuid);

        if (lastPos == null || !lastPos.getWorld().equals(to.getWorld())) {
            // Première position ou changement de monde
            lastPositionCache.put(uuid, to.clone());
            return;
        }

        // Calculer la distance horizontale (ignorer Y pour éviter les abus avec les ascenseurs)
        double dx = to.getX() - lastPos.getX();
        double dz = to.getZ() - lastPos.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        // Ignorer les téléportations (distance > 10 blocs en un tick)
        if (distance > 10.0) {
            lastPositionCache.put(uuid, to.clone());
            return;
        }

        // Mettre à jour la position cache
        lastPositionCache.put(uuid, to.clone());

        // Accumuler la distance
        double accumulated = distanceAccumulator.getOrDefault(uuid, 0.0) + distance;
        distanceAccumulator.put(uuid, accumulated);

        // Si on a accumulé assez de distance, mettre à jour les trackers
        if (accumulated >= DISTANCE_UPDATE_THRESHOLD) {
            int distanceInBlocks = (int) accumulated;

            // Reset l'accumulateur
            distanceAccumulator.put(uuid, accumulated - distanceInBlocks);

            // Mettre à jour les stats du joueur
            var playerData = plugin.getPlayerDataManager().getPlayer(player);
            if (playerData != null) {
                playerData.addStat("distance_traveled", distanceInBlocks);
                long totalDistance = (long) playerData.getStat("distance_traveled");

                // Missions
                plugin.getMissionManager().updateProgress(player,
                    com.rinaorc.zombiez.progression.MissionManager.MissionTracker.DISTANCE_TRAVELED, distanceInBlocks);

                // Achievements (100km = 100000 blocs)
                var achievementManager = plugin.getAchievementManager();
                achievementManager.checkAndUnlock(player, "distance_walker", (int) Math.min(totalDistance, Integer.MAX_VALUE));
            }
        }
    }

    /**
     * Gère les téléportations (changement de zone immédiat)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        
        if (to == null) return;

        // Forcer une vérification de zone après téléportation
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.getZoneManager().checkPlayerZone(player);
                lastZCache.put(player.getUniqueId(), player.getLocation().getBlockZ());
            }
        }, 1L);
    }

    /**
     * Vérifie si le joueur est dans les limites de la map
     */
    private void checkMapBounds(Player player, Location location) {
        var zoneManager = plugin.getZoneManager();
        
        if (!zoneManager.isInMapBounds(location)) {
            // Joueur hors limites - le repousser doucement
            // ou appliquer des effets négatifs
            
            int x = location.getBlockX();
            int z = location.getBlockZ();
            
            // Limite X
            if (x < -500 || x > 500) {
                // Message d'avertissement
                com.rinaorc.zombiez.utils.MessageUtils.sendActionBar(player, 
                    "§c⚠ Vous atteignez les limites de la map!");
            }
            
            // Limite Z (sud)
            if (z < 0) {
                com.rinaorc.zombiez.utils.MessageUtils.sendActionBar(player, 
                    "§c⚠ Vous ne pouvez pas aller plus au sud!");
            }
            
            // Limite Z (nord) - normalement le boss final est là
            if (z > 10000) {
                com.rinaorc.zombiez.utils.MessageUtils.sendActionBar(player, 
                    "§c⚠ Vous avez atteint la fin du monde...");
            }
        }
    }

    /**
     * Nettoie le cache quand un joueur se déconnecte
     */
    public void removeFromCache(UUID uuid) {
        lastZCache.remove(uuid);
        lastPositionCache.remove(uuid);
        distanceAccumulator.remove(uuid);
    }

    /**
     * Obtient la taille du cache (debug)
     */
    public int getCacheSize() {
        return lastZCache.size();
    }
}
