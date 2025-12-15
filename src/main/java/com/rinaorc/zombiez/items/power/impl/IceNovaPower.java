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
import java.util.List;

/**
 * Pouvoir: Nova Glaciale
 *
 * Déclenche une explosion de glace autour de vous,
 * gelant les ennemis et créant temporairement un terrain gelé.
 *
 * SYSTÈME DE SCALING PAR ZONE:
 * - Dégâts: Zone 1 = 4, Zone 50 = 4 × 7 = 28
 * - Rayon: Zone 1 = 4, Zone 50 = 4 × 3 = 12
 * - Durée gel: Zone 1 = 2s, Zone 50 = 6s
 * - Durée glace au sol: Zone 1 = 3s, Zone 50 = 9s
 *
 * La ZONE est le facteur PRINCIPAL de puissance!
 */
public class IceNovaPower extends Power {

    // Paramètres de BASE (avant scaling de zone)
    private static final double BASE_DAMAGE = 4.0;
    private static final double BASE_RADIUS = 4.0;
    private static final int BASE_FREEZE_TICKS = 40; // 2 secondes
    private static final int BASE_ICE_DURATION = 60; // 3 secondes

    public IceNovaPower() {
        super("ice_nova", "Nova Glaciale",
            "Déclenche une explosion de glace gelant tout autour de vous");

        this.baseProcChance = 0.08;  // 8% de chance de proc
        this.cooldownMs = 18000;     // 18 secondes de cooldown
        this.minimumRarity = Rarity.EPIC;
    }

    @Override
    public void trigger(Player player, LivingEntity target, int itemLevel) {
        // Déléguer à la nouvelle méthode avec zone 1 par défaut
        trigger(player, target, itemLevel, 1);
    }

    @Override
    public void trigger(Player player, LivingEntity target, int itemLevel, int zoneId) {
        if (!canProc(player, itemLevel, zoneId, Rarity.EPIC)) {
            return;
        }

        applyCooldown(player);

        // Calculer les valeurs scalées PAR ZONE (facteur PRINCIPAL)
        double damage = getScaledDamage(BASE_DAMAGE, zoneId);
        double radius = getScaledRadius(BASE_RADIUS, zoneId);
        int freezeTicks = getScaledDuration(BASE_FREEZE_TICKS, zoneId);
        int iceDuration = getScaledDuration(BASE_ICE_DURATION, zoneId);

        Location center = player.getLocation();

        // Notification avec indication de zone
        player.sendMessage("§b❄ §3Nova Glaciale! §7(Rayon: §b" +
            String.format("%.1f", radius) + " blocs §7| Dégâts: §c" +
            String.format("%.1f", damage) + "§7)");

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

                    // Effet de gel (durée scalée par zone)
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

    @Override
    public List<String> getPowerStats(int itemLevel, int zoneId) {
        List<String> stats = new ArrayList<>();
        // Afficher les valeurs scalées par zone
        double damage = getScaledDamage(BASE_DAMAGE, zoneId);
        double radius = getScaledRadius(BASE_RADIUS, zoneId);
        int freezeTicks = getScaledDuration(BASE_FREEZE_TICKS, zoneId);

        stats.add("§8Dégâts: §c" + String.format("%.1f", damage));
        stats.add("§8Rayon: §b" + String.format("%.1f", radius) + " blocs");
        stats.add("§8Gel: §e" + String.format("%.1f", freezeTicks / 20.0) + "s");
        stats.add("§8Effets: §bSlowness V, Mining Fatigue III");
        return stats;
    }
}
