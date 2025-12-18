package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener pour gérer les mobs ZombieZ lors du chargement/déchargement de chunks
 *
 * - ChunkUnload: Supprime les mobs pour éviter les orphelins en mémoire
 * - ChunkLoad: Nettoie les mobs orphelins après un reboot serveur
 *
 * Utilise le PDC pour identifier les mobs de manière ultra-performante
 */
public class ChunkUnloadListener implements Listener {

    private final ZombieZPlugin plugin;

    // Flag pour savoir si on est en phase de démarrage (nettoyage agressif)
    private volatile boolean startupPhase = true;
    private static final long STARTUP_DURATION_MS = 60000; // 1 minute après le démarrage

    public ChunkUnloadListener(ZombieZPlugin plugin) {
        this.plugin = plugin;

        // Désactiver la phase de démarrage après 1 minute
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            startupPhase = false;
            plugin.getLogger().info("§7[Performance] Phase de démarrage terminée - nettoyage normal activé");
        }, 20L * 60); // 60 secondes
    }

    /**
     * Nettoie les mobs ZombieZ quand un chunk est déchargé
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        // Déléguer au PerformanceManager pour un cleanup optimisé avec PDC
        if (plugin.getPerformanceManager() != null) {
            plugin.getPerformanceManager().cleanupChunk(chunk);
        }
    }

    /**
     * Nettoie les mobs ZombieZ orphelins quand un chunk est chargé
     * Cela gère le cas où le serveur redémarre et les mobs persistent dans les chunks
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        // En phase de démarrage, nettoyer tous les mobs ZombieZ orphelins
        // Ces mobs ont été spawnés avant le reboot et ne sont plus trackés
        if (startupPhase || !plugin.isFullyLoaded()) {
            cleanupOrphanedMobs(chunk);
            return;
        }

        // Après la phase de démarrage, vérifier seulement les mobs orphelins
        // (ceux avec PDC mais pas dans activeZombies du ZombieManager)
        cleanupOrphanedMobs(chunk);
    }

    /**
     * Nettoie les mobs ZombieZ orphelins dans un chunk
     * Un mob orphelin est un mob avec le marqueur PDC mais qui n'est pas dans le tracking du ZombieManager
     */
    private void cleanupOrphanedMobs(Chunk chunk) {
        var zombieManager = plugin.getZombieManager();
        if (zombieManager == null) return;

        NamespacedKey pdcMobKey = zombieManager.getPdcMobKey();
        int cleaned = 0;

        for (Entity entity : chunk.getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;

            boolean isZombieZMob = false;

            // Vérification PDC (prioritaire - ultra-rapide)
            if (living.getPersistentDataContainer().has(pdcMobKey, PersistentDataType.BYTE)) {
                isZombieZMob = true;
            }

            // Fallback sur scoreboard tag
            if (!isZombieZMob && entity.getScoreboardTags().contains("zombiez_mob")) {
                isZombieZMob = true;
            }

            // Fallback sur metadata (anciens mobs)
            if (!isZombieZMob && entity.hasMetadata("zombiez_type")) {
                isZombieZMob = true;
            }

            if (isZombieZMob) {
                // Vérifier si ce mob est tracké par le ZombieManager
                // Si non, c'est un orphelin (persisté après reboot) -> supprimer
                if (zombieManager.getActiveZombie(entity.getUniqueId()) == null) {
                    entity.remove();
                    cleaned++;
                }
            }

            // Nettoyer aussi les mobs passifs orphelins
            if (entity.getScoreboardTags().contains("zombiez_passive")) {
                entity.remove();
                cleaned++;
            }
        }

        // Log si beaucoup de mobs nettoyés (debug)
        if (cleaned > 5 && plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("§7[ChunkLoad] Nettoyé " + cleaned + " mobs orphelins dans chunk "
                + chunk.getX() + "," + chunk.getZ());
        }
    }
}
