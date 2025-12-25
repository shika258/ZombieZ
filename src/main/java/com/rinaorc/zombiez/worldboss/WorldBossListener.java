package com.rinaorc.zombiez.worldboss;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.worldboss.bosses.ButcherBoss;
import com.rinaorc.zombiez.worldboss.procedural.BossModifiers;
import com.rinaorc.zombiez.worldboss.procedural.BossTrait;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

        // Vérifier l'invincibilité (ex: HordeQueen avec sbires)
        if (!boss.canReceiveDamage()) {
            event.setCancelled(true);

            // Feedback au joueur
            Player attacker = getPlayerAttacker(event.getDamager());
            if (attacker != null) {
                boss.onDamage(attacker, 0); // Notifier quand même pour le feedback
            }
            return;
        }

        // Identifier l'attaquant
        Player attacker = getPlayerAttacker(event.getDamager());
        if (attacker == null) return;

        // Enregistrer les dégâts
        double finalDamage = event.getFinalDamage();
        boss.onDamage(attacker, finalDamage);
    }

    /**
     * Gère les attaques des World Boss sur les joueurs
     * Applique les effets procéduraux des traits
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBossAttack(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // Vérifier si l'attaquant est un World Boss
        if (!(damager instanceof Zombie zombie)) return;
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

        // Appliquer les effets procéduraux des traits
        BossModifiers modifiers = boss.getModifiers();
        if (modifiers != null) {
            double finalDamage = event.getFinalDamage();

            // Trait: Venimeux - empoisonne la victime
            if (modifiers.hasTrait(BossTrait.VENOMOUS)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1, true, true));
                player.sendMessage("§2§l☠ §7Vous êtes empoisonné!");
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0),
                    10, 0.3, 0.5, 0.3, new Particle.DustOptions(org.bukkit.Color.GREEN, 1.5f));
            }

            // Trait: Vampirique - le boss se soigne
            if (modifiers.hasLifesteal() && modifiers.getLifestealPercent() > 0) {
                double lifestealPercent = modifiers.getLifestealPercent();
                var maxHealth = zombie.getAttribute(Attribute.MAX_HEALTH);

                // SYNERGIE: Frénésie Sanguine (BERSERKER + VAMPIRIC)
                // Double le lifesteal quand le boss est sous 30% HP
                boolean bloodFrenzy = false;
                if (maxHealth != null && modifiers.hasTrait(BossTrait.BERSERKER)) {
                    double healthPercent = zombie.getHealth() / maxHealth.getValue();
                    if (healthPercent < 0.3) {
                        lifestealPercent *= 2.0; // Double lifesteal!
                        bloodFrenzy = true;
                    }
                }

                double healAmount = finalDamage * lifestealPercent;
                if (maxHealth != null) {
                    double newHealth = Math.min(maxHealth.getValue(), zombie.getHealth() + healAmount);
                    zombie.setHealth(newHealth);

                    // Effet visuel (amplifié si Frénésie Sanguine)
                    if (bloodFrenzy) {
                        zombie.getWorld().spawnParticle(Particle.DUST,
                            zombie.getLocation().add(0, 2, 0), 15, 0.5, 0.5, 0.5,
                            new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 2f));
                        zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.5f);
                        player.sendMessage("§4§l🩸 §7Frénésie Sanguine! Le boss se soigne massivement!");
                    } else {
                        zombie.getWorld().spawnParticle(Particle.HEART,
                            zombie.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0);
                        zombie.getWorld().playSound(zombie.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.5f, 1.2f);
                    }
                }
            }

            // Trait: Enragé - attaque plus vite (effet d'alerte visuel)
            if (modifiers.hasTrait(BossTrait.ENRAGED)) {
                zombie.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                    zombie.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0);
            }
        }
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
