package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Listener pour nettoyer les mobs ZombieZ quand un chunk est déchargé
 * Évite les mobs orphelins qui restent en mémoire
 */
public class ChunkUnloadListener implements Listener {

    private final ZombieZPlugin plugin;

    public ChunkUnloadListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        // Nettoyer tous les mobs ZombieZ dans ce chunk
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity) {
                // Vérifier si c'est un mob ZombieZ
                if (entity.getScoreboardTags().contains("zombiez_mob")) {
                    // Notifier le ZombieManager avant de supprimer
                    if (plugin.getZombieManager() != null) {
                        plugin.getZombieManager().onZombieDeath(entity.getUniqueId(), null);
                    }
                    entity.remove();
                }

                // Nettoyer aussi les mobs passifs du plugin
                if (entity.getScoreboardTags().contains("zombiez_passive")) {
                    entity.remove();
                }
            }
        }
    }
}
