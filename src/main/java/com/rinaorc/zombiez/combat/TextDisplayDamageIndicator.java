package com.rinaorc.zombiez.combat;

import com.rinaorc.zombiez.ZombieZPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Systeme d'indicateurs de degats ultra-fluide via TextDisplay natif Bukkit
 *
 * Caracteristiques:
 * - Utilise de vraies entites TextDisplay (100% Bukkit, pas de ProtocolLib)
 * - Animation fluide via setTransformation() et setInterpolationDuration()
 * - Interpolation client-side pour 60Hz+ de fluidite
 * - Nettoyage automatique apres animation avec display.remove()
 * - Culling intelligent a 16 blocs
 * - Easing Cubic Out pour une deceleration naturelle
 *
 * Architecture inspiree de PetDisplayManager pour la gestion des TextDisplay.
 *
 * @author Rinaorc Studio
 */
public class TextDisplayDamageIndicator {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.#");

    // Configuration d'animation
    private static final int ANIMATION_DURATION_TICKS = 20; // 1 seconde
    private static final int CRITICAL_DURATION_TICKS = 26; // 1.3 secondes
    private static final double TOTAL_RISE_DISTANCE = 1.2; // Blocs montes
    private static final double VIEW_DISTANCE = 16.0; // Rayon de visibilite

    // Duree d'interpolation pour fluidite client-side (en ticks)
    // Plus long = plus fluide mais moins reactif
    private static final int INTERPOLATION_DURATION = 3;

    // Scales
    private static final float BASE_SCALE = 1.0f;
    private static final float CRITICAL_SCALE = 1.4f;
    private static final float HEAL_SCALE = 0.9f;

    // Systeme anti-stack
    private static final Map<UUID, Deque<IndicatorSlot>> recentIndicators = new ConcurrentHashMap<>();
    private static final int MAX_TRACKED_INDICATORS = 8;
    private static final long INDICATOR_TRACKING_TIME_MS = 1500;
    private static final double[] SPIRAL_ANGLES = {0, 72, 144, 216, 288, 36, 108, 180, 252, 324};
    private static final double[] SPIRAL_RADII = {0.3, 0.5, 0.7, 0.4, 0.6, 0.35, 0.55, 0.45, 0.65, 0.5};

    private record IndicatorSlot(double offsetX, double offsetZ, double offsetY, long timestamp) {}

