package com.rinaorc.zombiez.items.power.impl;

import com.rinaorc.zombiez.items.power.Power;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pouvoir: Pluie de Météores
 *
 * Fait tomber une pluie de météores de feu dans une zone,
 * infligeant des dégâts massifs et mettant le feu aux ennemis.
 *
 * Scaling ILVL:
 * - Dégâts par météore: 10 * (ILVL / 10)
 * - Nombre de météores: 3 + (ILVL / 20)
 * - Rayon de la zone: 8 + (ILVL / 25)
 * - Durée du feu: 3s + (ILVL / 30)s
 */
public class MeteorShowerPower extends Power {

    private final Random random = new Random();

    // Paramètres de base
    private double baseDamagePerMeteor = 10.0;
    private int baseMeteorCount = 3;
    private double baseZoneRadius = 8.0;
    private int baseFireTicks = 60; // 3 secondes

    // Paramètres de scaling
    private double damagePerILVL = 1.0;
    private double meteorCountPerILVL = 0.05; // 1 tous les 20 ILVL
    private double radiusPerILVL = 0.04;
    private double fireTicksPerILVL = 0.67; // ~1s tous les 30 ILVL

    public MeteorShowerPower() {
        super("meteor_shower", "Pluie de Météores",
            "Fait tomber des météores enflammés sur vos ennemis");

        this.baseProcChance = 0.05;  // 5% de chance de proc
        this.cooldownMs = 25000;     // 25 secondes de cooldown
        this.minimumRarity = Rarity.LEGENDARY;
    }

    @Override
    public void trigger(Player player, LivingEntity target, int itemLevel) {
        if (!canProc(player, itemLevel)) {
            return;
        }

        applyCooldown(player);

        // Calculer les valeurs scalées
        double damagePerMeteor = calculateDamage(itemLevel);
        int meteorCount = calculateMeteorCount(itemLevel);
        double radius = calculateRadius(itemLevel);
        int fireTicks = calculateFireDuration(itemLevel);

        // Centre de la zone (cible ou position du joueur)
        Location center = target != null ? target.getLocation() : player.getLocation().add(player.getLocation().getDirection().multiply(5));

        // Notification
        player.sendMessage("§c☄ §6Pluie de Météores! §7(" + meteorCount + " météores)");

        // Effet d'annonce
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);

