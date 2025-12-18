package com.rinaorc.zombiez.dopamine;

import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.items.types.Rarity;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Random;

/**
 * Système d'explosion visuelle pour les drops de loot
 *
 * Effet dopamine: Crée une récompense visuelle et sonore immédiate et satisfaisante
 * quand un item rare droppe, renforçant le plaisir du joueur.
 *
 * Fonctionnalités:
 * - Explosion de particules au moment du drop
 * - Fontaine de particules qui retombent
 * - Beam de lumière pour les items rares+
 * - Sons progressifs selon la rareté
 * - Effet "Lucky!" pour les drops très rares
 *
 * @author ZombieZ Dopamine System
 */
public class LootExplosionManager {

    private final ZombieZPlugin plugin;
    private final Random random = new Random();

    public LootExplosionManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Déclenche une explosion de loot à une location donnée
     *
     * @param location Location du drop
     * @param rarity   Rareté de l'item
     * @param killer   Le joueur qui a tué le mob (peut être null)
     */
    public void triggerExplosion(Location location, Rarity rarity, Player killer) {
        if (location.getWorld() == null) return;

        // Intensité basée sur la rareté
        ExplosionIntensity intensity = getIntensity(rarity);

        // 1. Explosion initiale instantanée
        spawnInitialBurst(location, rarity, intensity);

        // 2. Sons satisfaisants
        playDropSounds(location, rarity, killer);

        // 3. Effet de fontaine de particules (animé)
        if (intensity.hasFountain) {
            startFountainEffect(location, rarity, intensity);
        }

        // 4. Beam de lumière vers le ciel
        if (intensity.hasLightBeam) {
            startLightBeam(location, rarity);
        }

        // 5. Notification "LUCKY!" pour les drops très rares
        if (rarity.isAtLeast(Rarity.LEGENDARY) && killer != null) {
            showLuckyPopup(killer, rarity);
        }
    }

    /**
     * Spawn l'explosion initiale de particules
     */
    private void spawnInitialBurst(Location location, Rarity rarity, ExplosionIntensity intensity) {
        Location center = location.clone().add(0, 0.5, 0);

        // Particules principales selon la rareté
        Particle mainParticle = getMainParticle(rarity);
        Color dustColor = rarity.getGlowColor();

        // Explosion sphérique
        for (int i = 0; i < intensity.burstParticleCount; i++) {
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;

            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.cos(phi);
            double z = Math.sin(phi) * Math.sin(theta);

            Vector direction = new Vector(x, y, z).normalize().multiply(intensity.burstRadius);
            Location particleLoc = center.clone().add(direction);

            location.getWorld().spawnParticle(mainParticle, particleLoc, 1, 0, 0, 0, 0);
        }

        // Poussière colorée selon la rareté
        Particle.DustOptions dustOptions = new Particle.DustOptions(dustColor, 1.5f);
        location.getWorld().spawnParticle(
            Particle.DUST,
            center,
            intensity.burstParticleCount / 2,
            0.5, 0.5, 0.5, 0,
            dustOptions
        );

        // Effet de flash/étoile au centre
        if (rarity.isAtLeast(Rarity.RARE)) {
            location.getWorld().spawnParticle(
                Particle.FLASH,
                center,
                1,
                0, 0, 0, 0
            );
            location.getWorld().spawnParticle(
                Particle.END_ROD,
                center,
                8,
                0.3, 0.3, 0.3, 0.1
            );
        }

        // Explosion de fireworks pour legendary+
        if (rarity.isAtLeast(Rarity.LEGENDARY)) {
            location.getWorld().spawnParticle(
                Particle.FIREWORK,
                center,
                25,
                0.5, 0.5, 0.5, 0.15
            );
        }
    }

