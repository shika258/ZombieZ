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
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Système d'indicateurs de dégâts ULTRA-FLUIDE v3
 *
 * Principe "Fire & Forget" :
 * - Spawn avec transformation initiale
 * - Définit IMMÉDIATEMENT la transformation finale
 * - Le CLIENT interpole à 60Hz+ (zéro saccade)
 * - UN SEUL scheduler async pour le cleanup
 *
 * Avantages :
 * - 2 packets seulement (spawn + transformation finale) au lieu de 20-30
 * - Animation 100% client-side = parfaitement fluide
 * - Charge serveur minimale
 * - Compatible 200 joueurs
 *
 * @author Rinaorc Studio
 */
public class FluidDamageIndicator {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.#");

    // ========================================================================
    // CONFIGURATION
    // ========================================================================

    // Durées en ticks (20 ticks = 1 seconde)
    private static final int NORMAL_DURATION_TICKS = 22;      // 1.1s
    private static final int CRITICAL_DURATION_TICKS = 28;    // 1.4s
    private static final int HEAL_DURATION_TICKS = 18;        // 0.9s
    private static final int STATUS_DURATION_TICKS = 21;      // 1.05s
    private static final int HEADSHOT_DURATION_TICKS = 24;    // 1.2s
    private static final int COMBO_DURATION_TICKS = 20;       // 1s
    private static final int DANCING_SWORD_DURATION_TICKS = 18; // 0.9s

    // Distances de montée (en blocs)
    private static final float NORMAL_RISE = 1.2f;
    private static final float HEAL_RISE = 0.5f;
    private static final float STATUS_RISE = 0.35f;
    private static final float HEADSHOT_RISE = 0.6f;
    private static final float COMBO_RISE = 0.4f;
    private static final float DANCING_SWORD_RISE = 0.9f;

    // Échelles
    private static final float BASE_SCALE = 1.0f;
    private static final float CRITICAL_SCALE = 1.5f;
    private static final float HEAL_SCALE = 0.95f;
    private static final float STATUS_SCALE = 0.9f;
    private static final float HEADSHOT_SCALE = 1.35f;
    private static final float DANCING_SWORD_SCALE = 0.95f;

    // View distance pour culling
    private static final double VIEW_DISTANCE_SQUARED = 24.0 * 24.0; // 24 blocs

    // ========================================================================
    // SYSTÈME ANTI-STACK
    // ========================================================================

    private static final Map<UUID, Deque<IndicatorSlot>> recentIndicators = new ConcurrentHashMap<>();
    private static final int MAX_TRACKED_INDICATORS = 10;
    private static final long INDICATOR_TRACKING_TIME_MS = 1200;

    // Spirale dorée pour distribution optimale
    private static final double GOLDEN_ANGLE = 137.5077640500378; // degrés
    private static final double[] RADII = {0.25, 0.35, 0.45, 0.55, 0.40, 0.30, 0.50, 0.38, 0.48, 0.33};

    private record IndicatorSlot(double offsetX, double offsetZ, double offsetY, long timestamp) {}

    // ========================================================================
    // MÉTHODES PUBLIQUES - DÉGÂTS
    // ========================================================================