        // Faire tomber les météores avec un délai entre chacun
        new BukkitRunnable() {
            int meteorsSpawned = 0;

            @Override
            public void run() {
                if (meteorsSpawned >= meteorCount) {
                    cancel();
                    return;
                }

                // Position aléatoire dans le rayon
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;
                double x = Math.cos(angle) * distance;
                double z = Math.sin(angle) * distance;

                Location meteorTarget = center.clone().add(x, 0, z);
                spawnMeteor(meteorTarget, damagePerMeteor, fireTicks, player);

                meteorsSpawned++;
            }
        }.runTaskTimer(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            0L,
            10L // 0.5 seconde entre chaque météore
        );
    }

    /**
     * Fait apparaître un météore
     */
    private void spawnMeteor(Location targetLoc, double damage, int fireTicks, Player player) {
        // Spawn du météore en hauteur
        Location spawnLoc = targetLoc.clone().add(0, 20, 0);

        // Créer un FallingBlock pour l'effet visuel
        FallingBlock meteor = spawnLoc.getWorld().spawnFallingBlock(
            spawnLoc,
            Material.MAGMA_BLOCK.createBlockData()
        );

        meteor.setDropItem(false);
        meteor.setHurtEntities(false);
        meteor.setVelocity(new Vector(0, -2, 0));

        // Particules de feu pendant la chute
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!meteor.isValid() || meteor.isOnGround()) {
                    cancel();
                    return;
                }

                Location loc = meteor.getLocation();
                loc.getWorld().spawnParticle(
                    Particle.FLAME,
                    loc,
                    20,
                    0.5, 0.5, 0.5,
                    0.05
                );

                loc.getWorld().spawnParticle(
                    Particle.LAVA,
                    loc,
                    5,
                    0.3, 0.3, 0.3
                );

                // Son de sifflement
                if (random.nextInt(5) == 0) {
                    loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.7f);
                }
            }
        }.runTaskTimer(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            0L,
            2L
        );

        // Détection de l'impact
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!meteor.isValid()) {
                    cancel();
                    return;
                }

                if (meteor.isOnGround() || meteor.getLocation().getY() <= targetLoc.getY() + 1) {
                    Location impactLoc = meteor.getLocation();
                    meteor.remove();

                    // Explosion visuelle
                    createMeteorImpact(impactLoc, damage, fireTicks, player);
                    cancel();
                }
            }
        }.runTaskTimer(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            0L,
            1L
        );
    }

    /**
     * Crée l'effet d'impact du météore
     */
    private void createMeteorImpact(Location loc, double damage, int fireTicks, Player player) {
        // Effets visuels
        loc.getWorld().spawnParticle(
            Particle.EXPLOSION,
            loc,
            5,
            1, 0.5, 1,
            0
        );

        loc.getWorld().spawnParticle(
            Particle.FLAME,
            loc,
            100,
            2, 1, 2,
            0.2
        );

        loc.getWorld().spawnParticle(
            Particle.LAVA,
            loc,
            30,
            2, 1, 2
        );

        // Son d'explosion
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);

        // Dégâts AOE
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (entity instanceof LivingEntity livingTarget && entity != player) {
                // Ne pas affecter les joueurs (sauf si PvP)
                if (livingTarget instanceof Player && !livingTarget.getWorld().getPVP()) {
                    continue;
                }

                double distance = entity.getLocation().distance(loc);
                double distanceFactor = 1.0 - (distance / 3.0) * 0.5;
                double finalDamage = damage * distanceFactor;

                livingTarget.damage(finalDamage, player);
                livingTarget.setFireTicks(fireTicks);

                // Appliquer Wither pour simuler brûlure intense
                livingTarget.addPotionEffect(
                    new PotionEffect(
                        PotionEffectType.WITHER,
                        fireTicks / 2,
                        1,
                        false,
                        true
                    )
                );

                // Effet visuel sur la cible
                livingTarget.getWorld().spawnParticle(
                    Particle.FLAME,
                    livingTarget.getEyeLocation(),
                    10,
                    0.3, 0.5, 0.3
                );
            }
        }

        // Mettre le feu au sol temporairement
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.sqrt(x * x + z * z) <= 2) {
                    Location fireLoc = loc.clone().add(x, 0, z);
                    if (fireLoc.getBlock().getType() == Material.AIR) {
                        fireLoc.getBlock().setType(Material.FIRE);

                        // Éteindre après quelques secondes
                        Location finalFireLoc = fireLoc;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (finalFireLoc.getBlock().getType() == Material.FIRE) {
                                    finalFireLoc.getBlock().setType(Material.AIR);
                                }
                            }
                        }.runTaskLater(
                            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
                            fireTicks
                        );
                    }
                }
            }
        }
    }

    /**
     * Calcule les dégâts par météore selon l'ILVL
     */
    private double calculateDamage(int itemLevel) {
        return baseDamagePerMeteor + (itemLevel * damagePerILVL);
    }

    /**
     * Calcule le nombre de météores selon l'ILVL
     */
    private int calculateMeteorCount(int itemLevel) {
        return (int) (baseMeteorCount + (itemLevel * meteorCountPerILVL));
    }

    /**
     * Calcule le rayon de la zone selon l'ILVL
     */
    private double calculateRadius(int itemLevel) {
        return baseZoneRadius + (itemLevel * radiusPerILVL);
    }

    /**
     * Calcule la durée du feu selon l'ILVL
     */
    private int calculateFireDuration(int itemLevel) {
        return (int) (baseFireTicks + (itemLevel * fireTicksPerILVL));
    }

    @Override
    protected List<String> getPowerStats(int itemLevel) {
        List<String> stats = new ArrayList<>();
        stats.add("§8Dégâts/météore: §c" + String.format("%.1f", calculateDamage(itemLevel)));
        stats.add("§8Nombre: §e" + calculateMeteorCount(itemLevel) + " météores");
        stats.add("§8Rayon: §e" + String.format("%.1f", calculateRadius(itemLevel)) + " blocs");
        stats.add("§8Feu: §6" + String.format("%.1f", calculateFireDuration(itemLevel) / 20.0) + "s");
        stats.add("§8Effets: §6Feu, §8Wither II");
        return stats;
    }
}