    /**
     * Affiche un indicateur de degats ultra-fluide avec TextDisplay natif
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical, Player viewer) {
        if (location.getWorld() == null) return;

        // Calculer l'offset anti-stack
        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 1.2 + offset.getY(), offset.getZ());

        // Verifier si le viewer est dans le range
        if (viewer != null && viewer.getLocation().distanceSquared(spawnLoc) > VIEW_DISTANCE * VIEW_DISTANCE) {
            return;
        }

        // Composant de texte formate
        Component text = formatDamageComponent(damage, critical);

        // Configuration
        float scale = critical ? CRITICAL_SCALE : BASE_SCALE;
        int duration = critical ? CRITICAL_DURATION_TICKS : ANIMATION_DURATION_TICKS;
        float initialScale = critical ? scale * 1.3f : scale * 0.85f;

        // Spawn le TextDisplay
        try {
            TextDisplay display = spawnTextDisplay(spawnLoc, text, initialScale, viewer);
            if (display != null) {
                animateDamage(plugin, display, spawnLoc, scale, duration, critical);
            }
        } catch (Exception e) {
            // Fallback vers l'ancien systeme si erreur
            DamageIndicator.display(plugin, location, damage, critical, viewer);
        }
    }

    /**
     * Version legacy compatible
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical) {
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

        Component text = Component.text("+" + FORMAT.format(amount) + " ", NamedTextColor.GREEN)
            .append(Component.text("\u2764", NamedTextColor.RED));

        try {
            TextDisplay display = spawnTextDisplay(spawnLoc, text, HEAL_SCALE * 0.75f, viewer);
            if (display != null) {
                animateHeal(plugin, display, spawnLoc);
            }
        } catch (Exception e) {
            DamageIndicator.displayHeal(plugin, location, amount, viewer);
        }
    }

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

        Component text = Component.text("ESQUIVE!", NamedTextColor.YELLOW, TextDecoration.BOLD, TextDecoration.ITALIC);

        try {
            TextDisplay display = spawnTextDisplay(spawnLoc, text, 0.5f, viewer);
            if (display != null) {
                animateStatus(plugin, display, spawnLoc, 21);
            }
        } catch (Exception e) {
            DamageIndicator.displayDodge(plugin, location, viewer);
        }
    }

    public static void displayDodge(ZombieZPlugin plugin, Location location) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayDodge(plugin, location, nearestPlayer);
    }

    /**
     * Affiche un indicateur de headshot
     */
    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage, Player viewer) {
        if (location.getWorld() == null) return;

        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 0.3 + offset.getY(), offset.getZ());

        String formattedDamage = FORMAT.format(damage);
        Component text = Component.text("\u2295 ", TextColor.color(0xFF6600), TextDecoration.BOLD)
            .append(Component.text(formattedDamage, TextColor.color(0xFFAA00), TextDecoration.BOLD))
            .append(Component.text(" \u2295", TextColor.color(0xFF6600), TextDecoration.BOLD));

        try {
            TextDisplay display = spawnTextDisplay(spawnLoc, text, 1.3f * 0.7f, viewer);
            if (display != null) {
                animateHeadshot(plugin, display, spawnLoc, 1.3f);
            }
        } catch (Exception e) {
            DamageIndicator.displayHeadshot(plugin, location, damage, viewer);
        }
    }

    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayHeadshot(plugin, location, damage, nearestPlayer);
    }

    /**
     * Affiche un indicateur de bloc
     */
    public static void displayBlock(ZombieZPlugin plugin, Location location, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.4, 0);

        Component text = Component.text("BLOQUE!", TextColor.color(0x5555FF), TextDecoration.BOLD);

        try {
            TextDisplay display = spawnTextDisplay(spawnLoc, text, 0.5f, viewer);
            if (display != null) {
                animateStatus(plugin, display, spawnLoc, 21);
            }
        } catch (Exception e) {
            DamageIndicator.displayBlock(plugin, location, viewer);
        }
    }

    public static void displayBlock(ZombieZPlugin plugin, Location location) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayBlock(plugin, location, nearestPlayer);
    }

    /**
     * Affiche un indicateur d'immunite
     */
    public static void displayImmune(ZombieZPlugin plugin, Location location, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.4, 0);

        Component text = Component.text("IMMUNISE", NamedTextColor.GRAY, TextDecoration.ITALIC);

        try {
            TextDisplay display = spawnTextDisplay(spawnLoc, text, 0.5f, viewer);
            if (display != null) {
                animateStatus(plugin, display, spawnLoc, 18);
            }
        } catch (Exception e) {
            DamageIndicator.displayImmune(plugin, location, viewer);
        }
    }

    /**
     * Affiche un indicateur de combo
     */
    public static void displayCombo(ZombieZPlugin plugin, Location location, int comboCount, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.6, 0);

        TextColor comboColor;
        if (comboCount >= 50) {
            comboColor = TextColor.color(0xFF5555);
        } else if (comboCount >= 25) {
            comboColor = TextColor.color(0xFFAA00);
        } else if (comboCount >= 10) {
            comboColor = TextColor.color(0xFFFF55);
        } else {
            comboColor = TextColor.color(0xAAAAAA);
        }

        Component text = Component.text(comboCount + "x COMBO!", comboColor, TextDecoration.BOLD);
        float scale = 0.9f + Math.min(comboCount * 0.03f, 0.9f);

        try {
            TextDisplay display = spawnTextDisplay(spawnLoc, text, scale * 0.6f, viewer);
            if (display != null) {
                animateCombo(plugin, display, spawnLoc, scale);
            }
        } catch (Exception e) {
            DamageIndicator.displayCombo(plugin, location, comboCount, viewer);
        }
    }

    // ========================================================================
    // TEXTDISPLAY SPAWN - Creation d'entites TextDisplay natives
    // ========================================================================

    /**
     * Spawn un TextDisplay natif avec configuration optimale pour les indicateurs
     */
    private static TextDisplay spawnTextDisplay(Location location, Component text, float scale, Player viewer) {
        if (location.getWorld() == null) return null;

        TextDisplay display = location.getWorld().spawn(location, TextDisplay.class, td -> {
            // Configuration du texte
            td.text(text);

            // Billboard center - toujours face a la camera
            td.setBillboard(Display.Billboard.CENTER);

            // Background transparent
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setDefaultBackground(false);

            // Ombre pour lisibilite
            td.setShadowed(true);
            td.setSeeThrough(false);

            // Transformation initiale avec scale
            td.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),           // Translation initiale
                new AxisAngle4f(0, 0, 0, 1),    // Rotation gauche (identite)
                new Vector3f(scale, scale, scale), // Echelle initiale
                new AxisAngle4f(0, 0, 0, 1)     // Rotation droite (identite)
            ));

            // Interpolation pour fluidite client-side
            td.setInterpolationDuration(INTERPOLATION_DURATION);
            td.setInterpolationDelay(0);

            // Configuration entite
            td.setPersistent(false);
            td.setInvulnerable(true);

            // Tag pour identification
            td.addScoreboardTag("zombiez_damage_indicator");

            // Visibilite limitee au viewer si specifie
            if (viewer != null) {
                td.setVisibleByDefault(false);
                viewer.showEntity(ZombieZPlugin.getInstance(), td);
            }
        });

        return display;
    }

    /**
     * Met a jour la transformation du TextDisplay pour l'animation
     * Utilise setInterpolationDuration pour fluidite client-side
     */
    private static void updateTransformation(TextDisplay display, float yOffset, float scale) {
        if (!display.isValid()) return;

        display.setInterpolationDuration(INTERPOLATION_DURATION);
        display.setTransformation(new Transformation(
            new Vector3f(0, yOffset, 0),        // Translation Y pour mouvement
            new AxisAngle4f(0, 0, 0, 1),        // Rotation gauche
            new Vector3f(scale, scale, scale), // Echelle
            new AxisAngle4f(0, 0, 0, 1)         // Rotation droite
        ));
    }

    // ========================================================================
    // ANIMATION METHODS - Animations fluides avec interpolation
    // ========================================================================

    /**
     * Animation principale des degats
     * Utilise l'interpolation native pour une fluidite maximale
     */
    private static void animateDamage(ZombieZPlugin plugin, TextDisplay display, Location startLoc,
                                       float targetScale, int duration, boolean critical) {
        new BukkitRunnable() {
            private int tick = 0;
            private final float startScale = critical ? targetScale * 1.3f : targetScale * 0.85f;

            @Override
            public void run() {
                if (tick >= duration || !display.isValid()) {
                    // Nettoyage automatique de l'entite
                    if (display.isValid()) {
                        display.remove();
                    }
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;

                // Easing Cubic Out: 1 - (1 - t)^3
                float easedProgress = easeOutCubic(progress);

                // Calcul du deplacement Y avec easing
                float yOffset = (float) (easedProgress * TOTAL_RISE_DISTANCE);

                // Calcul du scale avec animation
                float currentScale;
                if (critical) {
                    // Critique: shrink depuis grand vers normal
                    if (progress < 0.12f) {
                        float popProgress = progress / 0.12f;
                        currentScale = startScale - (startScale - targetScale) * easeOutCubic(popProgress);
                    } else if (progress > 0.72f) {
                        float fadeProgress = (progress - 0.72f) / 0.28f;
                        currentScale = targetScale * (1 - easeInCubic(fadeProgress) * 0.45f);
                    } else {
                        currentScale = targetScale;
                    }
                } else {
                    // Normal: grow puis shrink
                    if (progress < 0.1f) {
                        float growProgress = progress / 0.1f;
                        currentScale = startScale + (targetScale - startScale) * easeOutCubic(growProgress);
                    } else if (progress > 0.72f) {
                        float fadeProgress = (progress - 0.72f) / 0.28f;
                        currentScale = targetScale * (1 - easeInCubic(fadeProgress) * 0.55f);
                    } else {
                        currentScale = targetScale;
                    }
                }

                // Mise a jour via transformation (interpolation client-side)
                updateTransformation(display, yOffset, currentScale);

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Animation de soin
     */
    private static void animateHeal(ZombieZPlugin plugin, TextDisplay display, Location startLoc) {
        new BukkitRunnable() {
            private int tick = 0;
            private final int duration = 18;

            @Override
            public void run() {
                if (tick >= duration || !display.isValid()) {
                    if (display.isValid()) {
                        display.remove();
                    }
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;
                float easedProgress = easeOutCubic(progress);

                float yOffset = easedProgress * 0.4f;

                float scale;
                if (progress < 0.12f) {
                    scale = HEAL_SCALE * (0.8f + 0.35f * easeOutBack(progress / 0.12f));
                } else if (progress > 0.7f) {
                    float fadeProgress = (progress - 0.7f) / 0.3f;
                    scale = HEAL_SCALE * (1 - easeInCubic(fadeProgress) * 0.65f);
                } else {
                    scale = HEAL_SCALE;
                }

                updateTransformation(display, yOffset, scale);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Animation de statut (esquive, bloc, immunite)
     */
    private static void animateStatus(ZombieZPlugin plugin, TextDisplay display, Location startLoc, int duration) {
        new BukkitRunnable() {
            private int tick = 0;
            private final float baseScale = 0.85f;

            @Override
            public void run() {
                if (tick >= duration || !display.isValid()) {
                    if (display.isValid()) {
                        display.remove();
                    }
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;
                float easedProgress = easeOutCubic(progress);

                float yOffset = easedProgress * 0.28f;

                float scale;
                if (progress < 0.1f) {
                    scale = baseScale * (0.5f + 0.7f * easeOutBack(progress / 0.1f));
                } else if (progress > 0.75f) {
                    float fadeProgress = (progress - 0.75f) / 0.25f;
                    scale = baseScale * (1 - easeInCubic(fadeProgress) * 0.75f);
                } else {
                    scale = baseScale;
                }

                updateTransformation(display, yOffset, scale);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Animation de headshot
     */
    private static void animateHeadshot(ZombieZPlugin plugin, TextDisplay display, Location startLoc, float targetScale) {
        new BukkitRunnable() {
            private int tick = 0;
            private final int duration = 24;

            @Override
            public void run() {
                if (tick >= duration || !display.isValid()) {
                    if (display.isValid()) {
                        display.remove();
                    }
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;
                float easedProgress = easeOutCubic(progress);

                float yOffset = easedProgress * 0.45f;

                float scale;
                if (progress < 0.08f) {
                    float popProgress = progress / 0.08f;
                    scale = targetScale * (0.7f + 0.5f * easeOutBack(popProgress));
                } else if (progress < 0.18f) {
                    float settleProgress = (progress - 0.08f) / 0.1f;
                    scale = targetScale * (1.2f - 0.2f * easeOutCubic(settleProgress));
                } else if (progress > 0.68f) {
                    float fadeProgress = (progress - 0.68f) / 0.32f;
                    scale = targetScale * (1 - easeInCubic(fadeProgress) * 0.55f);
                } else {
                    scale = targetScale;
                }

                updateTransformation(display, yOffset, scale);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Animation de combo
     */
    private static void animateCombo(ZombieZPlugin plugin, TextDisplay display, Location startLoc, float targetScale) {
        float reducedScale = targetScale / 1.4f;

        new BukkitRunnable() {
            private int tick = 0;
            private final int duration = 20;

            @Override
            public void run() {
                if (tick >= duration || !display.isValid()) {
                    if (display.isValid()) {
                        display.remove();
                    }
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;
                float easedProgress = easeOutCubic(progress);

                float yOffset = easedProgress * 0.3f;

                float scale;
                if (progress < 0.12f) {
                    scale = reducedScale * (0.8f + 0.35f * easeOutBack(progress / 0.12f));
                } else if (progress > 0.7f) {
                    float fadeProgress = (progress - 0.7f) / 0.3f;
                    scale = reducedScale * (1 - easeInCubic(fadeProgress) * 0.65f);
                } else {
                    scale = reducedScale;
                }

                updateTransformation(display, yOffset, scale);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ========================================================================
    // EASING FUNCTIONS - Courbes d'animation fluides
    // ========================================================================

    /**
     * Cubic Out - Deceleration naturelle (vitesse elevee au debut, lente a la fin)
     * Formule: 1 - (1 - t)^3
     */
    private static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    /**
     * Cubic In - Acceleration progressive
     */
    private static float easeInCubic(float t) {
        return t * t * t;
    }

    /**
     * Back Out - Effet de depassement puis retour (overshoot)
     */
    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Formate le texte des degats avec couleurs appropriees
     */
    private static Component formatDamageComponent(double damage, boolean critical) {
        String formattedDamage = FORMAT.format(damage);

        if (critical) {
            return Component.text("\u2726 ", TextColor.color(0xFFD700), TextDecoration.BOLD)
                .append(Component.text(formattedDamage, TextColor.color(0xFF4444), TextDecoration.BOLD))
                .append(Component.text(" \u2726", TextColor.color(0xFFD700), TextDecoration.BOLD));
        } else if (damage >= 50000) {
            return Component.text("\u2620 ", TextColor.color(0x8B0000), TextDecoration.BOLD)
                .append(Component.text(formattedDamage, TextColor.color(0x8B0000), TextDecoration.BOLD));
        } else if (damage >= 20000) {
            return Component.text(formattedDamage, TextColor.color(0xAA0000), TextDecoration.BOLD);
        } else if (damage >= 10000) {
            return Component.text(formattedDamage, TextColor.color(0xFF3333), TextDecoration.BOLD);
        } else if (damage >= 5000) {
            return Component.text(formattedDamage, TextColor.color(0xFF5555));
        } else if (damage >= 2000) {
            return Component.text(formattedDamage, TextColor.color(0xFFAA00));
        } else if (damage >= 500) {
            return Component.text(formattedDamage, TextColor.color(0xFFFF55));
        } else if (damage >= 100) {
            return Component.text(formattedDamage, NamedTextColor.WHITE);
        } else {
            return Component.text(formattedDamage, NamedTextColor.GRAY);
        }
    }

    /**
     * Calcule un offset anti-stack intelligent
     */
    private static Vector calculateAntiStackOffset(Location location, Player viewer) {
        UUID key = viewer != null ? viewer.getUniqueId() :
                   (location.getWorld() != null ? location.getWorld().getUID() : UUID.randomUUID());

        long now = System.currentTimeMillis();

        recentIndicators.computeIfPresent(key, (k, slots) -> {
            slots.removeIf(slot -> now - slot.timestamp() > INDICATOR_TRACKING_TIME_MS);
            return slots.isEmpty() ? null : slots;
        });

        Deque<IndicatorSlot> slots = recentIndicators.computeIfAbsent(key, k -> new LinkedList<>());

        int slotIndex = slots.size() % SPIRAL_ANGLES.length;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = Math.toRadians(SPIRAL_ANGLES[slotIndex] + random.nextDouble() * 20 - 10);
        double radius = SPIRAL_RADII[slotIndex] + random.nextDouble() * 0.15;

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        double offsetY = slots.size() * 0.15;

        IndicatorSlot newSlot = new IndicatorSlot(offsetX, offsetZ, offsetY, now);
        slots.addLast(newSlot);

        while (slots.size() > MAX_TRACKED_INDICATORS) {
            slots.removeFirst();
        }

        return new Vector(offsetX, offsetY, offsetZ);
    }

    /**
     * Trouve le joueur le plus proche
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
     * Nettoie le cache des indicateurs
     */
    public static void cleanup() {
        long now = System.currentTimeMillis();
        recentIndicators.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(slot -> now - slot.timestamp() > INDICATOR_TRACKING_TIME_MS);
            return entry.getValue().isEmpty();
        });
    }
}
