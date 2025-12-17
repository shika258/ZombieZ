package com.rinaorc.zombiez.pets.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Listener pour les événements de connexion/déconnexion et protection des pets
 */
public class PetConnectionListener implements Listener {

    private final ZombieZPlugin plugin;

    public PetConnectionListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge les données pets quand un joueur se connecte
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPetManager().onPlayerJoin(event.getPlayer());
    }

    /**
     * Sauvegarde les données pets quand un joueur se déconnecte
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPetManager().onPlayerQuit(event.getPlayer());
    }

    /**
     * Empêche les pets de prendre des dégâts
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPetDamage(EntityDamageEvent event) {
        if (isPetEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Empêche les pets d'être ciblés par les mobs
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() != null && isPetEntity(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    /**
     * Empêche les dégâts aux pets des autres joueurs
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPetDamageByEntity(EntityDamageByEntityEvent event) {
        if (isPetEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /**
     * Nettoie les pets quand un chunk est déchargé
     */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (isPetEntity(entity)) {
                entity.remove();
            }
        }
    }

    /**
     * Vérifie si une entité est un pet
     */
    private boolean isPetEntity(Entity entity) {
        return entity.getScoreboardTags().contains("zombiez_pet");
    }
}
