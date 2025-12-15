package com.rinaorc.zombiez.combat;

import com.rinaorc.zombiez.ZombieZPlugin;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.Random;

/**
 * Système d'affichage des dégâts flottants
 * Utilise des ArmorStand invisibles avec des noms customisés
 */
public class DamageIndicator {

    private static final DecimalFormat FORMAT = new DecimalFormat("#.#");
    private static final Random RANDOM = new Random();

    /**
     * Affiche un indicateur de dégâts flottant
     *
     * @param plugin   Instance du plugin
     * @param location Position où afficher les dégâts
     * @param damage   Montant des dégâts
     * @param critical Si c'est un coup critique
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical) {
        // Ajouter un léger décalage aléatoire pour éviter les superpositions
        double offsetX = (RANDOM.nextDouble() - 0.5) * 0.8;
        double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.8;
        Location spawnLoc = location.clone().add(offsetX, 1.5, offsetZ);

        // Créer l'ArmorStand
        ArmorStand stand = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);

        // Configurer l'ArmorStand
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.setCollidable(false);
        stand.setSilent(true);

        // Formater le texte des dégâts
        String damageText = formatDamage(damage, critical);
        stand.setCustomName(damageText);

        // Animation: monter puis disparaître
        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 20; // 1 seconde
            private final Vector velocity = new Vector(0, 0.05, 0);

            @Override
            public void run() {
                if (ticks >= maxTicks || !stand.isValid()) {
                    stand.remove();
                    cancel();
                    return;
                }

                // Faire monter l'indicateur
                stand.teleport(stand.getLocation().add(velocity));

                // Réduire la vélocité au fil du temps
                velocity.multiply(0.9);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Affiche un indicateur de soin
     */
    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount) {
        Location spawnLoc = location.clone().add(0, 1.5, 0);

        ArmorStand stand = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        configureStand(stand);

        String healText = "§a+" + FORMAT.format(amount) + " §c❤";
        stand.setCustomName(healText);

        animateAndRemove(plugin, stand, 15);
    }

    /**
     * Affiche un indicateur d'esquive
     */
    public static void displayDodge(ZombieZPlugin plugin, Location location) {
        Location spawnLoc = location.clone().add(0, 1.5, 0);

        ArmorStand stand = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        configureStand(stand);

        stand.setCustomName("§e§lESQUIVE!");

        animateAndRemove(plugin, stand, 15);
    }

    /**
     * Affiche un indicateur de bloc
     */
    public static void displayBlock(ZombieZPlugin plugin, Location location) {
        Location spawnLoc = location.clone().add(0, 1.5, 0);

        ArmorStand stand = (ArmorStand) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        configureStand(stand);

        stand.setCustomName("§9§lBLOQUÉ!");

        animateAndRemove(plugin, stand, 15);
    }

    /**
     * Formate le texte des dégâts selon le type
     */
    private static String formatDamage(double damage, boolean critical) {
        String formattedDamage = FORMAT.format(damage);

        if (critical) {
            // Dégâts critiques - plus gros et coloré
            return "§6§l✦ §c§l" + formattedDamage + " §6§l✦";
        } else if (damage >= 100) {
            // Très gros dégâts
            return "§c§l" + formattedDamage;
        } else if (damage >= 50) {
            // Gros dégâts
            return "§c" + formattedDamage;
        } else if (damage >= 20) {
            // Dégâts moyens
            return "§e" + formattedDamage;
        } else {
            // Petits dégâts
            return "§f" + formattedDamage;
        }
    }

    /**
     * Configure un ArmorStand pour l'affichage
     */
    private static void configureStand(ArmorStand stand) {
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setCustomNameVisible(true);
        stand.setInvulnerable(true);
        stand.setCollidable(false);
        stand.setSilent(true);
    }

    /**
     * Anime et supprime un ArmorStand après un délai
     */
    private static void animateAndRemove(ZombieZPlugin plugin, ArmorStand stand, int ticks) {
        new BukkitRunnable() {
            private int tick = 0;

            @Override
            public void run() {
                if (tick >= ticks || !stand.isValid()) {
                    stand.remove();
                    cancel();
                    return;
                }

                stand.teleport(stand.getLocation().add(0, 0.04, 0));
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
