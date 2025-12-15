package com.rinaorc.zombiez.items.power.impl;

import com.rinaorc.zombiez.items.power.Power;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pouvoir: Éclair en Chaîne
 *
 * La foudre rebondit entre plusieurs ennemis, infligeant des dégâts décroissants.
 * Plus vous touchez d'ennemis, plus le rebond continue loin.
 *
 * Scaling ILVL:
 * - Dégâts initial: 6 * (ILVL / 10)
 * - Nombre de rebonds: 2 + (ILVL / 25)
 * - Rayon de rebond: 6 + (ILVL / 50)
 * - Réduction par rebond: 25% (fixe)
 */
public class ChainLightningPower extends Power {

    // Paramètres de base
    private double baseDamage = 6.0;
    private int baseBounces = 2;
    private double baseBounceRadius = 6.0;
    private double damageReduction = 0.25; // 25% de moins par rebond

    // Paramètres de scaling
    private double damagePerILVL = 0.6;
    private double bouncesPerILVL = 0.04; // 1 rebond tous les 25 ILVL
    private double radiusPerILVL = 0.02;

    public ChainLightningPower() {
        super("chain_lightning", "Éclair en Chaîne",
            "La foudre rebondit entre vos ennemis");

        this.baseProcChance = 0.12;  // 12% de chance de proc
        this.cooldownMs = 10000;     // 10 secondes de cooldown
        this.minimumRarity = Rarity.EPIC;
    }

    @Override
    public void trigger(Player player, LivingEntity target, int itemLevel) {
        if (!canProc(player, itemLevel)) {
            return;
        }

        if (target == null) {
            return;
        }

        applyCooldown(player);

        // Calculer les valeurs scalées
        double initialDamage = calculateDamage(itemLevel);
        int maxBounces = calculateBounces(itemLevel);
        double bounceRadius = calculateBounceRadius(itemLevel);

        // Notification
        player.sendMessage("§e⚡ §6Éclair en Chaîne! §7(Max: §e" + maxBounces + " rebonds§7)");

        // Démarrer la chaîne
        chainLightning(player, target, initialDamage, maxBounces, bounceRadius, new HashSet<>(), 0);
    }

    /**
     * Fait rebondir la foudre récursivement
     */
    private void chainLightning(Player player, LivingEntity currentTarget, double damage,
                                int remainingBounces, double radius, Set<LivingEntity> hitTargets,
                                int bounceNumber) {

        if (currentTarget == null || !currentTarget.isValid() || remainingBounces <= 0) {
            return;
        }

        // Marquer cette cible comme touchée
        hitTargets.add(currentTarget);

        // Infliger les dégâts
        currentTarget.damage(damage, player);

        // Effets visuels
        Location targetLoc = currentTarget.getEyeLocation();
        targetLoc.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK,
            targetLoc,
            20,
            0.3, 0.5, 0.3,
            0.1
        );

        targetLoc.getWorld().spawnParticle(
            Particle.FLASH,
            targetLoc,
            1
        );

        // Son différent selon le rebond
        float pitch = 1.0f + (bounceNumber * 0.15f);
        targetLoc.getWorld().playSound(
            targetLoc,
            Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
            0.5f,
            pitch
        );

        // Chercher la prochaine cible
        LivingEntity nextTarget = findNextTarget(currentTarget.getLocation(), radius, hitTargets, player);

        if (nextTarget != null) {
            final Location fromLoc = currentTarget.getEyeLocation();
            final Location toLoc = nextTarget.getEyeLocation();

            // Animation de l'éclair entre les deux cibles
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 5;

                @Override
                public void run() {
                    if (ticks >= maxTicks) {
                        // Passer à la cible suivante
                        double nextDamage = damage * (1.0 - damageReduction);
                        chainLightning(player, nextTarget, nextDamage, remainingBounces - 1,
                            radius, hitTargets, bounceNumber + 1);
                        cancel();
                        return;
                    }

                    double progress = (double) ticks / maxTicks;

                    // Interpolation avec ondulation
                    double x = fromLoc.getX() + (toLoc.getX() - fromLoc.getX()) * progress;
                    double y = fromLoc.getY() + (toLoc.getY() - fromLoc.getY()) * progress;
                    double z = fromLoc.getZ() + (toLoc.getZ() - fromLoc.getZ()) * progress;

                    // Ajouter un effet de zigzag
                    double offset = Math.sin(progress * Math.PI * 4) * 0.3;
                    y += offset;

                    Location boltLoc = new Location(fromLoc.getWorld(), x, y, z);

                    // Particules d'éclair
                    boltLoc.getWorld().spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        boltLoc,
                        5,
                        0.1, 0.1, 0.1,
                        0.05
                    );

                    ticks++;
                }
            }.runTaskTimer(
                org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
                3L,
                1L
            );
        }
    }

    /**
     * Trouve la prochaine cible pour le rebond
     */
    private LivingEntity findNextTarget(Location from, double radius, Set<LivingEntity> hitTargets, Player player) {
        LivingEntity closest = null;
        double closestDistance = radius;

        for (Entity entity : from.getWorld().getNearbyEntities(from, radius, radius, radius)) {
            if (entity instanceof LivingEntity livingEntity && entity != player) {
                // Ne pas cibler les joueurs (sauf si PvP)
                if (livingEntity instanceof Player && !livingEntity.getWorld().getPVP()) {
                    continue;
                }

                // Ne pas toucher la même cible deux fois
                if (hitTargets.contains(livingEntity)) {
                    continue;
                }

                double distance = from.distance(livingEntity.getLocation());
                if (distance < closestDistance) {
                    closest = livingEntity;
                    closestDistance = distance;
                }
            }
        }

        return closest;
    }

    /**
     * Calcule les dégâts selon l'ILVL
     */
    private double calculateDamage(int itemLevel) {
        return baseDamage + (itemLevel * damagePerILVL);
    }

    /**
     * Calcule le nombre de rebonds selon l'ILVL
     */
    private int calculateBounces(int itemLevel) {
        return (int) (baseBounces + (itemLevel * bouncesPerILVL));
    }

    /**
     * Calcule le rayon de rebond selon l'ILVL
     */
    private double calculateBounceRadius(int itemLevel) {
        return baseBounceRadius + (itemLevel * radiusPerILVL);
    }

    @Override
    protected List<String> getPowerStats(int itemLevel) {
        List<String> stats = new ArrayList<>();
        stats.add("§8Dégâts initial: §c" + String.format("%.1f", calculateDamage(itemLevel)));
        stats.add("§8Rebonds: §e" + calculateBounces(itemLevel));
        stats.add("§8Rayon: §e" + String.format("%.1f", calculateBounceRadius(itemLevel)) + " blocs");
        stats.add("§8Réduction: §c-25% par rebond");
        return stats;
    }
}
