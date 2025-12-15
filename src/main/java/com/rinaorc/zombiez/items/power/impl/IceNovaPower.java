package com.rinaorc.zombiez.items.power.impl;

import com.rinaorc.zombiez.items.power.Power;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pouvoir: Nova Glaciale
 *
 * Déclenche une explosion de glace autour de vous,
 * gelant les ennemis et créant temporairement un terrain gelé.
 *
 * Scaling ILVL:
 * - Dégâts: 4 * (ILVL / 10)
 * - Rayon: 4 + (ILVL / 30)
 * - Durée gel: 2s + (ILVL / 50)s
 * - Durée glace au sol: 3s + (ILVL / 25)s
 */
public class IceNovaPower extends Power {

    // Paramètres de base
    private double baseDamage = 4.0;
    private double baseRadius = 4.0;
    private int baseFreezeTicks = 40; // 2 secondes
    private int baseIceDuration = 60; // 3 secondes

    // Paramètres de scaling
    private double damagePerILVL = 0.4;
    private double radiusPerILVL = 0.033; // 1 bloc tous les 30 ILVL
    private double freezePerILVL = 0.4;
    private double iceDurationPerILVL = 0.8;

    public IceNovaPower() {
        super("ice_nova", "Nova Glaciale",
            "Déclenche une explosion de glace gelant tout autour de vous");

        this.baseProcChance = 0.08;  // 8% de chance de proc
        this.cooldownMs = 18000;     // 18 secondes de cooldown
        this.minimumRarity = Rarity.EPIC;
    }

    @Override
    public void trigger(Player player, LivingEntity target, int itemLevel) {
        if (!canProc(player, itemLevel)) {
            return;
        }

        applyCooldown(player);

        // Calculer les valeurs scalées
        double damage = calculateDamage(itemLevel);
        double radius = calculateRadius(itemLevel);
        int freezeTicks = calculateFreezeDuration(itemLevel);
        int iceDuration = calculateIceDuration(itemLevel);

        Location center = player.getLocation();

        // Notification
        player.sendMessage("§b❄ §3Nova Glaciale! §7(Rayon: §b" +
            String.format("%.1f", radius) + " blocs§7)");

        // Effet visuel central
        center.getWorld().spawnParticle(
            Particle.EXPLOSION,
            center.clone().add(0, 1, 0),
            3,
            0, 0, 0,
            0
        );

        center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);

        // Créer l'effet de nova qui se propage
        new BukkitRunnable() {
            double currentRadius = 0;
            final double maxRadius = radius;
            final double step = 0.5;

            @Override
            public void run() {
                if (currentRadius >= maxRadius) {
                    cancel();
                    return;
                }

                currentRadius += step;

                // Particules en cercle
                for (int i = 0; i < 360; i += 10) {
                    double angle = Math.toRadians(i);
                    double x = Math.cos(angle) * currentRadius;
                    double z = Math.sin(angle) * currentRadius;

                    Location particleLoc = center.clone().add(x, 0.2, z);
                    center.getWorld().spawnParticle(
                        Particle.SNOWFLAKE,
                        particleLoc,
                        3,
                        0.1, 0.1, 0.1,
                        0
                    );

                    center.getWorld().spawnParticle(
                        Particle.CLOUD,
                        particleLoc,
                        1,
                        0, 0, 0,
                        0.01
                    );
                }

                // Son de glace
                if (currentRadius % 2 < step) {
                    center.getWorld().playSound(center, Sound.BLOCK_SNOW_BREAK, 0.5f, 0.8f);
                }
            }
        }.runTaskTimer(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            0L,
            1L
        );

