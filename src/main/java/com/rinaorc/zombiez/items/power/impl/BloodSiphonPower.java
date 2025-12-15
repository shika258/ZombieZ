package com.rinaorc.zombiez.items.power.impl;

import com.rinaorc.zombiez.items.power.Power;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Pouvoir: Siphon de Sang
 *
 * Vole la vie de votre cible, vous soignant d'une partie des dégâts infligés.
 * Plus la cible est blessée, plus le siphon est efficace.
 *
 * Scaling ILVL:
 * - Dégâts: 3 * (ILVL / 10)
 * - Vol de vie: 50% + (ILVL / 200)%
 * - Bonus si cible < 50% HP: +25%
 * - Durée régénération: 3s + (ILVL / 40)s
 */
public class BloodSiphonPower extends Power {

    // Paramètres de base
    private double baseDamage = 3.0;
    private double baseLifesteal = 0.5; // 50% des dégâts en heal
    private double lowHealthBonus = 0.25; // +25% si cible < 50% HP
    private int baseRegenTicks = 60; // 3 secondes de régénération

    // Paramètres de scaling
    private double damagePerILVL = 0.3;
    private double lifestealPerILVL = 0.005; // +0.5% par ILVL
    private double regenPerILVL = 0.5;

    public BloodSiphonPower() {
        super("blood_siphon", "Siphon de Sang",
            "Vole la vie de vos ennemis pour vous soigner");

        this.baseProcChance = 0.20;  // 20% de chance de proc
        this.cooldownMs = 6000;      // 6 secondes de cooldown
        this.minimumRarity = Rarity.RARE;
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
        double lifestealPercent = calculateLifesteal(itemLevel);
        int regenTicks = calculateRegenDuration(itemLevel);

        // Vérifier si la cible est blessée (< 50% HP)
        double targetHealthPercent = target.getHealth() / target.getAttribute(Attribute.MAX_HEALTH).getValue();
        boolean isLowHealth = targetHealthPercent < 0.5;

        // Appliquer le bonus si cible blessée
        if (isLowHealth) {
            lifestealPercent += lowHealthBonus;
        }

        // Infliger les dégâts
        target.damage(damage, player);

        // Calculer le soin
        double healAmount = damage * lifestealPercent;

        // Appliquer le soin au joueur
        double currentHealth = player.getHealth();
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double newHealth = Math.min(maxHealth, currentHealth + healAmount);
        player.setHealth(newHealth);

        // Appliquer régénération
        player.addPotionEffect(
            new PotionEffect(
                PotionEffectType.REGENERATION,
                regenTicks,
                1,
                false,
                true
            )
        );

        // Effet visuel: particules de sang allant de la cible au joueur
        createBloodSiphonEffect(target.getLocation(), player.getLocation());

        // Son d'absorption
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 0.8f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 0.5f);

        // Notification
        String bonusText = isLowHealth ? " §c§l[BONUS]" : "";
        player.sendMessage("§c💉 §4Siphon de Sang! §7(§a+" +
            String.format("%.1f", healAmount) + " ❤§7)" + bonusText);
    }

    /**
     * Crée l'effet visuel du siphon de sang
     */
    private void createBloodSiphonEffect(Location from, Location to) {
        // Particules rouges à la cible
        from.getWorld().spawnParticle(
            Particle.BLOCK,
            from.clone().add(0, 1, 0),
            30,
            0.5, 0.5, 0.5,
            org.bukkit.Material.REDSTONE_BLOCK.createBlockData()
        );

        // Animation de particules allant de la cible au joueur
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 15;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                double progress = (double) ticks / maxTicks;

                // Interpolation entre from et to
                double x = from.getX() + (to.getX() - from.getX()) * progress;
                double y = from.getY() + (to.getY() - from.getY()) * progress + 1;
                double z = from.getZ() + (to.getZ() - from.getZ()) * progress;

                Location particleLoc = new Location(from.getWorld(), x, y, z);

                // Particules de sang
                particleLoc.getWorld().spawnParticle(
                    Particle.BLOCK,
                    particleLoc,
                    3,
                    0.1, 0.1, 0.1,
                    org.bukkit.Material.REDSTONE_BLOCK.createBlockData()
                );

                particleLoc.getWorld().spawnParticle(
                    Particle.HEART,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0
                );

                ticks++;
            }
        }.runTaskTimer(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            0L,
            1L
        );
    }

    /**
     * Calcule les dégâts selon l'ILVL
     */
    private double calculateDamage(int itemLevel) {
        return baseDamage + (itemLevel * damagePerILVL);
    }

    /**
     * Calcule le pourcentage de vol de vie selon l'ILVL
     */
    private double calculateLifesteal(int itemLevel) {
        return Math.min(1.0, baseLifesteal + (itemLevel * lifestealPerILVL));
    }

    /**
     * Calcule la durée de régénération selon l'ILVL
     */
    private int calculateRegenDuration(int itemLevel) {
        return (int) (baseRegenTicks + (itemLevel * regenPerILVL));
    }

    @Override
    public List<String> getPowerStats(int itemLevel, int zoneId) {
        List<String> stats = new ArrayList<>();
        // Scaling par zone
        double scaledDamage = getScaledDamage(calculateDamage(itemLevel), zoneId);
        int scaledRegenDuration = getScaledDuration(calculateRegenDuration(itemLevel), zoneId);

        stats.add("§8Dégâts: §c" + String.format("%.1f", scaledDamage));
        stats.add("§8Vol de vie: §a" + String.format("%.0f%%", calculateLifesteal(itemLevel) * 100));
        stats.add("§8Bonus cible blessée: §c+25%");
        stats.add("§8Régénération: §e" + String.format("%.1f", scaledRegenDuration / 20.0) + "s");
        stats.add("§8Effet: §aRegénération II");
        return stats;
    }
}
