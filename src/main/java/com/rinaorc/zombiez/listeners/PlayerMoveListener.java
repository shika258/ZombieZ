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
    }

    /**
     * Obtient la taille du cache (debug)
     */
    public int getCacheSize() {
        return lastZCache.size();
    }
}
