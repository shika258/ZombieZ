package com.rinaorc.zombiez.events.micro;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener pour les micro-evenements
 * Gere les interactions entre joueurs et entites des micro-events
 */
public class MicroEventListener implements Listener {

    private final ZombieZPlugin plugin;
    private final MicroEventManager manager;

    public MicroEventListener(ZombieZPlugin plugin, MicroEventManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * Gere les degats infliges aux entites des micro-events
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (!victim.getScoreboardTags().contains("micro_event_entity")) return;

        // Trouver l'attaquant (joueur)
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj) {
            if (proj.getShooter() instanceof Player p) {
                attacker = p;
            }
        }

        if (attacker == null) {
            // Empecher les autres entites d'attaquer les mobs de micro-event
            event.setCancelled(true);
            return;
        }

        // Deleguer au manager
        boolean handled = manager.handleEntityDamage(victim, attacker, event.getDamage());

        // Si le handler a traité les dégâts (ex: calcul custom ZombieZ), annuler l'event vanilla
        // pour éviter les dégâts doubles
        if (handled) {
            event.setCancelled(true);
        }
    }

    /**
     * Gere la mort des entites des micro-events
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getScoreboardTags().contains("micro_event_entity")) return;

        // Trouver le tueur
        Player killer = entity.getKiller();

        // Empecher les drops vanilla
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Deleguer au manager
        if (killer != null) {
            manager.handleEntityDeath(entity, killer);
        }
    }

    /**
     * Gere la deconnexion d'un joueur - annule son micro-event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.stopPlayerEvent(event.getPlayer());
    }
}
