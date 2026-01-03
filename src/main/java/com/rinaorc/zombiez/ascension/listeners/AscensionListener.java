package com.rinaorc.zombiez.ascension.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.ascension.AscensionData;
import com.rinaorc.zombiez.ascension.AscensionManager;
import com.rinaorc.zombiez.ascension.Mutation;
import com.rinaorc.zombiez.ascension.gui.InsuranceGUI;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Listener pour les événements liés à l'Ascension
 */
public class AscensionListener implements Listener {

    private final ZombieZPlugin plugin;

    public AscensionListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        // Créer les données (vides car reset au reboot)
        manager.getOrCreateData(player);
        // Pas de message spam - le système est visible dans l'ActionBar
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        // Nettoyer les données pour libérer la RAM
        manager.removeData(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        AscensionData data = manager.getData(player);
        if (data == null) return;

        // Si le joueur a des mutations, proposer l'assurance
        if (!data.getActiveMutations().isEmpty()) {
            // Sauvegarder les mutations pour l'assurance (avant reset)
            // Le GUI d'assurance sera ouvert au respawn
            player.setMetadata("ascension_had_mutations",
                new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        // Vérifier si le joueur avait des mutations
        if (player.hasMetadata("ascension_had_mutations")) {
            player.removeMetadata("ascension_had_mutations", plugin);

            AscensionData data = manager.getData(player);
            if (data != null && !data.getActiveMutations().isEmpty()) {
                // Ouvrir le GUI d'assurance avec un délai
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        InsuranceGUI.open(plugin, player, data);
                    }
                }, 20L); // 1 seconde après respawn
                return;
            }
        }

        // Reset normal sans assurance
        manager.resetPlayer(player);
    }

    /**
     * Track les mobs qui touchent le joueur (pour Traque) et déclenche Phase Spectrale
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;

        AscensionManager manager = plugin.getAscensionManager();
        if (manager == null) return;

        AscensionData data = manager.getData(player);
        if (data == null) return;

        // Enregistrer que ce mob nous a touché (pour Traque: +15% dmg contre ceux qui nous ont hit)
        if (data.hasMutation(Mutation.TRAQUE)) {
            data.getHitByMobs().add(attacker.getUniqueId());
        }

        // Phase Spectrale: 10% chance de TP 5 blocs derrière l'attaquant
        if (data.hasMutation(Mutation.PHASE_SPECTRALE)) {
            if (data.canTriggerEffect("phase_spectrale", 2000)) { // Cooldown 2s
                if (Math.random() < 0.10) {
                    triggerPhaseSpectrale(player, attacker);
                }
            }
        }
    }

    /**
     * Téléporte le joueur derrière l'attaquant (Phase Spectrale)
     */
    private void triggerPhaseSpectrale(Player player, LivingEntity attacker) {
        org.bukkit.Location attackerLoc = attacker.getLocation();
        org.bukkit.util.Vector direction = attackerLoc.getDirection().normalize().multiply(-3); // 3 blocs derrière

        org.bukkit.Location teleportLoc = attackerLoc.add(direction);
        teleportLoc.setY(attackerLoc.getWorld().getHighestBlockYAt(teleportLoc) + 1);

        // Vérifier que la destination est sûre
        if (teleportLoc.getBlock().isPassable() && teleportLoc.clone().add(0, 1, 0).getBlock().isPassable()) {
            // Particules à l'origine
            player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);

            // Téléporter
            teleportLoc.setYaw(attackerLoc.getYaw() + 180); // Regarder l'attaquant
            teleportLoc.setPitch(0);
            player.teleport(teleportLoc);

            // Particules à la destination
            player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, teleportLoc.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);

            // Son
            player.playSound(teleportLoc, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.5f);
        }
    }
}
