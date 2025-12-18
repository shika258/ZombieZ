package com.rinaorc.zombiez.managers;

import com.rinaorc.zombiez.ZombieZPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * PerformanceManager - Gestionnaire centralisé de performance pour ZombieZ
 *
 * Fonctionnalités:
 * - Nettoyage intelligent des mobs distants (configurable)
 * - Nettoyage des mobs dans les chunks déchargés
 * - Nettoyage des drops après délai configurable
 * - Statistiques de performance
 *
 * Optimisé pour 200+ joueurs simultanés avec 20 TPS constant
 */
public class PerformanceManager {

    private final ZombieZPlugin plugin;

    // === CONFIGURATION ===
    @Getter
    private double despawnDistance = 64.0;          // Distance de despawn des zombies
    @Getter
    private int maxGlobalMobs = 500;                // Limite globale de mobs
    @Getter
    private int cleanupIntervalTicks = 600;         // Intervalle de cleanup (30 secondes par défaut)
    @Getter
    private int dropRemovalDelayTicks = 200;        // Délai avant suppression des drops (10 secondes)
    @Getter
    private boolean dropCleanupEnabled = true;      // Active/désactive le nettoyage des drops
    @Getter
    private int cleanupBatchSize = 50;              // Nombre de mobs traités par tick

    // === CLÉS PDC ===
    private final NamespacedKey dropKey;            // Clé pour identifier les drops du plugin
    private final NamespacedKey dropTimeKey;        // Clé pour le temps de drop

    // === TRACKING DES DROPS ===
    private final Map<UUID, Long> trackedDrops = new ConcurrentHashMap<>();

    // === TÂCHES PLANIFIÉES ===
    private BukkitTask cleanupTask;
    private BukkitTask dropCleanupTask;

    // === STATISTIQUES ===
    @Getter
    private long totalMobsCleaned = 0;
    @Getter
    private long totalDropsCleaned = 0;
    @Getter
    private long totalChunkCleanups = 0;

    public PerformanceManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.dropKey = new NamespacedKey(plugin, "zombiez_drop");
        this.dropTimeKey = new NamespacedKey(plugin, "zombiez_drop_time");

        loadConfig();
        startTasks();

