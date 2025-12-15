package com.rinaorc.zombiez.items.power.impl;

import com.rinaorc.zombiez.items.power.Power;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Pouvoir: Clone d'Ombre
 *
 * Invoque un clone d'ombre qui combat à vos côtés pendant quelques secondes.
 * Le clone inflige un pourcentage de vos dégâts et disparaît après la durée.
 *
 * Scaling ILVL:
 * - Dégâts du clone: 30% + (ILVL / 250)% de vos dégâts
 * - Santé du clone: 10 + (ILVL / 5)
 * - Durée: 8s + (ILVL / 20)s
 * - Nombre de clones: 1 + (ILVL / 50)
 */
public class ShadowClonePower extends Power {

    // Paramètres de base
    private double baseCloneDamage = 0.3; // 30% des dégâts du joueur
    private double baseCloneHealth = 10.0;
    private int baseDurationTicks = 160; // 8 secondes
    private int baseCloneCount = 1;

    // Paramètres de scaling
    private double damagePerILVL = 0.004; // +0.4% par ILVL
    private double healthPerILVL = 0.2;
    private double durationPerILVL = 1.0;
    private double cloneCountPerILVL = 0.02; // 1 clone tous les 50 ILVL

    public ShadowClonePower() {
        super("shadow_clone", "Clone d'Ombre",
            "Invoque un clone qui combat à vos côtés");

        this.baseProcChance = 0.08;  // 8% de chance de proc
        this.cooldownMs = 20000;     // 20 secondes de cooldown
        this.minimumRarity = Rarity.LEGENDARY;
    }

    @Override
    public void trigger(Player player, LivingEntity target, int itemLevel) {
        if (!canProc(player, itemLevel)) {
            return;
        }

        applyCooldown(player);

        // Calculer les valeurs scalées
        double cloneDamagePercent = calculateCloneDamage(itemLevel);
        double cloneHealth = calculateCloneHealth(itemLevel);
        int duration = calculateDuration(itemLevel);
        int cloneCount = calculateCloneCount(itemLevel);

        // Notification
        player.sendMessage("§8👥 §7Clone d'Ombre! §8(" + cloneCount +
            " clone" + (cloneCount > 1 ? "s" : "") +
            ", §7" + String.format("%.0f%%", cloneDamagePercent * 100) + " dégâts§8)");

        // Invoquer les clones
        for (int i = 0; i < cloneCount; i++) {
            spawnShadowClone(player, target, cloneDamagePercent, cloneHealth, duration, i);
        }
    }

    /**
     * Invoque un clone d'ombre
     */
    private void spawnShadowClone(Player player, LivingEntity initialTarget,
                                  double damagePercent, double health, int duration, int cloneIndex) {

        // Position d'invocation (derrière ou à côté du joueur)
        double angle = cloneIndex * (2 * Math.PI / Math.max(1, calculateCloneCount(100))) + Math.PI;
        double distance = 2.0;
        double x = Math.cos(angle) * distance;
        double z = Math.sin(angle) * distance;

        Location spawnLoc = player.getLocation().add(x, 0, z);

        // Effet visuel de spawn
        spawnLoc.getWorld().spawnParticle(
            Particle.PORTAL,
            spawnLoc.clone().add(0, 1, 0),
            50,
            0.5, 1, 0.5,
            0.1
        );

        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

        // Créer le clone (utiliser un Zombie invisible avec armure)
        Zombie clone = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);

        clone.setCustomName("§8§l⚔ Clone de " + player.getName() + " §8§l⚔");
        clone.setCustomNameVisible(true);

        // Configuration du clone
        clone.setAdult();
        clone.setShouldBurnInDay(false);
        clone.setRemoveWhenFarAway(false);

        // Santé du clone
        clone.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
        clone.setHealth(health);

