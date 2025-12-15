package com.rinaorc.zombiez.combat;

import com.rinaorc.zombiez.ZombieZPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Système d'affichage des dégâts flottants optimisé
 * - Utilise TextDisplay (plus léger que ArmorStand)
 * - Affichage client-side uniquement pour le joueur concerné
 * - Système anti-stack intelligent pour éviter les superpositions
 * - Animations fluides avec interpolation
 * - Coups critiques en gras avec effets spéciaux
 */
public class DamageIndicator {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.#");
    private static final Random RANDOM = new Random();

    // Configuration - Tailles réduites pour une meilleure lisibilité
    private static final int DISPLAY_DURATION_TICKS = 20; // 1 seconde
    private static final int CRITICAL_DURATION_TICKS = 25; // 1.25 secondes pour critiques
    private static final float BASE_SCALE = 1.0f;      // Taille réduite (÷1.5)
    private static final float CRITICAL_SCALE = 1.4f;  // Taille réduite (÷1.5)
    private static final float HEAL_SCALE = 0.9f;      // Taille réduite (÷1.5)

    // Animation optimisée - mise à jour tous les 2 ticks pour moins de lag
    private static final int ANIMATION_INTERVAL_TICKS = 2;

    // Système anti-stack: garde trace des positions récentes par entité
    private static final Map<UUID, Deque<IndicatorSlot>> recentIndicators = new ConcurrentHashMap<>();
    private static final int MAX_TRACKED_INDICATORS = 8;
    private static final long INDICATOR_TRACKING_TIME_MS = 1500;

    // Angles de décalage en spirale pour éviter le stacking
    private static final double[] SPIRAL_ANGLES = {0, 72, 144, 216, 288, 36, 108, 180, 252, 324};
    private static final double[] SPIRAL_RADII = {0.3, 0.5, 0.7, 0.4, 0.6, 0.35, 0.55, 0.45, 0.65, 0.5};

    /**
     * Représente un slot d'indicateur pour le système anti-stack
     */
    private record IndicatorSlot(double offsetX, double offsetZ, double offsetY, long timestamp) {}

