package com.rinaorc.zombiez.listeners;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.zones.Zone;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listener pour les événements de combat
 */
public class CombatListener implements Listener {

    private final ZombieZPlugin plugin;

    public CombatListener(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gère les dégâts entre entités
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // PvP
        if (damager instanceof Player attacker && victim instanceof Player target) {
            handlePvP(event, attacker, target);
            return;
        }

        // Joueur attaque zombie
        if (damager instanceof Player player && victim instanceof Zombie zombie) {
            handlePlayerAttackZombie(event, player, zombie);
            return;
        }

        // Zombie attaque joueur
        if (damager instanceof Zombie zombie && victim instanceof Player player) {
            handleZombieAttackPlayer(event, zombie, player);
        }
    }

    /**
     * Gère le PvP
     */
    private void handlePvP(EntityDamageByEntityEvent event, Player attacker, Player target) {
        Zone attackerZone = plugin.getZoneManager().getPlayerZone(attacker);
        Zone targetZone = plugin.getZoneManager().getPlayerZone(target);

        // Vérifier si le PvP est activé dans la zone
        boolean pvpAllowed = (attackerZone != null && attackerZone.isPvpEnabled()) ||
                            (targetZone != null && targetZone.isPvpEnabled());

        if (!pvpAllowed) {
            event.setCancelled(true);
            com.rinaorc.zombiez.utils.MessageUtils.sendActionBar(attacker, "§cPvP désactivé dans cette zone!");
        }
    }

    /**
     * Gère les attaques de joueur sur zombie
     */
    private void handlePlayerAttackZombie(EntityDamageByEntityEvent event, Player player, Zombie zombie) {
        // Les dégâts sont gérés normalement
        // On pourrait ajouter des multiplicateurs ici basés sur l'équipement
    }

    /**
     * Gère les attaques de zombie sur joueur
     */
    private void handleZombieAttackPlayer(EntityDamageByEntityEvent event, Zombie zombie, Player player) {
        // Appliquer les réductions de dégâts basées sur la zone
        Zone zone = plugin.getZoneManager().getPlayerZone(player);
        
        if (zone != null) {
            // Les zombies font plus de dégâts dans les zones avancées
            double damageMultiplier = zone.getZombieDamageMultiplier();
            event.setDamage(event.getDamage() * damageMultiplier);
        }
    }

    /**
     * Gère la mort d'une entité (pour les rewards)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) return;

        // Zombie tué par un joueur
        if (entity instanceof Zombie zombie) {
            handleZombieDeath(killer, zombie);
        }
    }

    /**
     * Gère la mort d'un zombie
     */
    private void handleZombieDeath(Player killer, Zombie zombie) {
        Zone zone = plugin.getZoneManager().getPlayerZone(killer);
        int zombieLevel = zone != null ? zone.getZombieLevelAt(killer.getLocation().getBlockZ()) : 1;
        
        // Déterminer le type de zombie (basique pour l'instant)
        String zombieType = "WALKER";
        
        // TODO: Lire les métadonnées du zombie pour déterminer son type réel
        // if (zombie.hasMetadata("zombiez_type")) { ... }

        // Récompenser le joueur
        plugin.getEconomyManager().rewardZombieKill(killer, zombieType, zombieLevel);
    }
}