    /**
     * Affiche un indicateur de dégâts ultra-fluide
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical, Player viewer) {
        if (location.getWorld() == null) return;
        if (viewer != null && viewer.getLocation().distanceSquared(location) > VIEW_DISTANCE_SQUARED) return;

        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 1.3 + offset.getY(), offset.getZ());

        Component text = formatDamageText(damage, critical);
        float targetScale = critical ? CRITICAL_SCALE : BASE_SCALE;
        float rise = NORMAL_RISE;
        int duration = critical ? CRITICAL_DURATION_TICKS : NORMAL_DURATION_TICKS;

        spawnFluidIndicator(plugin, spawnLoc, text, targetScale, rise, duration, viewer, critical);
    }

    /**
     * Version legacy compatible
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical) {
        display(plugin, location, damage, critical, findNearestPlayer(location));
    }

    // ========================================================================
    // MÉTHODES PUBLIQUES - SOIN
    // ========================================================================

    /**
     * Affiche un indicateur de soin
     */
    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount, Player viewer) {
        if (location.getWorld() == null) return;
        if (viewer != null && viewer.getLocation().distanceSquared(location) > VIEW_DISTANCE_SQUARED) return;

        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 1.3 + offset.getY(), offset.getZ());

        Component text = Component.text("+" + FORMAT.format(amount) + " ", NamedTextColor.GREEN)
            .append(Component.text("❤", NamedTextColor.RED));

        spawnFluidIndicator(plugin, spawnLoc, text, HEAL_SCALE, HEAL_RISE, HEAL_DURATION_TICKS, viewer, false);
    }

    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount) {
        displayHeal(plugin, location, amount, findNearestPlayer(location));
    }

    // ========================================================================
    // MÉTHODES PUBLIQUES - STATUTS
    // ========================================================================

    /**
     * Affiche un indicateur d'esquive
     */
    public static void displayDodge(ZombieZPlugin plugin, Location location, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.5, 0);
        Component text = Component.text("ESQUIVE!", NamedTextColor.YELLOW, TextDecoration.BOLD, TextDecoration.ITALIC);

        spawnFluidIndicator(plugin, spawnLoc, text, STATUS_SCALE, STATUS_RISE, STATUS_DURATION_TICKS, viewer, false);
    }

    public static void displayDodge(ZombieZPlugin plugin, Location location) {
        displayDodge(plugin, location, findNearestPlayer(location));
    }

    /**
     * Affiche un indicateur de bloc
     */
    public static void displayBlock(ZombieZPlugin plugin, Location location, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.5, 0);
        Component text = Component.text("🛡 BLOQUÉ!", TextColor.color(0x5599FF), TextDecoration.BOLD);

        spawnFluidIndicator(plugin, spawnLoc, text, STATUS_SCALE, STATUS_RISE, STATUS_DURATION_TICKS, viewer, false);
    }

    public static void displayBlock(ZombieZPlugin plugin, Location location) {
        displayBlock(plugin, location, findNearestPlayer(location));
    }

    /**
     * Affiche un indicateur d'immunité
     */
    public static void displayImmune(ZombieZPlugin plugin, Location location, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.5, 0);
        Component text = Component.text("IMMUNISÉ", NamedTextColor.GRAY, TextDecoration.ITALIC);

        spawnFluidIndicator(plugin, spawnLoc, text, STATUS_SCALE * 0.9f, STATUS_RISE, STATUS_DURATION_TICKS - 3, viewer, false);
    }

    // ========================================================================
    // MÉTHODES PUBLIQUES - HEADSHOT
    // ========================================================================

    /**
     * Affiche un indicateur de headshot
     */
    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage, Player viewer) {
        if (location.getWorld() == null) return;

        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 1.8 + offset.getY(), offset.getZ());

        String formattedDamage = FORMAT.format(damage);
        Component text = Component.text("⊕ ", TextColor.color(0xFF6600), TextDecoration.BOLD)
            .append(Component.text(formattedDamage, TextColor.color(0xFFAA00), TextDecoration.BOLD))
            .append(Component.text(" ⊕", TextColor.color(0xFF6600), TextDecoration.BOLD));

        spawnFluidIndicator(plugin, spawnLoc, text, HEADSHOT_SCALE, HEADSHOT_RISE, HEADSHOT_DURATION_TICKS, viewer, true);
    }

    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage) {
        displayHeadshot(plugin, location, damage, findNearestPlayer(location));
    }

    // ========================================================================
    // MÉTHODES PUBLIQUES - COMBO
    // ========================================================================

    /**
     * Affiche un indicateur de combo
     */
    public static void displayCombo(ZombieZPlugin plugin, Location location, int comboCount, Player viewer) {
        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.7, 0);

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
        float scale = Math.min(0.9f + comboCount * 0.025f, 1.6f);

        spawnFluidIndicator(plugin, spawnLoc, text, scale, COMBO_RISE, COMBO_DURATION_TICKS, viewer, false);
    }

    // ========================================================================
    // MÉTHODES PUBLIQUES - ÉPÉE DANSANTE
    // ========================================================================

    /**
     * Affiche un indicateur de dégâts pour l'Épée Dansante
     */
    public static void displayDancingSword(ZombieZPlugin plugin, Location location, double damage, Player viewer) {
        if (location.getWorld() == null) return;

        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX() + 0.3, 1.1 + offset.getY(), offset.getZ() + 0.3);

        String formattedDamage = FORMAT.format(damage);
        Component text = Component.text("⚔ ", TextColor.color(0x9932CC), TextDecoration.BOLD)
            .append(Component.text(formattedDamage, TextColor.color(0xBA55D3), TextDecoration.BOLD));

        spawnFluidIndicator(plugin, spawnLoc, text, DANCING_SWORD_SCALE, DANCING_SWORD_RISE, DANCING_SWORD_DURATION_TICKS, viewer, false);
    }

    // ========================================================================
    // CORE - SPAWN FLUIDE "FIRE & FORGET"
    // ========================================================================

    /**
     * Spawn un indicateur avec animation 100% client-side
     *
     * Principe "Fire & Forget" optimisé :
     * 1. Spawn avec transformation initiale
     * 2. Capture la référence et schedule les phases d'animation
     * 3. Le CLIENT interpole à 60Hz+ (zéro saccade)
     * 4. Cleanup automatique
     *
     * Animation en 2 phases :
     * - Phase 1 (75% durée) : Pop-in + montée fluide
     * - Phase 2 (25% durée) : Fin de montée + shrink/fade
     */
    private static void spawnFluidIndicator(ZombieZPlugin plugin, Location spawnLoc, Component text,
                                             float targetScale, float riseDistance, int durationTicks,
                                             Player viewer, boolean popEffect) {
        if (spawnLoc.getWorld() == null) return;

        // Calcul des scales pour l'effet
        final float initialScale = popEffect ? targetScale * 1.35f : targetScale * 0.55f;
        final float finalScale = targetScale * 0.25f;

        // Durations des phases
        final int phase1Duration = (int) (durationTicks * 0.72);
        final int phase2Duration = durationTicks - phase1Duration;

        // Spawn et capture de la référence
        TextDisplay display = spawnLoc.getWorld().spawn(spawnLoc, TextDisplay.class, td -> {
            // === Configuration de base ===
            td.text(text);
            td.setBillboard(Display.Billboard.CENTER);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setDefaultBackground(false);
            td.setShadowed(true);
            td.setSeeThrough(false);
            td.setPersistent(false);
            td.setInvulnerable(true);

            // === Visibilité limitée au viewer ===
            if (viewer != null) {
                td.setVisibleByDefault(false);
                viewer.showEntity(plugin, td);
            }

            // === Transformation initiale (apparition instantanée) ===
            td.setInterpolationDelay(-1); // Désactive l'interpolation pour le premier état
            td.setInterpolationDuration(0);
            td.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(initialScale, initialScale, initialScale),
                new AxisAngle4f(0, 0, 0, 1)
            ));
        });

        // === PHASE 1 : Montée fluide + normalisation du scale ===
        // Délai de 1 tick pour que le client reçoive le spawn avant l'animation
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;

            display.setInterpolationDelay(0);
            display.setInterpolationDuration(phase1Duration);
            display.setTransformation(new Transformation(
                new Vector3f(0, riseDistance * 0.82f, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(targetScale, targetScale, targetScale),
                new AxisAngle4f(0, 0, 0, 1)
            ));
        }, 1L);

        // === PHASE 2 : Fin de montée + shrink (fade out visuel) ===
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) return;

            display.setInterpolationDelay(0);
            display.setInterpolationDuration(phase2Duration);
            display.setTransformation(new Transformation(
                new Vector3f(0, riseDistance, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(finalScale, finalScale, finalScale),
                new AxisAngle4f(0, 0, 0, 1)
            ));
        }, 1L + phase1Duration);

        // === CLEANUP : Suppression après animation complète ===
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (display.isValid()) {
                display.remove();
            }
        }, durationTicks + 3);
    }

    // ========================================================================
    // UTILITAIRES
    // ========================================================================

    /**
     * Formate le texte des dégâts avec couleurs adaptées
     */
    private static Component formatDamageText(double damage, boolean critical) {
        String formatted = FORMAT.format(damage);

        if (critical) {
            return Component.text("✦ ", TextColor.color(0xFFD700), TextDecoration.BOLD)
                .append(Component.text(formatted, TextColor.color(0xFF4444), TextDecoration.BOLD))
                .append(Component.text(" ✦", TextColor.color(0xFFD700), TextDecoration.BOLD));
        } else if (damage >= 50000) {
            return Component.text("☠ ", TextColor.color(0x8B0000), TextDecoration.BOLD)
                .append(Component.text(formatted, TextColor.color(0x8B0000), TextDecoration.BOLD));
        } else if (damage >= 20000) {
            return Component.text(formatted, TextColor.color(0xAA0000), TextDecoration.BOLD);
        } else if (damage >= 10000) {
            return Component.text(formatted, TextColor.color(0xFF3333), TextDecoration.BOLD);
        } else if (damage >= 5000) {
            return Component.text(formatted, TextColor.color(0xFF5555));
        } else if (damage >= 2000) {
            return Component.text(formatted, TextColor.color(0xFFAA00));
        } else if (damage >= 500) {
            return Component.text(formatted, TextColor.color(0xFFFF55));
        } else if (damage >= 100) {
            return Component.text(formatted, NamedTextColor.WHITE);
        } else {
            return Component.text(formatted, NamedTextColor.GRAY);
        }
    }

    /**
     * Calcule un offset anti-stack avec spirale dorée
     */
    private static Vector calculateAntiStackOffset(Location location, Player viewer) {
        UUID key = viewer != null ? viewer.getUniqueId() :
                   (location.getWorld() != null ? location.getWorld().getUID() : UUID.randomUUID());

        long now = System.currentTimeMillis();

        // Nettoyage des vieux slots
        recentIndicators.computeIfPresent(key, (k, slots) -> {
            slots.removeIf(slot -> now - slot.timestamp() > INDICATOR_TRACKING_TIME_MS);
            return slots.isEmpty() ? null : slots;
        });

        Deque<IndicatorSlot> slots = recentIndicators.computeIfAbsent(key, k -> new LinkedList<>());

        // Spirale dorée pour distribution uniforme
        int index = slots.size();
        double angle = Math.toRadians(index * GOLDEN_ANGLE);
        double radius = RADII[index % RADII.length];

        // Petite variation aléatoire
        ThreadLocalRandom random = ThreadLocalRandom.current();
        angle += random.nextDouble(-0.15, 0.15);
        radius += random.nextDouble(-0.05, 0.05);

        double offsetX = Math.cos(angle) * radius;
        double offsetZ = Math.sin(angle) * radius;
        double offsetY = index * 0.12; // Léger décalage vertical

        // Enregistrer ce slot
        slots.addLast(new IndicatorSlot(offsetX, offsetZ, offsetY, now));
        while (slots.size() > MAX_TRACKED_INDICATORS) {
            slots.removeFirst();
        }

        return new Vector(offsetX, offsetY, offsetZ);
    }

    /**
     * Trouve le joueur le plus proche
     */
    private static Player findNearestPlayer(Location location) {
        if (location.getWorld() == null) return null;

        Player nearest = null;
        double nearestDist = VIEW_DISTANCE_SQUARED;

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
