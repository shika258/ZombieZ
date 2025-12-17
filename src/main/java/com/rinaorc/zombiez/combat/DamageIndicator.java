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
 * Système d'affichage des dégâts flottants optimisé v2
 *
 * Améliorations de fluidité:
 * - Utilise l'interpolation NATIVE de Minecraft (client-side, 60Hz+)
 * - Mouvement via Translation au lieu de teleport() pour des animations lisses
 * - Mise à jour serveur tous les 3 ticks seulement (optimisé)
 * - Le client interpole automatiquement entre les états
 *
 * Caractéristiques:
 * - TextDisplay (plus léger que ArmorStand)
 * - Affichage client-side uniquement pour le joueur concerné
 * - Système anti-stack intelligent pour éviter les superpositions
 * - Fonctions d'easing variées (cubic, back) pour des animations satisfaisantes
 * - Coups critiques avec effets spéciaux et pop-in
 */
public class DamageIndicator {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.#");
    private static final Random RANDOM = new Random();

    // Configuration - Tailles réduites pour une meilleure lisibilité
    private static final int DISPLAY_DURATION_TICKS = 22; // 1.1 seconde
    private static final int CRITICAL_DURATION_TICKS = 28; // 1.4 secondes pour critiques
    private static final float BASE_SCALE = 1.0f;      // Taille réduite (÷1.5)
    private static final float CRITICAL_SCALE = 1.4f;  // Taille réduite (÷1.5)
    private static final float HEAL_SCALE = 0.9f;      // Taille réduite (÷1.5)

