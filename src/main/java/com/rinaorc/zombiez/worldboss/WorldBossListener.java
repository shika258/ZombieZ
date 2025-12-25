package com.rinaorc.zombiez.worldboss;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.worldboss.bosses.ButcherBoss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

/**
 * Listener pour les événements liés aux World Boss
 *
 * Gère:
 * - Dégâts infligés aux boss (tracking)
 * - Mort des boss (récompenses)
 * - Attaques des boss sur les joueurs (mécaniques spéciales)
 * - Ciblage des boss
 */
public class WorldBossListener implements Listener {

    private final ZombieZPlugin plugin;
    private final WorldBossManager manager;

    public WorldBossListener(ZombieZPlugin plugin, WorldBossManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * Gère les dégâts infligés aux World Boss
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBossDamaged(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();

        // Vérifier si c'est un World Boss
        if (!(damaged instanceof Zombie)) return;
        if (!damaged.getScoreboardTags().contains("world_boss")) return;

        WorldBoss boss = manager.getBossByEntity(damaged);
        if (boss == null || !boss.isActive()) return;

        // Identifier l'attaquant
        Player attacker = getPlayerAttacker(event.getDamager());
        if (attacker == null) return;

        // Enregistrer les dégâts
        double finalDamage = event.getFinalDamage();
        boss.onDamage(attacker, finalDamage);
    }

    /**
     * Gère les attaques des World Boss sur les joueurs
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBossAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // Vérifier si l'attaquant est un World Boss
        if (!(damager instanceof Zombie)) return;
        if (!damager.getScoreboardTags().contains("world_boss")) return;
        if (!(victim instanceof Player player)) return;

        WorldBoss boss = manager.getBossByEntity(damager);
        if (boss == null || !boss.isActive()) return;

        // Mécaniques spéciales selon le type de boss
        if (boss instanceof ButcherBoss butcher) {
            // Le Boucher gagne de la résistance en frappant
            butcher.onAttackPlayer(player);
        }

        // Bonus de dégâts pour les World Boss
        double bonusDamage = boss.getZoneId() * 0.5;
        event.setDamage(event.getDamage() + bonusDamage);
    }

    /**
     * Gère la mort des World Boss
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Vérifier si c'est un World Boss
        if (!(entity instanceof Zombie)) return;
        if (!entity.getScoreboardTags().contains("world_boss")) return;

        WorldBoss boss = manager.getBossByEntity(entity);
        if (boss == null) return;

        // Trouver le killer
        Player killer = entity.getKiller();
        if (killer == null) {
            // Chercher le top damager comme killer
            var topDamager = boss.getDamageDealt().entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(e -> plugin.getServer().getPlayer(e.getKey()))
                .orElse(null);

            if (topDamager != null) {
                killer = topDamager;
            }
        }

        // Empêcher les drops vanilla
        event.getDrops().clear();
        event.setDroppedExp(0);

        // Gérer la mort via le manager
        if (killer != null) {
            manager.onBossDeath(boss, killer);
        } else {
            // Pas de killer identifié, juste nettoyer
            manager.removeBoss(boss, "Mort sans killer identifié");
        }
    }

    /**
     * Force le ciblage des World Boss sur les joueurs uniquement
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBossTarget(EntityTargetLivingEntityEvent event) {
        Entity entity = event.getEntity();

        // Vérifier si c'est un World Boss
        if (!(entity instanceof Zombie)) return;
        if (!entity.getScoreboardTags().contains("world_boss")) return;

        // Les World Boss ne ciblent que les joueurs
        LivingEntity target = event.getTarget();
        if (target != null && !(target instanceof Player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Obtient le joueur qui a causé les dégâts (gère les projectiles, etc.)
     */
    private Player getPlayerAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        // Projectile
        if (damager instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player player) {
                return player;
            }
        }

        // TNT
        if (damager instanceof org.bukkit.entity.TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player player) {
                return player;
            }
        }

        // AreaEffectCloud (potions)
        if (damager instanceof org.bukkit.entity.AreaEffectCloud cloud) {
            if (cloud.getSource() instanceof Player player) {
                return player;
            }
        }

        return null;
    }
}