        // Vitesse du clone
        clone.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);

        // Dégâts du clone (basé sur le pourcentage des dégâts du joueur)
        double playerDamage = player.getInventory().getItemInMainHand().getType().toString().contains("SWORD") ? 7.0 : 2.0;
        double cloneDamage = playerDamage * damagePercent;
        clone.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(cloneDamage);

        // Invisibilité partielle (fumée d'ombre)
        clone.addPotionEffect(
            new PotionEffect(PotionEffectType.INVISIBILITY, duration, 0, false, false)
        );

        // Copier l'équipement du joueur (visuel)
        clone.getEquipment().setHelmet(player.getInventory().getHelmet());
        clone.getEquipment().setChestplate(player.getInventory().getChestplate());
        clone.getEquipment().setLeggings(player.getInventory().getLeggings());
        clone.getEquipment().setBoots(player.getInventory().getBoots());
        clone.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand());

        // Cibler la même cible que le joueur
        if (initialTarget != null) {
            clone.setTarget(initialTarget);
        }

        // Effets visuels persistants
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!clone.isValid() || clone.isDead() || ticks >= duration) {
                    // Disparition du clone
                    if (clone.isValid()) {
                        Location deathLoc = clone.getLocation();
                        deathLoc.getWorld().spawnParticle(
                            Particle.PORTAL,
                            deathLoc.clone().add(0, 1, 0),
                            30,
                            0.5, 1, 0.5,
                            0.1
                        );
                        deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 0.7f);
                        clone.remove();
                    }
                    cancel();
                    return;
                }

                ticks++;

                // Particules d'ombre autour du clone
                if (ticks % 5 == 0) {
                    clone.getWorld().spawnParticle(
                        Particle.SMOKE,
                        clone.getLocation().clone().add(0, 1, 0),
                        5,
                        0.3, 0.5, 0.3,
                        0.01
                    );

                    clone.getWorld().spawnParticle(
                        Particle.PORTAL,
                        clone.getLocation().clone().add(0, 1, 0),
                        3,
                        0.2, 0.3, 0.2,
                        0.01
                    );
                }

                // Rechercher une cible si le clone n'en a pas
                if (clone.getTarget() == null || !clone.getTarget().isValid()) {
                    LivingEntity nearestEnemy = findNearestEnemy(clone.getLocation(), player);
                    if (nearestEnemy != null) {
                        clone.setTarget(nearestEnemy);
                    }
                }
            }
        }.runTaskTimer(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            0L,
            1L
        );
    }

    /**
     * Trouve l'ennemi le plus proche pour le clone
     */
    private LivingEntity findNearestEnemy(Location center, Player owner) {
        LivingEntity nearest = null;
        double nearestDistance = 20.0;

        for (Entity entity : center.getWorld().getNearbyEntities(center, 20, 20, 20)) {
            if (entity instanceof LivingEntity target && entity != owner) {
                // Ne pas cibler les joueurs (sauf si PvP)
                if (target instanceof Player && !target.getWorld().getPVP()) {
                    continue;
                }

                // Ne pas cibler les zombies (autres clones)
                if (target instanceof Zombie) {
                    continue;
                }

                double distance = center.distance(target.getLocation());
                if (distance < nearestDistance) {
                    nearest = target;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    /**
     * Calcule le pourcentage de dégâts du clone selon l'ILVL
     */
    private double calculateCloneDamage(int itemLevel) {
        return Math.min(1.0, baseCloneDamage + (itemLevel * damagePerILVL));
    }

    /**
     * Calcule la santé du clone selon l'ILVL
     */
    private double calculateCloneHealth(int itemLevel) {
        return baseCloneHealth + (itemLevel * healthPerILVL);
    }

    /**
     * Calcule la durée du clone selon l'ILVL
     */
    private int calculateDuration(int itemLevel) {
        return (int) (baseDurationTicks + (itemLevel * durationPerILVL));
    }

    /**
     * Calcule le nombre de clones selon l'ILVL
     */
    private int calculateCloneCount(int itemLevel) {
        return Math.min(3, (int) (baseCloneCount + (itemLevel * cloneCountPerILVL)));
    }

    @Override
    protected List<String> getPowerStats(int itemLevel) {
        List<String> stats = new ArrayList<>();
        stats.add("§8Dégâts: §c" + String.format("%.0f%%", calculateCloneDamage(itemLevel) * 100) + " des vôtres");
        stats.add("§8Santé: §a" + String.format("%.0f", calculateCloneHealth(itemLevel)) + " ❤");
        stats.add("§8Durée: §e" + String.format("%.1f", calculateDuration(itemLevel) / 20.0) + "s");
        stats.add("§8Nombre: §e" + calculateCloneCount(itemLevel) + " clone" +
            (calculateCloneCount(itemLevel) > 1 ? "s" : ""));
        return stats;
    }
}