    // Animation optimisée - mise à jour tous les 3 ticks, interpolation native pour fluidité
    private static final int ANIMATION_INTERVAL_TICKS = 3;
    // Durée d'interpolation native (client-side, très fluide)
    private static final int INTERPOLATION_TICKS = 4;

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
                new Vector3f(0, 0, 0), // Commence à Y=0, le mouvement se fait via translation
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(initialScale, initialScale, initialScale),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            // Configurer l'interpolation native pour fluidité client-side
            display.setInterpolationDuration(INTERPOLATION_TICKS);
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
                new Vector3f(HEAL_SCALE * 0.75f, HEAL_SCALE * 0.75f, HEAL_SCALE * 0.75f),
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(INTERPOLATION_TICKS);
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
                new Vector3f(0.5f, 0.5f, 0.5f), // Petit pour pop-in effect
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateStatusIndicator(plugin, display, 21);
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
     * Affiche un indicateur de headshot avec dégâts
     */
    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage, Player viewer) {
        if (location.getWorld() == null) return;

        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 0.3 + offset.getY(), offset.getZ());

        location.getWorld().spawn(spawnLoc, TextDisplay.class, display -> {
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

            // Texte de headshot avec dégâts - couleur dorée/orange distinctive
            String formattedDamage = FORMAT.format(damage);
            Component text = Component.text("⊕ ", TextColor.color(0xFF6600), TextDecoration.BOLD)
                .append(Component.text(formattedDamage, TextColor.color(0xFFAA00), TextDecoration.BOLD))
                .append(Component.text(" ⊕", TextColor.color(0xFF6600), TextDecoration.BOLD));
            display.text(text);

            // Scale légèrement plus grand pour les headshots
            float scale = 1.3f;

            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(scale * 0.7f, scale * 0.7f, scale * 0.7f), // Petit pour pop-in
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateHeadshotIndicator(plugin, display, scale);
        });
    }

    /**
     * Affiche un indicateur de headshot (version legacy)
     */
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
                new Vector3f(0.5f, 0.5f, 0.5f), // Petit pour pop-in effect
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateStatusIndicator(plugin, display, 21);
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
                new Vector3f(0.5f, 0.5f, 0.5f), // Petit pour pop-in
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateStatusIndicator(plugin, display, 18);
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
                new Vector3f(scale * 0.6f, scale * 0.6f, scale * 0.6f), // Petit pour pop-in
                new AxisAngle4f(0, 0, 0, 1)
            ));

            display.setInterpolationDuration(INTERPOLATION_TICKS);
            display.setInterpolationDelay(0);

            if (viewer != null) {
                hideFromOtherPlayers(display, viewer);
            }

            animateComboIndicator(plugin, display, scale);
        });
    }

    /**
     * Formate le texte des dégâts en Component avec styles appropriés
     * Seuils adaptés au nouveau scaling de dégâts des items
     */
    private static Component formatDamageComponent(double damage, boolean critical) {
        String formattedDamage = FORMAT.format(damage);

        if (critical) {
            // Coup critique: gros, gras, rouge avec étoiles dorées
            return Component.text("✦ ", TextColor.color(0xFFD700), TextDecoration.BOLD)
                .append(Component.text(formattedDamage, TextColor.color(0xFF4444), TextDecoration.BOLD))
                .append(Component.text(" ✦", TextColor.color(0xFFD700), TextDecoration.BOLD));
        } else if (damage >= 50000) {
            // Dégâts apocalyptiques - Rouge foncé gras avec effet
            return Component.text("☠ ", TextColor.color(0x8B0000), TextDecoration.BOLD)
                .append(Component.text(formattedDamage, TextColor.color(0x8B0000), TextDecoration.BOLD));
        } else if (damage >= 20000) {
            // Dégâts dévastateurs - Rouge foncé gras
            return Component.text(formattedDamage, TextColor.color(0xAA0000), TextDecoration.BOLD);
        } else if (damage >= 10000) {
            // Dégâts massifs - Rouge gras
            return Component.text(formattedDamage, TextColor.color(0xFF3333), TextDecoration.BOLD);
        } else if (damage >= 5000) {
            // Très gros dégâts - Rouge
            return Component.text(formattedDamage, TextColor.color(0xFF5555));
        } else if (damage >= 2000) {
            // Gros dégâts - Orange
            return Component.text(formattedDamage, TextColor.color(0xFFAA00));
        } else if (damage >= 500) {
            // Dégâts moyens - Jaune
            return Component.text(formattedDamage, TextColor.color(0xFFFF55));
        } else if (damage >= 100) {
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
     * Utilise l'interpolation native de Minecraft pour une fluidité maximale
     * Mise à jour tous les 3 ticks, le client interpole entre les états
     */
    private static void animateIndicatorOptimized(ZombieZPlugin plugin, TextDisplay display, float targetScale, int duration, boolean critical) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final float startScale = critical ? targetScale * 1.3f : targetScale * 0.85f;
            private final int effectiveDuration = duration / ANIMATION_INTERVAL_TICKS;
            private final float totalRise = 0.55f; // Distance totale de montée

            @Override
            public void run() {
                if (ticks >= effectiveDuration || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / effectiveDuration;
                float nextProgress = (float) (ticks + 1) / effectiveDuration;

                // Calculer la position Y cible avec easing (pour le prochain frame)
                float easeOutNext = 1 - (float) Math.pow(1 - nextProgress, 2.5);
                float yOffset = easeOutNext * totalRise;

                // Animation de scale avec transitions fluides
                float currentScale;
                if (critical) {
                    // Critique: shrink fluide depuis grand vers normal
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
                    // Normal: grow puis shrink fluide
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

                // Appliquer la transformation avec translation (pas de teleport!)
                // L'interpolation native rend le mouvement fluide côté client
                display.setInterpolationDuration(INTERPOLATION_TICKS);
                display.setTransformation(new Transformation(
                    new Vector3f(0, yOffset, 0), // Translation Y pour le mouvement
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

    private static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private static float easeInCubic(float t) {
        return t * t * t;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    /**
     * Anime l'indicateur de soin avec interpolation native
     */
    private static void animateHealIndicator(ZombieZPlugin plugin, TextDisplay display) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int effectiveDuration = 18 / ANIMATION_INTERVAL_TICKS;
            private final float totalRise = 0.4f;

            @Override
            public void run() {
                if (ticks >= effectiveDuration || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / effectiveDuration;
                float nextProgress = Math.min(1f, (float) (ticks + 1) / effectiveDuration);

                // Mouvement vers le haut avec easing
                float yOffset = easeOutCubic(nextProgress) * totalRise;

                // Scale animation fluide avec léger "bounce"
                float scale;
                if (progress < 0.12f) {
                    scale = HEAL_SCALE * (0.8f + 0.35f * easeOutBack(progress / 0.12f));
                } else if (progress > 0.7f) {
                    float fadeProgress = (progress - 0.7f) / 0.3f;
                    scale = HEAL_SCALE * (1 - easeInCubic(fadeProgress) * 0.65f);
                } else {
                    scale = HEAL_SCALE;
                }

                display.setInterpolationDuration(INTERPOLATION_TICKS);
                display.setTransformation(new Transformation(
                    new Vector3f(0, yOffset, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, ANIMATION_INTERVAL_TICKS);
    }

    /**
     * Anime les indicateurs de statut (esquive, bloc, immunité) avec interpolation native
     */
    private static void animateStatusIndicator(ZombieZPlugin plugin, TextDisplay display, int duration) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int effectiveDuration = duration / ANIMATION_INTERVAL_TICKS;
            private final float baseScale = 0.85f;
            private final float totalRise = 0.28f;

            @Override
            public void run() {
                if (ticks >= effectiveDuration || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / effectiveDuration;
                float nextProgress = Math.min(1f, (float) (ticks + 1) / effectiveDuration);

                // Mouvement vers le haut fluide avec interpolation
                float yOffset = easeOutCubic(nextProgress) * totalRise;

                // Scale avec pop-in satisfaisant
                float scale;
                if (progress < 0.1f) {
                    scale = baseScale * (0.5f + 0.7f * easeOutBack(progress / 0.1f));
                } else if (progress > 0.75f) {
                    float fadeProgress = (progress - 0.75f) / 0.25f;
                    scale = baseScale * (1 - easeInCubic(fadeProgress) * 0.75f);
                } else {
                    scale = baseScale;
                }

                display.setInterpolationDuration(INTERPOLATION_TICKS);
                display.setTransformation(new Transformation(
                    new Vector3f(0, yOffset, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, ANIMATION_INTERVAL_TICKS);
    }

    /**
     * Anime l'indicateur de combo avec interpolation native
     */
    private static void animateComboIndicator(ZombieZPlugin plugin, TextDisplay display, float targetScale) {
        float reducedScale = targetScale / 1.4f;

        new BukkitRunnable() {
            private int ticks = 0;
            private final int effectiveDuration = 20 / ANIMATION_INTERVAL_TICKS;
            private final float totalRise = 0.3f;

            @Override
            public void run() {
                if (ticks >= effectiveDuration || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / effectiveDuration;
                float nextProgress = Math.min(1f, (float) (ticks + 1) / effectiveDuration);

                // Mouvement vers le haut avec interpolation
                float yOffset = easeOutCubic(nextProgress) * totalRise;

                // Scale fluide avec pop-in
                float scale;
                if (progress < 0.12f) {
                    scale = reducedScale * (0.8f + 0.35f * easeOutBack(progress / 0.12f));
                } else if (progress > 0.7f) {
                    float fadeProgress = (progress - 0.7f) / 0.3f;
                    scale = reducedScale * (1 - easeInCubic(fadeProgress) * 0.65f);
                } else {
                    scale = reducedScale;
                }

                display.setInterpolationDuration(INTERPOLATION_TICKS);
                display.setTransformation(new Transformation(
                    new Vector3f(0, yOffset, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0, 0, 0, 1)
                ));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, ANIMATION_INTERVAL_TICKS);
    }

    /**
     * Anime l'indicateur de headshot avec interpolation native - effet pop satisfaisant
     */
    private static void animateHeadshotIndicator(ZombieZPlugin plugin, TextDisplay display, float targetScale) {
        new BukkitRunnable() {
            private int ticks = 0;
            private final int effectiveDuration = 24 / ANIMATION_INTERVAL_TICKS;
            private final float totalRise = 0.45f;

            @Override
            public void run() {
                if (ticks >= effectiveDuration || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }

                float progress = (float) ticks / effectiveDuration;
                float nextProgress = Math.min(1f, (float) (ticks + 1) / effectiveDuration);

                // Mouvement vers le haut avec interpolation fluide
                float yOffset = easeOutCubic(nextProgress) * totalRise;

                // Animation pop-in exagérée puis shrink pour effet satisfaisant
                float scale;
                if (progress < 0.08f) {
                    // Pop-in rapide et exagéré avec overshoot
                    float popProgress = progress / 0.08f;
                    scale = targetScale * (0.7f + 0.5f * easeOutBack(popProgress));
                } else if (progress < 0.18f) {
                    // Retour à la normale
                    float settleProgress = (progress - 0.08f) / 0.1f;
                    scale = targetScale * (1.2f - 0.2f * easeOutCubic(settleProgress));
                } else if (progress > 0.68f) {
                    // Fade-out progressif
                    float fadeProgress = (progress - 0.68f) / 0.32f;
                    scale = targetScale * (1 - easeInCubic(fadeProgress) * 0.55f);
                } else {
                    scale = targetScale;
                }

                display.setInterpolationDuration(INTERPOLATION_TICKS);
                display.setTransformation(new Transformation(
                    new Vector3f(0, yOffset, 0),
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
