package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Listener pour nettoyer les mobs ZombieZ quand un chunk est déchargé
 * Évite les mobs orphelins qui restent en mémoire
 *
 * Utilise le PerformanceManager pour un cleanup optimisé avec vérification PDC
 */
public class ChunkUnloadListener implements Listener {

    private final ZombieZPlugin plugin;

    public ChunkUnloadListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        // Déléguer au PerformanceManager pour un cleanup optimisé avec PDC
        if (plugin.getPerformanceManager() != null) {
            plugin.getPerformanceManager().cleanupChunk(chunk);
        }
    }
}
