package com.rinaorc.zombiez.items.power.impl;

import com.rinaorc.zombiez.items.power.Power;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Pouvoir: Renaissance du Phénix
 *
 * POUVOIR PASSIF - Déclenché à la mort
 * Renaît de vos cendres au lieu de mourir, avec un cooldown très long.
 * Inflige des dégâts de feu aux ennemis proches lors de la résurrection.
 *
 * Scaling ILVL:
 * - Santé à la résurrection: 20% + (ILVL / 500)% de la santé max
 * - Dégâts d'explosion: 15 * (ILVL / 10)
 * - Rayon d'explosion: 5 + (ILVL / 40)
 * - Durée invulnérabilité: 2s + (ILVL / 100)s
 *
 * Note: Ce pouvoir nécessite un listener spécial pour la mort
 */
public class PhoenixRebirthPower extends Power {

    // Stockage des joueurs ayant le pouvoir actif (pour le listener de mort)
    private static final HashMap<UUID, PhoenixRebirthPower> ACTIVE_PHOENIX_POWERS = new HashMap<>();

    // Paramètres de base
    private double baseHealthPercent = 0.20; // 20% de la santé max
    private double baseDamage = 15.0;
    private double baseRadius = 5.0;
    private int baseInvulnerabilityTicks = 40; // 2 secondes

    // Paramètres de scaling
    private double healthPercentPerILVL = 0.002; // +0.2% par ILVL
    private double damagePerILVL = 1.5;
    private double radiusPerILVL = 0.025;
    private double invulnerabilityPerILVL = 0.2;

    public PhoenixRebirthPower() {
        super("phoenix_rebirth", "Renaissance du Phénix",
            "Renaissez de vos cendres au lieu de mourir");

        this.baseProcChance = 1.0;   // 100% - Toujours actif (mais cooldown très long)
        this.cooldownMs = 300000;    // 5 minutes de cooldown
        this.minimumRarity = Rarity.MYTHIC;
    }

    @Override
    public void trigger(Player player, LivingEntity target, int itemLevel) {
        // Ce pouvoir se déclenche différemment (à la mort)
        // Cette méthode ne sera pas utilisée normalement
        // Elle est appelée manuellement par le listener de mort
    }