        // Appliquer les effets aux ennemis
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity livingTarget && entity != player) {
                // Ne pas affecter les joueurs (sauf si PvP)
                if (livingTarget instanceof Player && !livingTarget.getWorld().getPVP()) {
                    continue;
                }

                double distance = entity.getLocation().distance(center);
                if (distance <= radius) {
                    // Dégâts réduits selon la distance
                    double distanceFactor = 1.0 - (distance / radius) * 0.5;
                    double finalDamage = damage * distanceFactor;

                    livingTarget.damage(finalDamage, player);

                    // Effet de gel
                    livingTarget.addPotionEffect(
                        new PotionEffect(PotionEffectType.SLOWNESS, freezeTicks, 4, false, true)
                    );
                    livingTarget.addPotionEffect(
                        new PotionEffect(PotionEffectType.MINING_FATIGUE, freezeTicks, 2, false, true)
                    );

                    // Effet visuel sur la cible
                    livingTarget.getWorld().spawnParticle(
                        Particle.SNOWFLAKE,
                        livingTarget.getEyeLocation(),
                        20,
                        0.5, 0.5, 0.5,
                        0
                    );
                }
            }
        }

        // Créer temporairement de la glace au sol
        createIceGround(center, radius, iceDuration);
    }

    /**
     * Crée temporairement de la glace au sol (côté client uniquement)
     * Les blocs sont envoyés aux joueurs proches sans modifier le monde réel
     */
    private void createIceGround(Location center, double radius, int duration) {
        int radiusInt = (int) Math.ceil(radius);
        List<Location> iceLocations = new ArrayList<>();

        // Collecter les locations où afficher la glace
        for (int x = -radiusInt; x <= radiusInt; x++) {
            for (int z = -radiusInt; z <= radiusInt; z++) {
                if (Math.sqrt(x * x + z * z) <= radius) {
                    Block block = center.getBlock().getRelative(x, -1, z);

                    // Vérifier que c'est un bloc solide et pas déjà de la glace
                    if (block.getType().isSolid() &&
                        block.getType() != Material.ICE &&
                        block.getType() != Material.PACKED_ICE &&
                        block.getType() != Material.BLUE_ICE) {

                        iceLocations.add(block.getLocation());
                    }
                }
            }
        }

        // Envoyer les blocs de glace côté client à tous les joueurs proches
        org.bukkit.block.data.BlockData iceData = Material.ICE.createBlockData();
        for (Player nearbyPlayer : center.getWorld().getNearbyPlayers(center, radius + 32)) {
            for (Location loc : iceLocations) {
                nearbyPlayer.sendBlockChange(loc, iceData);
            }
        }

        // Restaurer les blocs après la durée (côté client)
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player nearbyPlayer : center.getWorld().getNearbyPlayers(center, radius + 32)) {
                    for (Location loc : iceLocations) {
                        Block realBlock = loc.getBlock();
                        nearbyPlayer.sendBlockChange(loc, realBlock.getBlockData());
                    }
                }
            }
        }.runTaskLater(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            duration
        );
    }

    /**
     * Calcule les dégâts selon l'ILVL
     */
    private double calculateDamage(int itemLevel) {
        return baseDamage + (itemLevel * damagePerILVL);
    }

    /**
     * Calcule le rayon selon l'ILVL
     */
    private double calculateRadius(int itemLevel) {
        return baseRadius + (itemLevel * radiusPerILVL);
    }

    /**
     * Calcule la durée de gel selon l'ILVL
     */
    private int calculateFreezeDuration(int itemLevel) {
        return (int) (baseFreezeTicks + (itemLevel * freezePerILVL));
    }

    /**
     * Calcule la durée de la glace au sol selon l'ILVL
     */
    private int calculateIceDuration(int itemLevel) {
        return (int) (baseIceDuration + (itemLevel * iceDurationPerILVL));
    }

    @Override
    protected List<String> getPowerStats(int itemLevel) {
        List<String> stats = new ArrayList<>();
        stats.add("§8Dégâts: §c" + String.format("%.1f", calculateDamage(itemLevel)));
        stats.add("§8Rayon: §b" + String.format("%.1f", calculateRadius(itemLevel)) + " blocs");
        stats.add("§8Gel: §e" + String.format("%.1f", calculateFreezeDuration(itemLevel) / 20.0) + "s");
        stats.add("§8Effets: §bSlowness V, Mining Fatigue III");
        return stats;
    }
}