    /**
     * Joue les sons de drop selon la rareté
     */
    private void playDropSounds(Location location, Rarity rarity, Player killer) {
        // Son principal selon la rareté
        switch (rarity) {
            case EXALTED -> {
                // Son épique de victoire
                location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.5f, 0.8f);
                location.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);
                location.getWorld().playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.2f);
                // Son personnel au joueur
                if (killer != null) {
                    killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);
                }
            }
            case MYTHIC -> {
                location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.7f);
                location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.2f);
                location.getWorld().playSound(location, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1f, 1.5f);
            }
            case LEGENDARY -> {
                location.getWorld().playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                location.getWorld().playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
                location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.8f);
            }
            case EPIC -> {
                location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
                location.getWorld().playSound(location, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1f, 1f);
            }
            case RARE -> {
                location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
            }
            case UNCOMMON -> {
                location.getWorld().playSound(location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
            }
            default -> {
                location.getWorld().playSound(location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1f);
            }
        }
    }

    /**
     * Démarre l'effet de fontaine de particules qui retombent
     */
    private void startFountainEffect(Location location, Rarity rarity, ExplosionIntensity intensity) {
        Particle fountainParticle = getFountainParticle(rarity);
        Color dustColor = rarity.getGlowColor();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = intensity.fountainDuration;

            @Override
            public void run() {
                if (ticks >= maxTicks || location.getWorld() == null) {
                    cancel();
                    return;
                }

                Location center = location.clone().add(0, 0.5 + (ticks * 0.15), 0);

                // Phase montante (premiers ticks)
                if (ticks < maxTicks / 2) {
                    // Particules qui montent en spirale
                    for (int i = 0; i < 3; i++) {
                        double angle = (ticks * 0.3) + (i * Math.PI * 2 / 3);
                        double radius = 0.3 + (ticks * 0.02);
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;

                        location.getWorld().spawnParticle(
                            fountainParticle,
                            center.clone().add(x, 0, z),
                            1, 0, 0.1, 0, 0
                        );
                    }
                }
                // Phase descendante (particules qui retombent)
                else {
                    Particle.DustOptions dust = new Particle.DustOptions(dustColor, 1.0f);
                    for (int i = 0; i < 5; i++) {
                        double angle = random.nextDouble() * Math.PI * 2;
                        double radius = random.nextDouble() * 1.5;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        double y = (maxTicks - ticks) * 0.1;

                        location.getWorld().spawnParticle(
                            Particle.DUST,
                            location.clone().add(x, y + 0.5, z),
                            1, 0, -0.1, 0, 0,
                            dust
                        );
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Démarre le beam de lumière vers le ciel
     */
    private void startLightBeam(Location location, Rarity rarity) {
        Particle beamParticle = getBeamParticle(rarity);
        Color dustColor = rarity.getGlowColor();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 30; // 1.5 secondes
            double currentHeight = 0;
            final double maxHeight = getBeamHeight(rarity);

            @Override
            public void run() {
                if (ticks >= maxTicks || location.getWorld() == null) {
                    cancel();
                    return;
                }

                // Montée progressive du beam
                currentHeight = Math.min(maxHeight, currentHeight + (maxHeight / 15));

                // Particules du beam
                for (double y = 0; y < currentHeight; y += 0.3) {
                    // Léger mouvement ondulant
                    double wave = Math.sin(y * 2 + ticks * 0.3) * 0.05;

                    location.getWorld().spawnParticle(
                        beamParticle,
                        location.clone().add(wave, y + 0.5, wave),
                        1, 0, 0, 0, 0
                    );
                }

                // Particules de dust colorées autour du beam
                if (ticks % 3 == 0) {
                    Particle.DustOptions dust = new Particle.DustOptions(dustColor, 1.2f);
                    location.getWorld().spawnParticle(
                        Particle.DUST,
                        location.clone().add(0, currentHeight / 2, 0),
                        5, 0.2, currentHeight / 4, 0.2, 0,
                        dust
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Affiche le popup "LUCKY!" au joueur
     */
    private void showLuckyPopup(Player player, Rarity rarity) {
        // Titre avec effet
        String title;
        String subtitle;

        switch (rarity) {
            case EXALTED -> {
                title = "§c§l✦ EXALTED DROP! ✦";
                subtitle = "§6§lINCROYABLE!";
            }
            case MYTHIC -> {
                title = "§d§l★ MYTHIQUE! ★";
                subtitle = "§eChance incroyable!";
            }
            case LEGENDARY -> {
                title = "§6§l✦ LÉGENDAIRE! ✦";
                subtitle = "§7Quelle chance!";
            }
            default -> {
                title = rarity.getChatColor() + "§lLUCKY!";
                subtitle = "§7Drop rare!";
            }
        }

        // Envoyer le title
        player.sendTitle(title, subtitle, 5, 30, 10);

        // Particules personnelles autour du joueur
        spawnPlayerCelebration(player, rarity);
    }

    /**
     * Spawn des particules de célébration autour du joueur
     */
    private void spawnPlayerCelebration(Player player, Rarity rarity) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 20 || !player.isOnline()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 1, 0);

                // Spirale de particules autour du joueur
                double angle = ticks * 0.5;
                for (int i = 0; i < 3; i++) {
                    double a = angle + (i * Math.PI * 2 / 3);
                    double radius = 1.0;
                    double x = Math.cos(a) * radius;
                    double z = Math.sin(a) * radius;
                    double y = ticks * 0.1;

                    player.getWorld().spawnParticle(
                        Particle.TOTEM_OF_UNDYING,
                        loc.clone().add(x, y, z),
                        1, 0, 0, 0, 0
                    );
                }

                // Son de célébration
                if (ticks == 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
                }
                if (ticks == 10) {
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.5f, 1f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS - Configuration des effets
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Obtient l'intensité de l'explosion selon la rareté
     */
    private ExplosionIntensity getIntensity(Rarity rarity) {
        return switch (rarity) {
            case EXALTED -> new ExplosionIntensity(80, 2.0, true, 40, true);
            case MYTHIC -> new ExplosionIntensity(60, 1.5, true, 35, true);
            case LEGENDARY -> new ExplosionIntensity(45, 1.2, true, 30, true);
            case EPIC -> new ExplosionIntensity(30, 1.0, true, 25, true);
            case RARE -> new ExplosionIntensity(20, 0.8, true, 20, false);
            case UNCOMMON -> new ExplosionIntensity(10, 0.5, false, 0, false);
            default -> new ExplosionIntensity(5, 0.3, false, 0, false);
        };
    }

    /**
     * Obtient la particule principale selon la rareté
     */
    private Particle getMainParticle(Rarity rarity) {
        return switch (rarity) {
            case EXALTED -> Particle.END_ROD;
            case MYTHIC -> Particle.DRAGON_BREATH;
            case LEGENDARY -> Particle.FLAME;
            case EPIC -> Particle.ENCHANT;
            case RARE -> Particle.GLOW;
            case UNCOMMON -> Particle.HAPPY_VILLAGER;
            default -> Particle.CRIT;
        };
    }

    /**
     * Obtient la particule de fontaine selon la rareté
     */
    private Particle getFountainParticle(Rarity rarity) {
        return switch (rarity) {
            case EXALTED, MYTHIC -> Particle.TOTEM_OF_UNDYING;
            case LEGENDARY -> Particle.FLAME;
            case EPIC -> Particle.SOUL_FIRE_FLAME;
            default -> Particle.ENCHANT;
        };
    }

    /**
     * Obtient la particule du beam selon la rareté
     */
    private Particle getBeamParticle(Rarity rarity) {
        return switch (rarity) {
            case EXALTED -> Particle.END_ROD;
            case MYTHIC -> Particle.DRAGON_BREATH;
            case LEGENDARY -> Particle.FLAME;
            case EPIC -> Particle.SOUL_FIRE_FLAME;
            default -> Particle.END_ROD;
        };
    }

    /**
     * Obtient la hauteur du beam selon la rareté
     */
    private double getBeamHeight(Rarity rarity) {
        return switch (rarity) {
            case EXALTED -> 12.0;
            case MYTHIC -> 10.0;
            case LEGENDARY -> 8.0;
            case EPIC -> 6.0;
            default -> 4.0;
        };
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Configuration de l'intensité d'une explosion
     */
    private record ExplosionIntensity(
        int burstParticleCount,
        double burstRadius,
        boolean hasFountain,
        int fountainDuration,
        boolean hasLightBeam
    ) {}
}