    /**
     * Méthode spéciale pour tenter de ressusciter le joueur
     * Appelée depuis un listener externe
     */
    public boolean attemptRebirth(Player player, int itemLevel) {
        if (isOnCooldown(player)) {
            return false; // Cooldown actif, pas de résurrection
        }

        applyCooldown(player);

        // Calculer les valeurs scalées
        double healthPercent = calculateHealthPercent(itemLevel);
        double damage = calculateDamage(itemLevel);
        double radius = calculateRadius(itemLevel);
        int invulnerabilityTicks = calculateInvulnerability(itemLevel);

        // Position de résurrection
        Location loc = player.getLocation();

        // Notification
        player.sendMessage("§6🔥 §e§lRENAISSANCE DU PHÉNIX!");
        loc.getWorld().playSound(loc, Sound.ENTITY_PHOENIX_DEATH, 2.0f, 1.0f);

        // Animation de résurrection
        new BukkitRunnable() {
            int ticks = 0;
            final int animationDuration = 20; // 1 seconde

            @Override
            public void run() {
                if (ticks >= animationDuration) {
                    // Fin de l'animation - Résurrection complète
                    completeRebirth(player, healthPercent, damage, radius, invulnerabilityTicks, itemLevel);
                    cancel();
                    return;
                }

                // Particules de feu montantes
                for (int i = 0; i < 360; i += 20) {
                    double angle = Math.toRadians(i + ticks * 18);
                    double rad = 2.0 - (ticks * 0.1);
                    double x = Math.cos(angle) * rad;
                    double z = Math.sin(angle) * rad;
                    double y = ticks * 0.1;

                    Location particleLoc = loc.clone().add(x, y, z);
                    loc.getWorld().spawnParticle(
                        Particle.FLAME,
                        particleLoc,
                        3,
                        0.1, 0.1, 0.1,
                        0.01
                    );

                    loc.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        particleLoc,
                        1,
                        0, 0, 0,
                        0
                    );
                }

                // Son de battement
                if (ticks % 5 == 0) {
                    loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.5f);
                }

                ticks++;
            }
        }.runTaskTimer(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            0L,
            1L
        );

        return true; // Résurrection en cours
    }

    /**
     * Complète la résurrection du joueur
     */
    private void completeRebirth(Player player, double healthPercent, double damage,
                                 double radius, int invulnerabilityTicks, int itemLevel) {

        Location loc = player.getLocation();

        // Restaurer la santé
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double newHealth = maxHealth * healthPercent;
        player.setHealth(newHealth);

        // Effets bénéfiques
        player.addPotionEffect(
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, invulnerabilityTicks * 3, 0, false, true)
        );
        player.addPotionEffect(
            new PotionEffect(PotionEffectType.REGENERATION, invulnerabilityTicks, 2, false, true)
        );
        player.addPotionEffect(
            new PotionEffect(PotionEffectType.RESISTANCE, invulnerabilityTicks, 2, false, true)
        );

        // Explosion de feu
        loc.getWorld().spawnParticle(
            Particle.EXPLOSION,
            loc.clone().add(0, 1, 0),
            10,
            2, 1, 2,
            0
        );

        loc.getWorld().spawnParticle(
            Particle.FLAME,
            loc.clone().add(0, 1, 0),
            200,
            radius, 2, radius,
            0.3
        );

        loc.getWorld().spawnParticle(
            Particle.SOUL_FIRE_FLAME,
            loc.clone().add(0, 1, 0),
            100,
            radius, 2, radius,
            0.2
        );

        // Son d'explosion
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 2.0f, 0.5f);

        // Dégâts aux ennemis proches
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof LivingEntity livingTarget && entity != player) {
                // Ne pas affecter les joueurs (sauf si PvP)
                if (livingTarget instanceof Player && !livingTarget.getWorld().getPVP()) {
                    continue;
                }

                double distance = entity.getLocation().distance(loc);
                double distanceFactor = 1.0 - (distance / radius) * 0.5;
                double finalDamage = damage * distanceFactor;

                livingTarget.damage(finalDamage, player);
                livingTarget.setFireTicks(100); // 5 secondes de feu

                // Knockback
                org.bukkit.util.Vector knockback = livingTarget.getLocation().toVector()
                    .subtract(loc.toVector())
                    .normalize()
                    .multiply(1.5)
                    .setY(0.5);
                livingTarget.setVelocity(knockback);

                // Effet visuel
                livingTarget.getWorld().spawnParticle(
                    Particle.FLAME,
                    livingTarget.getEyeLocation(),
                    20,
                    0.5, 0.5, 0.5
                );
            }
        }

        // Message au joueur
        player.sendMessage("§6Vous renaissez avec §c" + String.format("%.0f", newHealth) + " ❤");
        player.sendMessage("§8Cooldown: §c5 minutes");

        // Ailes de feu temporaires (particules)
        createFireWings(player, invulnerabilityTicks);
    }

    /**
     * Crée un effet d'ailes de feu temporaires
     */
    private void createFireWings(Player player, int duration) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= duration) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                double yaw = Math.toRadians(loc.getYaw() + 90);

                // Aile gauche
                for (int i = 0; i < 5; i++) {
                    double wingSpread = i * 0.3;
                    double wingHeight = i * 0.2;
                    double x = Math.cos(yaw) * wingSpread;
                    double z = Math.sin(yaw) * wingSpread;

                    Location wingLoc = loc.clone().add(x, 1.5 - wingHeight, z);
                    wingLoc.getWorld().spawnParticle(
                        Particle.FLAME,
                        wingLoc,
                        1,
                        0, 0, 0,
                        0
                    );
                }

                // Aile droite
                for (int i = 0; i < 5; i++) {
                    double wingSpread = i * 0.3;
                    double wingHeight = i * 0.2;
                    double x = Math.cos(yaw + Math.PI) * wingSpread;
                    double z = Math.sin(yaw + Math.PI) * wingSpread;

                    Location wingLoc = loc.clone().add(x, 1.5 - wingHeight, z);
                    wingLoc.getWorld().spawnParticle(
                        Particle.FLAME,
                        wingLoc,
                        1,
                        0, 0, 0,
                        0
                    );
                }

                ticks++;
            }
        }.runTaskTimer(
            org.bukkit.Bukkit.getPluginManager().getPlugin("ZombieZ"),
            0L,
            2L
        );
    }

    /**
     * Calcule le pourcentage de santé à la résurrection selon l'ILVL
     */
    private double calculateHealthPercent(int itemLevel) {
        return Math.min(0.5, baseHealthPercent + (itemLevel * healthPercentPerILVL));
    }

    /**
     * Calcule les dégâts d'explosion selon l'ILVL
     */
    private double calculateDamage(int itemLevel) {
        return baseDamage + (itemLevel * damagePerILVL);
    }

    /**
     * Calcule le rayon d'explosion selon l'ILVL
     */
    private double calculateRadius(int itemLevel) {
        return baseRadius + (itemLevel * radiusPerILVL);
    }

    /**
     * Calcule la durée d'invulnérabilité selon l'ILVL
     */
    private int calculateInvulnerability(int itemLevel) {
        return (int) (baseInvulnerabilityTicks + (itemLevel * invulnerabilityPerILVL));
    }

    @Override
    protected List<String> getPowerStats(int itemLevel) {
        List<String> stats = new ArrayList<>();
        stats.add("§7§lPOUVOIR PASSIF");
        stats.add("§8Santé: §a" + String.format("%.0f%%", calculateHealthPercent(itemLevel) * 100) + " HP max");
        stats.add("§8Dégâts explosion: §c" + String.format("%.1f", calculateDamage(itemLevel)));
        stats.add("§8Rayon: §e" + String.format("%.1f", calculateRadius(itemLevel)) + " blocs");
        stats.add("§8Invulnérabilité: §e" + String.format("%.1f", calculateInvulnerability(itemLevel) / 20.0) + "s");
        stats.add("§8Effets: §6Fire Resistance, §aRegénération III, §7Resistance III");
        stats.add("§c§lCooldown: 5 minutes");
        return stats;
    }

    /**
     * Enregistre un joueur comme ayant le pouvoir Phoenix actif
     */
    public static void registerPlayer(UUID playerUUID, PhoenixRebirthPower power) {
        ACTIVE_PHOENIX_POWERS.put(playerUUID, power);
    }

    /**
     * Retire un joueur du registre Phoenix
     */
    public static void unregisterPlayer(UUID playerUUID) {
        ACTIVE_PHOENIX_POWERS.remove(playerUUID);
    }

    /**
     * Obtient le pouvoir Phoenix d'un joueur s'il en a un
     */
    public static PhoenixRebirthPower getPlayerPower(UUID playerUUID) {
        return ACTIVE_PHOENIX_POWERS.get(playerUUID);
    }
}
