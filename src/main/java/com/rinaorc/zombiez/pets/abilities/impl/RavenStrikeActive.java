package com.rinaorc.zombiez.pets.abilities.impl;

import com.rinaorc.zombiez.pets.PetData;
import com.rinaorc.zombiez.pets.abilities.PetAbility;
import com.rinaorc.zombiez.pets.abilities.PetDamageUtils;
import com.rinaorc.zombiez.pets.listeners.PetCombatListener;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Ultimate du Corbeau Messager
 * Le corbeau se rue vers un ennemi et lui inflige un pourcentage des dégâts du joueur
 * Le corbeau cible prioritairement l'ennemi que le joueur attaque
 */
@Getter
public class RavenStrikeActive implements PetAbility {
    private final String id;
    private final String displayName;
    private final String description;
    private final double damagePercent; // Pourcentage des dégâts du joueur

    public RavenStrikeActive(String id, String name, String desc, double damagePercent) {
        this.id = id;
        this.displayName = name;
        this.description = desc;
        this.damagePercent = damagePercent;
    }

    @Override
    public boolean isPassive() {
        return false;
    }

    @Override
    public int getCooldown() {
        return 25;
    }

    @Override
    public boolean activate(Player player, PetData petData) {
        // Trouver la cible (priorité: cible du joueur > plus proche)
        Monster target = findTarget(player);
        if (target == null) {
            player.sendMessage("§c[Pet] §7Aucun ennemi à portée!");
            return false;
        }

        // Calculer les dégâts basés sur l'attaque du joueur
        double playerDamage = getPlayerAttackDamage(player);
        double adjustedPercent = damagePercent * petData.getStatMultiplier();
        double finalDamage = playerDamage * adjustedPercent;

        // Position de départ (à côté du joueur)
        Location startLoc = player.getLocation().add(0, 1.5, 0);
        Location targetLoc = target.getLocation().add(0, 1, 0);

        // Son de départ
        player.playSound(player.getLocation(), Sound.ENTITY_PARROT_FLY, 1.0f, 1.2f);

        // Animation du corbeau qui vole vers la cible
        animateRavenStrike(player, startLoc, target, finalDamage, petData);

        return true;
    }

    /**
     * Trouve la meilleure cible pour l'attaque
     */
    private Monster findTarget(Player player) {
        // Priorité 1: La cible que le joueur attaque
        LivingEntity playerTarget = PetCombatListener.getPlayerTarget(player);
        if (playerTarget instanceof Monster monster && monster.isValid() && !monster.isDead()) {
            double distance = player.getLocation().distance(monster.getLocation());
            if (distance <= 15) {
                return monster;
            }
        }

        // Priorité 2: Le monstre le plus proche dans la direction du regard
        Monster closestInSight = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(15, 10, 15)) {
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                Vector toEntity = entity.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
                double dot = player.getLocation().getDirection().dot(toEntity);

                // Dans le champ de vision (cône de 90°)
                if (dot > 0.5) {
                    double distance = player.getLocation().distance(entity.getLocation());
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestInSight = monster;
                    }
                }
            }
        }

        if (closestInSight != null) {
            return closestInSight;
        }

        // Priorité 3: N'importe quel monstre proche
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Monster monster && monster.isValid() && !monster.isDead()) {
                return monster;
            }
        }

        return null;
    }

    /**
     * Obtient les dégâts d'attaque du joueur (supporte armes à distance)
     */
    private double getPlayerAttackDamage(Player player) {
        return PetDamageUtils.getEffectiveDamage(player);
    }

    /**
     * Anime l'attaque du corbeau avec particules
     */
    private void animateRavenStrike(Player player, Location start, Monster target, double damage, PetData petData) {
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 15; // 0.75 seconde d'animation
            Location currentLoc = start.clone();

            @Override
            public void run() {
                if (ticks >= maxTicks || !target.isValid() || target.isDead()) {
                    // Arrivée ou cible morte
                    if (target.isValid() && !target.isDead()) {
                        // Infliger les dégâts
                        target.damage(damage, player);
                        petData.addDamage((long) damage);

                        // Effet d'impact
                        Location impactLoc = target.getLocation().add(0, 1, 0);
                        target.getWorld().spawnParticle(Particle.CRIT, impactLoc, 15, 0.3, 0.3, 0.3, 0.1);
                        target.getWorld().spawnParticle(Particle.DUST, impactLoc, 10,
                            new Particle.DustOptions(Color.fromRGB(30, 30, 30), 1.0f));

                        player.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);

                        // Message
                        player.sendMessage("§a[Pet] §7Frappe du Corbeau! §c" + String.format("%.1f", damage) + " §7dégâts!");
                    }
                    cancel();
                    return;
                }

                // Calculer la position intermédiaire
                Location targetLoc = target.getLocation().add(0, 1, 0);
                Vector direction = targetLoc.toVector().subtract(currentLoc.toVector());
                double distance = direction.length();

                if (distance > 0.5) {
                    direction.normalize().multiply(distance / (maxTicks - ticks));
                    currentLoc.add(direction);
                }

                // Particules de vol (traînée noire/grise pour un corbeau)
                currentLoc.getWorld().spawnParticle(Particle.DUST, currentLoc, 5,
                    new Particle.DustOptions(Color.fromRGB(50, 50, 50), 0.8f));
                currentLoc.getWorld().spawnParticle(Particle.SMOKE, currentLoc, 3, 0.1, 0.1, 0.1, 0);

                // Son de battement d'ailes occasionnel
                if (ticks % 5 == 0) {
                    player.playSound(currentLoc, Sound.ENTITY_PARROT_FLY, 0.5f, 1.5f);
                }

                ticks++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }
}
