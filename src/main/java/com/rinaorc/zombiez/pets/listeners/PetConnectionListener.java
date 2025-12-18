package com.rinaorc.zombiez.pets.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.pets.PlayerPetData;
import com.rinaorc.zombiez.pets.abilities.impl.PassiveAbilityCleanup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkUnloadEvent;

/**
 * Listener pour les événements de connexion/déconnexion et protection des pets
 * Gère également les téléportations, changements de monde et mort du joueur
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

        // Nettoyer les données en mémoire des abilities pour éviter les fuites
        PassiveAbilityCleanup.cleanupPlayer(event.getPlayer().getUniqueId());
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
     * Gère la mort du joueur - cache temporairement le pet
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Le pet sera caché automatiquement par updateAllPets car player.isDead()
    }

    /**
     * Gère le respawn du joueur - réaffiche le pet
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        PlayerPetData data = plugin.getPetManager().getPlayerData(player.getUniqueId());

        if (data != null && data.getEquippedPet() != null && data.isShowPetEntity()) {
            // Délai pour laisser le joueur respawn complètement
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !player.isDead()) {
                    // Recréer le pet à la nouvelle position
                    plugin.getPetManager().getDisplayManager().removePetDisplay(player);
                    plugin.getPetManager().getDisplayManager().spawnPetDisplay(player, data.getEquippedPet());
                }
            }, 20L);
        }
    }

    /**
     * Gère la téléportation du joueur
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        // Vérifier si c'est un changement de monde
        if (event.getFrom().getWorld() != null && event.getTo() != null &&
            event.getTo().getWorld() != null &&
            !event.getFrom().getWorld().equals(event.getTo().getWorld())) {

            // Retirer le pet de l'ancien monde
            plugin.getPetManager().getDisplayManager().removePetDisplay(player);

            // Le DisplayManager recréera le pet dans le nouveau monde automatiquement
            // lors de la prochaine mise à jour
        }
    }

    /**
     * Gère le changement de monde du joueur
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PlayerPetData data = plugin.getPetManager().getPlayerData(player.getUniqueId());

        if (data != null && data.getEquippedPet() != null && data.isShowPetEntity()) {
            // Délai pour laisser le joueur arriver dans le nouveau monde
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getPetManager().getDisplayManager().spawnPetDisplay(player, data.getEquippedPet());
                }
            }, 10L);
        }
    }

    /**
     * Vérifie si une entité est un pet
     */
    private boolean isPetEntity(Entity entity) {
        return entity.getScoreboardTags().contains("zombiez_pet");
    }
}