        plugin.log(Level.INFO, "§7[Performance] Manager initialisé - Distance: " + despawnDistance
                + " | Max Global: " + maxGlobalMobs + " | Cleanup: " + (cleanupIntervalTicks / 20) + "s");
    }

    /**
     * Charge la configuration de performance depuis config.yml
     */
    public void loadConfig() {
        var config = plugin.getConfigManager().getMainConfig();
        if (config == null) return;

        // Charger les valeurs avec defaults
        this.despawnDistance = config.getDouble("performance.despawn-distance", 64.0);
        this.maxGlobalMobs = config.getInt("zombies.max-global", 500);
        this.cleanupIntervalTicks = config.getInt("performance.cleanup-interval", 30) * 20;
        this.dropRemovalDelayTicks = config.getInt("performance.drop-removal-delay", 10) * 20;
        this.dropCleanupEnabled = config.getBoolean("performance.drop-cleanup-enabled", true);
        this.cleanupBatchSize = config.getInt("performance.cleanup-batch-size", 50);

        // Mettre à jour le ZombieManager si déjà initialisé
        if (plugin.getZombieManager() != null) {
            plugin.getZombieManager().loadConfigValues();
        }
    }

    /**
     * Démarre les tâches de nettoyage planifiées
     */
    private void startTasks() {
        // Tâche principale de nettoyage des mobs distants
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::runCleanupCycle,
                cleanupIntervalTicks, cleanupIntervalTicks);

        // Tâche de nettoyage des drops (plus fréquente)
        if (dropCleanupEnabled) {
            dropCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupDrops,
                    100L, 100L); // Toutes les 5 secondes
        }
    }

    /**
     * Arrête les tâches planifiées
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (dropCleanupTask != null) {
            dropCleanupTask.cancel();
        }

        // Cleanup final
        trackedDrops.clear();

        plugin.log(Level.INFO, "§7[Performance] Shutdown - Mobs nettoyés: " + totalMobsCleaned
                + " | Drops nettoyés: " + totalDropsCleaned);
    }

    /**
     * Cycle de nettoyage principal
     * Appelé périodiquement selon cleanupIntervalTicks
     */
    private void runCleanupCycle() {
        // Déléguer le cleanup des zombies au ZombieManager
        if (plugin.getZombieManager() != null) {
            plugin.getZombieManager().cleanupDistantZombies();
        }

        // Déléguer le cleanup des mobs passifs au PassiveMobManager
        if (plugin.getPassiveMobManager() != null) {
            plugin.getPassiveMobManager().cleanupDistantMobs();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SYSTÈME DE NETTOYAGE DES CHUNKS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Nettoie les mobs ZombieZ dans un chunk qui se décharge
     * Appelé par ChunkUnloadListener
     *
     * @param chunk Le chunk à nettoyer
     * @return Le nombre d'entités nettoyées
     */
    public int cleanupChunk(Chunk chunk) {
        int cleaned = 0;

        // Nettoyer les zombies via ZombieManager
        var zombieManager = plugin.getZombieManager();
        if (zombieManager != null) {
            NamespacedKey pdcMobKey = zombieManager.getPdcMobKey();

            for (Entity entity : chunk.getEntities()) {
                if (!(entity instanceof LivingEntity living)) continue;

                // Vérification PDC (prioritaire - ultra-rapide)
                boolean isZombieZMob = living.getPersistentDataContainer()
                        .has(pdcMobKey, PersistentDataType.BYTE);

                // Fallback sur scoreboard tag
                if (!isZombieZMob) {
                    isZombieZMob = entity.getScoreboardTags().contains("zombiez_mob");
                }

                if (isZombieZMob) {
                    zombieManager.onZombieDeath(entity.getUniqueId(), null);
                    entity.remove();
                    cleaned++;
                }
            }
        }

        // Nettoyer les mobs passifs via PassiveMobManager
        var passiveMobManager = plugin.getPassiveMobManager();
        if (passiveMobManager != null) {
            cleaned += passiveMobManager.cleanupChunk(chunk);
        }

        if (cleaned > 0) {
            totalMobsCleaned += cleaned;
            totalChunkCleanups++;
        }

        return cleaned;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SYSTÈME DE NETTOYAGE DES DROPS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Marque un item comme drop de zombie ZombieZ
     * L'item sera automatiquement supprimé après le délai configuré
     *
     * @param item L'entité Item à marquer
     */
    public void markAsZombieZDrop(Item item) {
        if (!dropCleanupEnabled) return;

        long dropTime = System.currentTimeMillis();

        // Marquer via PDC
        var pdc = item.getPersistentDataContainer();
        pdc.set(dropKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(dropTimeKey, PersistentDataType.LONG, dropTime);

        // Tracker pour cleanup rapide
        trackedDrops.put(item.getUniqueId(), dropTime);
    }

    /**
     * Vérifie si un item est un drop de zombie ZombieZ
     */
    public boolean isZombieZDrop(Item item) {
        return item.getPersistentDataContainer().has(dropKey, PersistentDataType.BYTE);
    }

    /**
     * Nettoie les drops expirés
     * Appelé périodiquement
     */
    private void cleanupDrops() {
        if (!dropCleanupEnabled || trackedDrops.isEmpty()) return;

        long now = System.currentTimeMillis();
        long maxAgeMs = (dropRemovalDelayTicks / 20) * 1000L;
        List<UUID> toRemove = new ArrayList<>();

        // Parcourir les drops trackés
        for (var entry : trackedDrops.entrySet()) {
            UUID itemId = entry.getKey();
            long dropTime = entry.getValue();

            // Vérifier si expiré
            if (now - dropTime >= maxAgeMs) {
                Entity entity = Bukkit.getEntity(itemId);
                if (entity instanceof Item item && item.isValid()) {
                    item.remove();
                    totalDropsCleaned++;
                }
                toRemove.add(itemId);
            }
        }

        // Nettoyer les entrées
        for (UUID id : toRemove) {
            trackedDrops.remove(id);
        }
    }

    /**
     * Force le nettoyage de tous les drops du plugin dans un rayon
     *
     * @param center Le centre de la zone
     * @param radius Le rayon de nettoyage
     * @return Le nombre de drops supprimés
     */
    public int forceCleanupDropsInRadius(Location center, double radius) {
        int cleaned = 0;
        double radiusSq = radius * radius;

        for (Entity entity : center.getWorld().getEntities()) {
            if (!(entity instanceof Item item)) continue;

            if (entity.getLocation().distanceSquared(center) <= radiusSq) {
                if (isZombieZDrop(item)) {
                    trackedDrops.remove(item.getUniqueId());
                    item.remove();
                    cleaned++;
                }
            }
        }

        totalDropsCleaned += cleaned;
        return cleaned;
    }

    /**
     * Force le nettoyage de tous les drops du plugin
     *
     * @return Le nombre de drops supprimés
     */
    public int forceCleanupAllDrops() {
        int cleaned = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item item && isZombieZDrop(item)) {
                    item.remove();
                    cleaned++;
                }
            }
        }

        trackedDrops.clear();
        totalDropsCleaned += cleaned;
        return cleaned;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITAIRES ET STATISTIQUES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie si un joueur est à proximité d'une localisation
     * Optimisé avec comparaison de distance au carré
     */
    public boolean hasPlayerNearby(Location location, double radius) {
        double radiusSq = radius * radius;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(location.getWorld())) {
                if (player.getLocation().distanceSquared(location) <= radiusSq) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Obtient le nombre de drops trackés
     */
    public int getTrackedDropsCount() {
        return trackedDrops.size();
    }

    /**
     * Obtient les statistiques de performance sous forme de String
     */
    public String getStats() {
        int currentMobs = plugin.getZombieManager() != null ?
                plugin.getZombieManager().getTotalZombieCount() : 0;

        return String.format(
                "§7[Performance] Mobs: §e%d§7/%d | Cleaned: §a%d §7| Drops Cleaned: §a%d §7| Chunk Cleanups: §a%d",
                currentMobs, maxGlobalMobs, totalMobsCleaned, totalDropsCleaned, totalChunkCleanups
        );
    }

    /**
     * Recharge la configuration et redémarre les tâches si nécessaire
     */
    public void reload() {
        // Arrêter les anciennes tâches
        if (cleanupTask != null) cleanupTask.cancel();
        if (dropCleanupTask != null) dropCleanupTask.cancel();

        // Recharger la config
        loadConfig();

        // Redémarrer les tâches
        startTasks();

        plugin.log(Level.INFO, "§7[Performance] Configuration rechargée");
    }
}
