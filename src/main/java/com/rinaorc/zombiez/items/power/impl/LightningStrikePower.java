package com.rinaorc.zombiez.items.power.impl;

import com.rinaorc.zombiez.items.power.Power;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Pouvoir: Frappe Foudroyante
 *
 * Vos coups ont une chance d'invoquer la foudre sur la cible,
 * infligeant de lourds dégâts et paralysant brièvement l'ennemi.
 *
 * Scaling ILVL:
 * - Dégâts: 8 * (ILVL / 10)
 * - Chance de coup critique: 15% + (ILVL / 200)
 * - Durée de paralysie: 1s + (ILVL / 100)s
 */
public class LightningStrikePower extends Power {

    // Paramètres de base
    private double baseDamage = 8.0;
    private double critChanceBase = 0.15;
    private int baseStunTicks = 20; // 1 seconde

    // Paramètres de scaling
    private double damagePerILVL = 0.8;
    private double critChancePerILVL = 0.005; // +0.5% par ILVL
    private double stunPerILVL = 0.2; // 0.2 ticks par ILVL

    public LightningStrikePower() {
        super("lightning_strike", "Frappe Foudroyante",
            "Invoque la foudre sur vos ennemis");

        this.baseProcChance = 0.10;  // 10% de chance de proc
        this.cooldownMs = 8000;      // 8 secondes de cooldown
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
        double damage = calculateDamage(itemLevel);
        double critChance = calculateCritChance(itemLevel);
        int stunTicks = calculateStunDuration(itemLevel);

        // Roll du critique
        boolean isCrit = Math.random() < critChance;
        double finalDamage = isCrit ? damage * 2.0 : damage;

        // Effet visuel avant la frappe
        Location targetLoc = target.getLocation();
        targetLoc.getWorld().spawnParticle(
            Particle.ELECTRIC_SPARK,
            targetLoc.clone().add(0, 3, 0),
            30,
            0.5, 0.5, 0.5,
            0.1
        );

        // Invoquer la foudre (cosmétique, pas de dégâts vanilla)
        new BukkitRunnable() {
            @Override
            public void run() {
                targetLoc.getWorld().strikeLightningEffect(targetLoc);

                // Infliger les dégâts
                target.damage(finalDamage, player);

                // Effet de paralysie (slowness + jump reduction)
                target.addPotionEffect(
                    new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SLOWNESS,
                        stunTicks,
                        3,
                        false,
                        true
                    )
                );
                target.addPotionEffect(
                    new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.JUMP_BOOST,
                        stunTicks,
                        -10,
                        false,
                        true
                    )
                );

                // Effets visuels d'impact
                targetLoc.getWorld().spawnParticle(
                    Particle.FLASH,
                    targetLoc.clone().add(0, 1, 0),
                    3
                );

                targetLoc.getWorld().spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    targetLoc.clone().add(0, 1, 0),
                    50,
                    0.5, 1, 0.5,
                    0.2
                );

                // Son de foudre
                targetLoc.getWorld().playSound(
                    targetLoc,
                    Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                    1.0f,
                    1.0f
                );

                // Notification au joueur
                String critText = isCrit ? " §e§lCRITIQUE!" : "";
                player.sendMessage("§e⚡ §6Frappe Foudroyante! §7(§c" +
                    String.format("%.1f", finalDamage) + " dégâts§7)" + critText);
            }
        }.runTaskLater(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            5L
        );
    }

    /**
     * Calcule les dégâts selon l'ILVL
     */
    private double calculateDamage(int itemLevel) {
        return baseDamage + (itemLevel * damagePerILVL);
    }

    /**
     * Calcule la chance de critique selon l'ILVL
     */
    private double calculateCritChance(int itemLevel) {
        return Math.min(0.5, critChanceBase + (itemLevel * critChancePerILVL));
    }

    /**
     * Calcule la durée de paralysie selon l'ILVL
     */
    private int calculateStunDuration(int itemLevel) {
        return (int) (baseStunTicks + (itemLevel * stunPerILVL));
    }

    @Override
    public List<String> getPowerStats(int itemLevel, int zoneId) {
        List<String> stats = new ArrayList<>();
        // Scaling par zone
        double scaledDamage = getScaledDamage(calculateDamage(itemLevel), zoneId);
        int scaledStunDuration = getScaledDuration(calculateStunDuration(itemLevel), zoneId);

        stats.add("§8Dégâts: §c" + String.format("%.1f", scaledDamage));
        stats.add("§8Chance critique: §e" + String.format("%.1f%%", calculateCritChance(itemLevel) * 100));
        stats.add("§8Paralysie: §e" + String.format("%.1f", scaledStunDuration / 20.0) + "s");
        stats.add("§8Effet: §bSlowness IV");
        return stats;
    }
}
