package com.rinaorc.zombiez.utils;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Manager pour les indicateurs de dégâts flottants (hologrammes)
 */
public class DamageIndicatorManager {

    private final ZombieZPlugin plugin;
    private boolean enabled;

    public DamageIndicatorManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("effects.damage-indicators", true);
    }

    /**
     * Recharge la configuration
     */
    public void reload() {
        this.enabled = plugin.getConfig().getBoolean("effects.damage-indicators", true);
    }

    /**
     * Vérifie si les indicateurs de dégâts sont activés
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Affiche un indicateur de dégâts flottant
     * @param entity L'entité qui a reçu les dégâts
     * @param damage Les dégâts infligés
     * @param isCritical Si c'est un coup critique
     */
    public void showDamageIndicator(LivingEntity entity, double damage, boolean isCritical) {
        if (!enabled) return;

        // Position avec décalage aléatoire pour éviter le chevauchement
        double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
        double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.5;
        double offsetY = 1.5 + ThreadLocalRandom.current().nextDouble() * 0.5;

        Location loc = entity.getLocation().add(offsetX, offsetY, offsetZ);

        // Créer l'ArmorStand invisible avec le texte des dégâts
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ArmorStand indicator = (ArmorStand) entity.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

            // Configuration de l'ArmorStand
            indicator.setVisible(false);
            indicator.setGravity(false);
            indicator.setSmall(true);
            indicator.setMarker(true);
            indicator.setCustomNameVisible(true);
            indicator.setInvulnerable(true);
            indicator.setCollidable(false);

            // Format du texte selon le type de dégâts
            String damageText;
            if (isCritical) {
                damageText = "§6§l✦ §c§l" + String.format("%.1f", damage) + " §6§l✦";
            } else if (damage >= 50) {
                damageText = "§4§l" + String.format("%.1f", damage);
            } else if (damage >= 20) {
                damageText = "§c" + String.format("%.1f", damage);
            } else if (damage >= 10) {
                damageText = "§e" + String.format("%.1f", damage);
            } else {
                damageText = "§f" + String.format("%.1f", damage);
            }

            indicator.setCustomName(damageText);

            // Animation: monter puis disparaître
            new BukkitRunnable() {
                int ticks = 0;
                double currentY = loc.getY();

                @Override
                public void run() {
                    if (ticks >= 20 || !indicator.isValid()) { // 1 seconde
                        indicator.remove();
                        cancel();
                        return;
                    }

                    // Monter doucement
                    currentY += 0.05;
                    indicator.teleport(new Location(loc.getWorld(), loc.getX(), currentY, loc.getZ()));

                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        });
    }

    /**
     * Affiche un indicateur de soin
     */
    public void showHealIndicator(LivingEntity entity, double heal) {
        if (!enabled) return;

        double offsetX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
        double offsetZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.0;
        Location loc = entity.getLocation().add(offsetX, 1.8, offsetZ);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ArmorStand indicator = (ArmorStand) entity.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

            indicator.setVisible(false);
            indicator.setGravity(false);
            indicator.setSmall(true);
            indicator.setMarker(true);
            indicator.setCustomNameVisible(true);
            indicator.setInvulnerable(true);
            indicator.setCollidable(false);
            indicator.setCustomName("§a§l+" + String.format("%.1f", heal) + " ❤");

            new BukkitRunnable() {
                int ticks = 0;
                double currentY = loc.getY();

                @Override
                public void run() {
                    if (ticks >= 15 || !indicator.isValid()) {
                        indicator.remove();
                        cancel();
                        return;
                    }

                    currentY += 0.04;
                    indicator.teleport(new Location(loc.getWorld(), loc.getX(), currentY, loc.getZ()));
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        });
    }

    /**
     * Affiche un indicateur de miss/esquive
     */
    public void showMissIndicator(LivingEntity entity) {
        if (!enabled) return;

        Location loc = entity.getLocation().add(0, 2, 0);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ArmorStand indicator = (ArmorStand) entity.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

            indicator.setVisible(false);
            indicator.setGravity(false);
            indicator.setSmall(true);
            indicator.setMarker(true);
            indicator.setCustomNameVisible(true);
            indicator.setInvulnerable(true);
            indicator.setCollidable(false);
            indicator.setCustomName("§7§oMiss");

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    if (ticks >= 10 || !indicator.isValid()) {
                        indicator.remove();
                        cancel();
                        return;
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        });
    }
}