    /**
     * Affiche un indicateur de dégâts flottant visible uniquement par le joueur spécifié
     *
     * @param plugin   Instance du plugin
     * @param location Position où afficher les dégâts
     * @param damage   Montant des dégâts
     * @param critical Si c'est un coup critique
     * @param viewer   Le joueur qui verra l'indicateur (null = tous les joueurs proches)
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical, Player viewer) {
        if (location.getWorld() == null) return;

        // Calculer l'offset anti-stack
        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 1.2 + offset.getY(), offset.getZ());

        // Créer le TextDisplay
        location.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
            // Configuration de base
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // Transparent

            // Texte formaté
            Component text = formatDamageComponent(damage, critical);
            display.text(text);

            // Échelle initiale
            float scale = critical ? CRITICAL_SCALE : BASE_SCALE;

            // Pour les critiques, commencer plus gros et shrink
            float initialScale = critical ? scale * 1.4f : scale * 0.8f;

            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(initialScale, initialScale, initialScale),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            // Configurer l'interpolation pour animation fluide (durée augmentée pour plus de fluidité)
            display.setInterpolationDuration(10);
            display.setInterpolationDelay(0);

            // Cacher aux autres joueurs si un viewer spécifique est défini
            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            // Animation optimisée (moins de mises à jour pour réduire le lag)
            int duration = critical ? CRITICAL_DURATION_TICKS : DISPLAY_DURATION_TICKS;
            animateIndicatorOptimized(plugin, display, scale, duration, critical);
        });
    }

    /**
     * Affiche un indicateur de dégâts (version legacy compatible)
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical) {
        // Trouver le joueur le plus proche pour afficher l'indicateur principalement à lui
        Player nearestPlayer = findNearestPlayer(location, 50);
        display(plugin, location, damage, critical, nearestPlayer);
    }

    /**
     * Affiche un indicateur de soin
     */
    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount, Player viewer) {
        if (location.getWorld() == null) return;

        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 1.2 + offset.getY(), offset.getZ());

        location.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            Component text = Component.text("+" + FORMAT.format(amount) + " ", NamedTextColor.GREEN)
                .append(Component.text("❤", NamedTextColor.RED));
            display.text(text);

            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(HEAL_SCALE * 0.8f, HEAL_SCALE * 0.8f, HEAL_SCALE * 0.8f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(6);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateHealIndicator(plugin, display);
        });
    }

    /**
     * Affiche un indicateur de soin (version legacy)
     */
    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayHeal(plugin, location, amount, nearestPlayer);
    }

    /**
     * Affiche un indicateur d'esquive
     */
    public static void displayDodge(ZombieZPlugin plugin, Location location, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.4, 0);

        location.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            Component text = Component.text("ESQUIVE!", NamedTextColor.YELLOW, TextDecoration.BOLD, TextDecoration.ITALIC);
            display.text(text);

            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(1.2f, 1.2f, 1.2f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(5);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateStatusIndicator(plugin, display, 18);
        });
    }

    /**
     * Affiche un indicateur d'esquive (version legacy)
     */
    public static void displayDodge(ZombieZPlugin plugin, Location location) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayDodge(plugin, location, nearestPlayer);
    }

    /**
     * Affiche un indicateur de bloc
     */
    public static void displayBlock(ZombieZPlugin plugin, Location location, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.4, 0);

        location.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            Component text = Component.text("BLOQUÉ!", TextColor.color(0x5555FF), TextDecoration.BOLD);
            display.text(text);

            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(1.2f, 1.2f, 1.2f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(5);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateStatusIndicator(plugin, display, 18);
        });
    }

    /**
     * Affiche un indicateur de bloc (version legacy)
     */
    public static void displayBlock(ZombieZPlugin plugin, Location location) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayBlock(plugin, location, nearestPlayer);
    }

    /**
     * Affiche un indicateur d'immunité
     */
    public static void displayImmune(ZombieZPlugin plugin, Location location, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.4, 0);

        location.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            Component text = Component.text("IMMUNISÉ", NamedTextColor.GRAY, TextDecoration.ITALIC);
            display.text(text);

            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(1.05f, 1.05f, 1.05f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(5);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateStatusIndicator(plugin, display, 15);
        });
    }

    /**
     * Affiche un indicateur de combo
     */
    public static void displayCombo(ZombieZPlugin plugin, Location location, int comboCount, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.6, 0);

        location.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            // Couleur basée sur le combo
            TextColor comboColor;
            if (comboCount >= 50) {
                comboColor = TextColor.color(0xFF5555); // Rouge
            } else if (comboCount >= 25) {
                comboColor = TextColor.color(0xFFAA00); // Orange
            } else if (comboCount >= 10) {
                comboColor = TextColor.color(0xFFFF55); // Jaune
            } else {
                comboColor = TextColor.color(0xAAAAAA); // Gris
            }

            Component text = Component.text(comboCount + "x COMBO!", comboColor, TextDecoration.BOLD);
            display.text(text);

            float scale = 0.9f + Math.min(comboCount * 0.03f, 0.9f);

            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale * 0.8f, scale * 0.8f, scale * 0.8f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(5);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateComboIndicator(plugin, display, scale);
        });
    }

    /**
     * Formate le texte des dégâts en Component avec styles appropriés
     */
    private static Component formatDamageComponent(double damage, boolean critical) {
        String formattedDamage = FORMAT.format(damage);

        if (critical) {
            // Coup critique: gros, gras, rouge avec étoiles dorées
            return Component.text("✦ ", TextColor.color(0xFFD700), TextDecoration.BOLD)
                .append(Component.text(formattedDamage, TextColor.color(0xFF4444), TextDecoration.BOLD))
                .append(Component.text(" ✦", TextColor.color(0xFFD700), TextDecoration.BOLD));
        } else if (damage >= 500) {
            // Dégâts massifs - Rouge foncé gras
            return Component.text(formattedDamage, TextColor.color(0xAA0000), TextDecoration.BOLD);
        } else if (damage >= 200) {
            // Très gros dégâts - Rouge gras
            return Component.text(formattedDamage, TextColor.color(0xFF3333), TextDecoration.BOLD);
        } else if (damage >= 100) {
            // Gros dégâts - Rouge
            return Component.text(formattedDamage, TextColor.color(0xFF5555));
        } else if (damage >= 50) {
            // Dégâts moyens-hauts - Orange
            return Component.text(formattedDamage, TextColor.color(0xFFAA00));
        } else if (damage >= 20) {
            // Dégâts moyens - Jaune
            return Component.text(formattedDamage, TextColor.color(0xFFFF55));
        } else if (damage >= 5) {
            // Petits dégâts - Blanc
            return Component.text(formattedDamage, NamedTextColor.WHITE);
        } else {
            // Dégâts mineurs - Gris
            return Component.text(formattedDamage, NamedTextColor.GRAY);
        }
    }

    /**
     * Calcule un offset intelligent pour éviter le stacking des indicateurs
     */
    private static Vector calculateAntiStackOffset(Location location, Player viewer) {
        UUID key = viewer != null ? viewer.getUniqueId() :
                   (location.getWorld() != null ? location.getWorld().getUID() : UUID.randomUUID());

        long now = System.currentTimeMillis();

        // Nettoyer les vieux indicateurs
        recentIndicators.computeIfPresent(key, (k, slots) -> {
            slots.removeIf(slot -> now - slot.timestamp() > INDICATOR_TRACKING_TIME_MS);
            return slots.isEmpty() ? null : slots;
        });

        // Obtenir ou créer la liste des slots
        Deque<IndicatorSlot> slots = recentIndicators.computeIfAbsent(key, k -> new LinkedList<>());

        // Calculer le prochain offset basé sur le nombre d'indicateurs actifs
        int slotIndex = slots.size() % SPIRAL_ANGLES.length;
        double angle = Math.toRadians(SPIRAL_ANGLES[slotIndex] + RANDOM.nextDouble() * 20 - 10);
        double radius = SPIRAL_RADII[slotIndex] + RANDOM.nextDouble() * 0.15;

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        double offsetY = slots.size() * 0.15; // Léger décalage vertical

        // Ajouter ce slot
        IndicatorSlot newSlot = new IndicatorSlot(offsetX, offsetZ, offsetY, now);
        slots.addLast(newSlot);

        // Limiter le nombre de slots trackés
        while (slots.size() > MAX_TRACKED_INDICATORS) {
            slots.removeFirst();
        }

        return new Vector(offsetX, offsetY, offsetZ);
    }

    /**
     * Cache l'entité TextDisplay aux autres joueurs
     */
    private static void hideFromOtherPlayers(TextDisplay display, Player viewer) {
        // Utiliser le système de visibilité de Paper/Spigot
        display.setVisibleByDefault(false);
        viewer.showEntity(ZombieZPlugin.getInstance(), display);
    }

    /**
     * Trouve le joueur le plus proche d'une location
     */
    private static Player findNearestPlayer(Location location, double radius) {
        if (location.getWorld() == null) return null;

        Player nearest = null;
        double nearestDist = radius * radius;

        for (Player player : location.getWorld().getPlayers()) {
            double dist = player.getLocation().distanceSquared(location);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = player;
            }
        }

        return nearest;
    }

    /**
     * Anime l'indicateur de dégâts avec mouvement fluide et optimisé
     * Mise à jour tous les 2 ticks pour réduire le lag
     */
    private static void animateIndicatorOptimized(ZombieZPlugin plugin, TextDisplay display, float targetScale, int duration, boolean critical) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final Location startLoc = display.getLocation().clone();
            private final float startScale = critical ? targetScale * 1.3f : targetScale * 0.85f;
            private final int effectiveDuration = duration / ANIMATION_INTERVAL_TICKS;

            @Override
            public void run() {
                if (ticks >= effectiveDuration || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / effectiveDuration;

                // Mouvement: ease-out cubic plus fluide
                double easeOut = 1 - Math.pow(1 - progress, 2.5);
                double yOffset = easeOut * 0.5; // Réduit de 0.6 à 0.5 pour moins de mouvement

                // Téléporter vers le haut
                Location newLoc = startLoc.clone().add(0, yOffset, 0);
                display.teleport(newLoc);

                // Animation de scale avec transitions plus douces
                float currentScale;
                if (critical) {
                    // Critique: shrink fluide
                    if (progress < 0.15) {
                        // Pop-in rapide
                        float popProgress = progress / 0.15f;
                        currentScale = startScale - (startScale - targetScale) * easeOutQuad(popProgress);
                    } else if (progress > 0.75) {
                        // Fade-out progressif
                        float fadeProgress = (progress - 0.75f) / 0.25f;
                        currentScale = targetScale * (1 - easeInQuad(fadeProgress) * 0.4f);
                    } else {
                        currentScale = targetScale;
                    }
                } else {
                    // Normal: grow puis shrink fluide
                    if (progress < 0.12) {
                        // Grow-in
                        float growProgress = progress / 0.12f;
                        currentScale = startScale + (targetScale - startScale) * easeOutQuad(growProgress);
                    } else if (progress > 0.75) {
                        // Fade-out progressif
                        float fadeProgress = (progress - 0.75f) / 0.25f;
                        currentScale = targetScale * (1 - easeInQuad(fadeProgress) * 0.5f);
                    } else {
                        currentScale = targetScale;
                    }
                }

                // Appliquer la transformation avec interpolation native
                display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(currentScale, currentScale, currentScale),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, ANIMATION_INTERVAL_TICKS);
    }

    // Fonctions d'easing pour des animations plus fluides
    private static float easeOutQuad(float t) {
        return 1 - (1 - t) * (1 - t);
    }

    private static float easeInQuad(float t) {
        return t * t;
    }

    /**
     * Anime l'indicateur de soin (optimisé)
     */
    private static void animateHealIndicator(ZombieZPlugin plugin, TextDisplay display) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int effectiveDuration = 16 / ANIMATION_INTERVAL_TICKS;
            private final Location startLoc = display.getLocation().clone();

            @Override
            public void run() {
                if (ticks >= effectiveDuration || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / effectiveDuration;

                // Mouvement doux vers le haut avec easing
                double yOffset = easeOutQuad(progress) * 0.35;
                Location newLoc = startLoc.clone().add(0, yOffset, 0);
                display.teleport(newLoc);

                // Scale animation fluide
                float scale;
                if (progress < 0.15) {
                    scale = HEAL_SCALE * (0.85f + 0.3f * easeOutQuad(progress / 0.15f));
                } else if (progress > 0.75) {
                    float fadeProgress = (progress - 0.75f) / 0.25f;
                    scale = HEAL_SCALE * (1 - easeInQuad(fadeProgress) * 0.6f);
                } else {
                    scale = HEAL_SCALE;
                }

                display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, ANIMATION_INTERVAL_TICKS);
    }

    /**
     * Anime les indicateurs de statut (esquive, bloc, immunité) - optimisé
     */
    private static void animateStatusIndicator(ZombieZPlugin plugin, TextDisplay display, int duration) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int effectiveDuration = duration / ANIMATION_INTERVAL_TICKS;
            private final Location startLoc = display.getLocation().clone();
            private final float baseScale = 0.8f; // Taille réduite

            @Override
            public void run() {
                if (ticks >= effectiveDuration || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / effectiveDuration;

                // Mouvement vers le haut fluide
                double yOffset = easeOutQuad(progress) * 0.22;
                Location newLoc = startLoc.clone().add(0, yOffset, 0);
                display.teleport(newLoc);

                // Scale avec pop-in fluide
                float scale;
                if (progress < 0.12) {
                    scale = baseScale * (0.6f + 0.6f * easeOutQuad(progress / 0.12f));
                } else if (progress > 0.8) {
                    float fadeProgress = (progress - 0.8f) / 0.2f;
                    scale = baseScale * (1 - easeInQuad(fadeProgress) * 0.7f);
                } else {
                    scale = baseScale;
                }

                display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, ANIMATION_INTERVAL_TICKS);
    }

    /**
     * Anime l'indicateur de combo - optimisé
     */
    private static void animateComboIndicator(ZombieZPlugin plugin, TextDisplay display, float targetScale) {
        // Réduire la taille du combo aussi
        float reducedScale = targetScale / 1.5f;

        new BukkitRunnable() {
            private int ticks = 0;
            private final int effectiveDuration = 18 / ANIMATION_INTERVAL_TICKS;
            private final Location startLoc = display.getLocation().clone();

            @Override
            public void run() {
                if (ticks >= effectiveDuration || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / effectiveDuration;

                // Mouvement vers le haut fluide
                double yOffset = easeOutQuad(progress) * 0.25;
                Location newLoc = startLoc.clone().add(0, yOffset, 0);
                display.teleport(newLoc);

                // Scale fluide sans wobble (moins de lag)
                float scale;
                if (progress < 0.15) {
                    scale = reducedScale * (0.85f + 0.3f * easeOutQuad(progress / 0.15f));
                } else if (progress > 0.75) {
                    float fadeProgress = (progress - 0.75f) / 0.25f;
                    scale = reducedScale * (1 - easeInQuad(fadeProgress) * 0.6f);
                } else {
                    scale = reducedScale;
                }

                display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, ANIMATION_INTERVAL_TICKS);
    }

    /**
     * Nettoie le cache des indicateurs (appelé périodiquement)
     */
    public static void cleanup() {
        long now = System.currentTimeMillis();
        recentIndicators.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(slot -> now - slot.timestamp() > INDICATOR_TRACKING_TIME_MS);
            return entry.getValue().isEmpty();
        });
    }
}
