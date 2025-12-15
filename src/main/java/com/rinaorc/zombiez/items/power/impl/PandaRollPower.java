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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Pouvoir: Roulade Panda
 *
 * Vos coups ont une chance d'invoquer un panda qui roule sur les ennemis
 * et inflige des dégâts de zone.
 *
 * Scaling ILVL:
 * - Dégâts: 5 * (ILVL / 10)
 * - Rayon AOE: 3 + (ILVL / 50)
 * - Durée du panda: 5s + (ILVL / 20)s
 */
public class PandaRollPower extends Power {

    // Paramètres de base (configurables)
    private double baseDamage = 5.0;
    private double baseRadius = 3.0;
    private int baseDurationTicks = 100; // 5 secondes

    // Paramètres de scaling
    private double damagePerILVL = 0.5;      // Dégâts par tranche de 10 ILVL
    private double radiusPerILVL = 0.02;     // Rayon par ILVL
    private double durationPerILVL = 0.5;    // Durée en ticks par ILVL

    public PandaRollPower() {
        super("panda_roll", "Roulade Panda",
            "Invoque un panda qui roule sur les ennemis");

        this.baseProcChance = 0.15;  // 15% de chance de proc
        this.cooldownMs = 12000;     // 12 secondes de cooldown
        this.minimumRarity = Rarity.RARE;
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
        int durationTicks = calculateDuration(itemLevel);

        // Position d'invocation (devant le joueur)
        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        spawnLoc.setY(player.getLocation().getY());

        // Invoquer le panda
        Panda panda = (Panda) player.getWorld().spawnEntity(spawnLoc, EntityType.PANDA);
        panda.setCustomName("§6✦ Panda Roulant §6✦");
        panda.setCustomNameVisible(true);
        panda.setAI(false); // Désactiver l'IA normale
        panda.setInvulnerable(true);

        // Effet visuel de spawn
        spawnLoc.getWorld().spawnParticle(Particle.CLOUD, spawnLoc, 30, 0.5, 0.5, 0.5, 0.1);
        spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_PANDA_AMBIENT, 1.5f, 0.8f);

        // Notification au joueur
        player.sendMessage("§6✦ §eRoulade Panda activée! §7(Dégâts: §c" +
            String.format("%.1f", damage) + "§7)");

        // Faire rouler le panda et infliger des dégâts
        new BukkitRunnable() {
            int ticksElapsed = 0;
            final int maxTicks = durationTicks;
            final Vector direction = player.getLocation().getDirection().setY(0).normalize().multiply(0.3);

            @Override
            public void run() {
                if (!panda.isValid() || panda.isDead() || ticksElapsed >= maxTicks) {
                    // Effet de disparition
                    if (panda.isValid()) {
                        Location loc = panda.getLocation();
                        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 5, 0.5, 0.5, 0.5);
                        loc.getWorld().playSound(loc, Sound.ENTITY_PANDA_DEATH, 0.5f, 1.2f);
                        panda.remove();
                    }
                    cancel();
                    return;
                }

                ticksElapsed++;

                // Déplacer le panda
                Location currentLoc = panda.getLocation();
                currentLoc.add(direction);
                panda.teleport(currentLoc);

                // Rotation pour effet de roulade
                float yaw = panda.getLocation().getYaw() + 45;
                panda.setRotation(yaw, 0);

                // Particules de roulade
                if (ticksElapsed % 3 == 0) {
                    currentLoc.getWorld().spawnParticle(
                        Particle.CLOUD,
                        currentLoc,
                        5,
                        0.3, 0.3, 0.3,
                        0.05
                    );
                }

                // Dégâts AOE tous les 10 ticks (0.5s)
                if (ticksElapsed % 10 == 0) {
                    dealAOEDamage(panda.getLocation(), damage, radius, player);

                    // Effet visuel de l'impact
                    currentLoc.getWorld().spawnParticle(
                        Particle.CRIT,
                        currentLoc,
                        15,
                        radius, 0.5, radius,
                        0
                    );
                    currentLoc.getWorld().playSound(
                        currentLoc,
                        Sound.ENTITY_PLAYER_ATTACK_STRONG,
                        0.5f,
                        1.0f
                    );
                }
            }
        }.runTaskTimer(org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"), 0L, 1L);
    }

    /**
     * Inflige des dégâts AOE autour du panda
     */
    private void dealAOEDamage(Location center, double damage, double radius, Player source) {
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != source) {
                // Ne pas attaquer les autres joueurs (sauf si PvP activé)
                if (target instanceof Player && !target.getWorld().getPVP()) {
                    continue;
                }

                // Infliger les dégâts
                target.damage(damage, source);

                // Effet de knockback
                Vector knockback = target.getLocation().toVector()
                    .subtract(center.toVector())
                    .normalize()
                    .multiply(0.5)
                    .setY(0.3);
                target.setVelocity(knockback);

                // Effet visuel sur la cible
                target.getWorld().spawnParticle(
                    Particle.DAMAGE_INDICATOR,
                    target.getEyeLocation(),
                    3,
                    0.3, 0.3, 0.3
                );
            }
        }
    }

    /**
     * Calcule les dégâts selon l'ILVL
     */
    private double calculateDamage(int itemLevel) {
        return baseDamage + (itemLevel * damagePerILVL);
    }

    /**
     * Calcule le rayon AOE selon l'ILVL
     */
    private double calculateRadius(int itemLevel) {
        return baseRadius + (itemLevel * radiusPerILVL);
    }

    /**
     * Calcule la durée selon l'ILVL
     */
    private int calculateDuration(int itemLevel) {
        return (int) (baseDurationTicks + (itemLevel * durationPerILVL));
    }

    @Override
    public List<String> getPowerStats(int itemLevel, int zoneId) {
        List<String> stats = new ArrayList<>();
        // Scaling par zone
        double scaledDamage = getScaledDamage(calculateDamage(itemLevel), zoneId);
        double scaledRadius = getScaledRadius(calculateRadius(itemLevel), zoneId);
        int scaledDuration = getScaledDuration(calculateDuration(itemLevel), zoneId);

        stats.add("§8Dégâts: §c" + String.format("%.1f", scaledDamage));
        stats.add("§8Rayon: §e" + String.format("%.1f", scaledRadius) + " blocs");
        stats.add("§8Durée: §e" + String.format("%.1f", scaledDuration / 20.0) + "s");
        return stats;
    }
}
