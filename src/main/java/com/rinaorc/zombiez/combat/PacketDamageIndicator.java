package com.rinaorc.zombiez.combat;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.rinaorc.zombiez.ZombieZPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Système d'indicateurs de dégâts ultra-fluide via ProtocolLib
 *
 * Caractéristiques:
 * - 100% packets virtuels (aucune entité réelle créée)
 * - Animation complètement asynchrone (pas de load sur le main thread)
 * - Easing Cubic Out pour une décélération naturelle
 * - TEXT_DISPLAY virtuel (1.19.4+) pour une meilleure fluidité
 * - Culling intelligent à 16 blocs
 * - Interpolation côté client pour 60Hz+ de fluidité
 *
 * @author Rinaorc Studio
 */
public class PacketDamageIndicator {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.#");
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE - 100000);

    // Configuration d'animation
    private static final int ANIMATION_DURATION_TICKS = 20; // 1 seconde
    private static final int CRITICAL_DURATION_TICKS = 26; // 1.3 secondes
    private static final double TOTAL_RISE_DISTANCE = 1.2; // Blocs montés
    private static final double VIEW_DISTANCE = 16.0; // Rayon de visibilité

    // Scales
    private static final float BASE_SCALE = 1.0f;
    private static final float CRITICAL_SCALE = 1.4f;
    private static final float HEAL_SCALE = 0.9f;

    // Système anti-stack
    private static final Map<UUID, Deque<IndicatorSlot>> recentIndicators = new ConcurrentHashMap<>();
    private static final int MAX_TRACKED_INDICATORS = 8;
    private static final long INDICATOR_TRACKING_TIME_MS = 1500;
    private static final double[] SPIRAL_ANGLES = {0, 72, 144, 216, 288, 36, 108, 180, 252, 324};
    private static final double[] SPIRAL_RADII = {0.3, 0.5, 0.7, 0.4, 0.6, 0.35, 0.55, 0.45, 0.65, 0.5};

    // ProtocolLib manager (cached)
    private static ProtocolManager protocolManager;

    // Cache de disponibilité de ProtocolLib
    private static Boolean protocolLibAvailable = null;

    private record IndicatorSlot(double offsetX, double offsetZ, double offsetY, long timestamp) {}

    /**
     * Vérifie si ProtocolLib est disponible
     */
    public static boolean isProtocolLibAvailable() {
        if (protocolLibAvailable == null) {
            try {
                protocolLibAvailable = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null
                    && Bukkit.getPluginManager().getPlugin("ProtocolLib").isEnabled();
                if (protocolLibAvailable) {
                    protocolManager = ProtocolLibrary.getProtocolManager();
                }
            } catch (Exception e) {
                protocolLibAvailable = false;
            }
        }
        return protocolLibAvailable;
    }

    /**
     * Affiche un indicateur de dégâts virtuel ultra-fluide
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical, Player viewer) {
        if (!isProtocolLibAvailable()) {
            // Fallback vers l'ancien système si ProtocolLib n'est pas disponible
            DamageIndicator.display(plugin, location, damage, critical, viewer);
            return;
        }

        if (location.getWorld() == null) return;

        // Calculer l'offset anti-stack
        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 1.2 + offset.getY(), offset.getZ());

        // Générer un ID d'entité unique
        int entityId = ENTITY_ID_COUNTER.getAndDecrement();
        if (entityId < Integer.MAX_VALUE - 200000) {
            ENTITY_ID_COUNTER.set(Integer.MAX_VALUE - 100000);
        }

        // UUID unique pour l'entité virtuelle
        UUID entityUuid = UUID.randomUUID();

        // Composant de texte formaté
        Component text = formatDamageComponent(damage, critical);

        // Configuration
        float scale = critical ? CRITICAL_SCALE : BASE_SCALE;
        int duration = critical ? CRITICAL_DURATION_TICKS : ANIMATION_DURATION_TICKS;

        // Trouver les joueurs dans le rayon de visibilité
        List<Player> viewers = getPlayersInRange(spawnLoc, viewer);
        if (viewers.isEmpty()) return;

        // Envoyer le packet de spawn initial
        spawnVirtualTextDisplay(entityId, entityUuid, spawnLoc, text, scale * 0.8f, viewers);

        // Animation asynchrone
        animateAsync(plugin, entityId, spawnLoc, scale, duration, critical, viewers);
    }

    /**
     * Version legacy compatible
     */
    public static void display(ZombieZPlugin plugin, Location location, double damage, boolean critical) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        display(plugin, location, damage, critical, nearestPlayer);
    }

    /**
     * Affiche un indicateur de soin virtuel
     */
    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount, Player viewer) {
        if (!isProtocolLibAvailable()) {
            DamageIndicator.displayHeal(plugin, location, amount, viewer);
            return;
        }

        if (location.getWorld() == null) return;

        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 1.2 + offset.getY(), offset.getZ());

        int entityId = ENTITY_ID_COUNTER.getAndDecrement();
        UUID entityUuid = UUID.randomUUID();

        Component text = Component.text("+" + FORMAT.format(amount) + " ", NamedTextColor.GREEN)
            .append(Component.text("❤", NamedTextColor.RED));

        List<Player> viewers = getPlayersInRange(spawnLoc, viewer);
        if (viewers.isEmpty()) return;

        spawnVirtualTextDisplay(entityId, entityUuid, spawnLoc, text, HEAL_SCALE * 0.75f, viewers);
        animateHealAsync(plugin, entityId, spawnLoc, viewers);
    }

    public static void displayHeal(ZombieZPlugin plugin, Location location, double amount) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayHeal(plugin, location, amount, nearestPlayer);
    }

    /**
     * Affiche un indicateur d'esquive virtuel
     */
    public static void displayDodge(ZombieZPlugin plugin, Location location, Player viewer) {
        if (!isProtocolLibAvailable()) {
            DamageIndicator.displayDodge(plugin, location, viewer);
            return;
        }

        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.4, 0);
        int entityId = ENTITY_ID_COUNTER.getAndDecrement();
        UUID entityUuid = UUID.randomUUID();

        Component text = Component.text("ESQUIVE!", NamedTextColor.YELLOW, TextDecoration.BOLD, TextDecoration.ITALIC);

        List<Player> viewers = getPlayersInRange(spawnLoc, viewer);
        if (viewers.isEmpty()) return;

        spawnVirtualTextDisplay(entityId, entityUuid, spawnLoc, text, 0.5f, viewers);
        animateStatusAsync(plugin, entityId, spawnLoc, 21, viewers);
    }

    public static void displayDodge(ZombieZPlugin plugin, Location location) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayDodge(plugin, location, nearestPlayer);
    }

    /**
     * Affiche un indicateur de headshot virtuel
     */
    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage, Player viewer) {
        if (!isProtocolLibAvailable()) {
            DamageIndicator.displayHeadshot(plugin, location, damage, viewer);
            return;
        }

        if (location.getWorld() == null) return;

        Vector offset = calculateAntiStackOffset(location, viewer);
        Location spawnLoc = location.clone().add(offset.getX(), 0.3 + offset.getY(), offset.getZ());

        int entityId = ENTITY_ID_COUNTER.getAndDecrement();
        UUID entityUuid = UUID.randomUUID();

        String formattedDamage = FORMAT.format(damage);
        Component text = Component.text("⊕ ", TextColor.color(0xFF6600), TextDecoration.BOLD)
            .append(Component.text(formattedDamage, TextColor.color(0xFFAA00), TextDecoration.BOLD))
            .append(Component.text(" ⊕", TextColor.color(0xFF6600), TextDecoration.BOLD));

        List<Player> viewers = getPlayersInRange(spawnLoc, viewer);
        if (viewers.isEmpty()) return;

        spawnVirtualTextDisplay(entityId, entityUuid, spawnLoc, text, 1.3f * 0.7f, viewers);
        animateHeadshotAsync(plugin, entityId, spawnLoc, 1.3f, viewers);
    }

    public static void displayHeadshot(ZombieZPlugin plugin, Location location, double damage) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayHeadshot(plugin, location, damage, nearestPlayer);
    }

    /**
     * Affiche un indicateur de bloc virtuel
     */
    public static void displayBlock(ZombieZPlugin plugin, Location location, Player viewer) {
        if (!isProtocolLibAvailable()) {
            DamageIndicator.displayBlock(plugin, location, viewer);
            return;
        }

        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.4, 0);
        int entityId = ENTITY_ID_COUNTER.getAndDecrement();
        UUID entityUuid = UUID.randomUUID();

        Component text = Component.text("BLOQUÉ!", TextColor.color(0x5555FF), TextDecoration.BOLD);

        List<Player> viewers = getPlayersInRange(spawnLoc, viewer);
        if (viewers.isEmpty()) return;

        spawnVirtualTextDisplay(entityId, entityUuid, spawnLoc, text, 0.5f, viewers);
        animateStatusAsync(plugin, entityId, spawnLoc, 21, viewers);
    }

    public static void displayBlock(ZombieZPlugin plugin, Location location) {
        Player nearestPlayer = findNearestPlayer(location, 50);
        displayBlock(plugin, location, nearestPlayer);
    }

    /**
     * Affiche un indicateur d'immunité virtuel
     */
    public static void displayImmune(ZombieZPlugin plugin, Location location, Player viewer) {
        if (!isProtocolLibAvailable()) {
            DamageIndicator.displayImmune(plugin, location, viewer);
            return;
        }

        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.4, 0);
        int entityId = ENTITY_ID_COUNTER.getAndDecrement();
        UUID entityUuid = UUID.randomUUID();

        Component text = Component.text("IMMUNISÉ", NamedTextColor.GRAY, TextDecoration.ITALIC);

        List<Player> viewers = getPlayersInRange(spawnLoc, viewer);
        if (viewers.isEmpty()) return;

        spawnVirtualTextDisplay(entityId, entityUuid, spawnLoc, text, 0.5f, viewers);
        animateStatusAsync(plugin, entityId, spawnLoc, 18, viewers);
    }

    /**
     * Affiche un indicateur de combo virtuel
     */
    public static void displayCombo(ZombieZPlugin plugin, Location location, int comboCount, Player viewer) {
        if (!isProtocolLibAvailable()) {
            DamageIndicator.displayCombo(plugin, location, comboCount, viewer);
            return;
        }

        if (location.getWorld() == null) return;

        Location spawnLoc = location.clone().add(0, 1.6, 0);
        int entityId = ENTITY_ID_COUNTER.getAndDecrement();
        UUID entityUuid = UUID.randomUUID();

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

        List<Player> viewers = getPlayersInRange(spawnLoc, viewer);
        if (viewers.isEmpty()) return;

        spawnVirtualTextDisplay(entityId, entityUuid, spawnLoc, text, scale * 0.6f, viewers);
        animateComboAsync(plugin, entityId, spawnLoc, scale, viewers);
    }

    // ========================================================================
    // PACKET METHODS - Manipulation directe des packets ProtocolLib
    // ========================================================================

    /**
     * Spawn un TextDisplay virtuel via packets
     */
    private static void spawnVirtualTextDisplay(int entityId, UUID entityUuid, Location location,
                                                  Component text, float scale, List<Player> viewers) {
        try {
            // Packet de spawn d'entité (PacketPlayOutSpawnEntity)
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawnPacket.getIntegers().write(0, entityId); // Entity ID
            spawnPacket.getUUIDs().write(0, entityUuid); // UUID
            spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY); // Type
            spawnPacket.getDoubles()
                .write(0, location.getX())  // X
                .write(1, location.getY())  // Y
                .write(2, location.getZ()); // Z
            spawnPacket.getBytes()
                .write(0, (byte) 0)  // Yaw
                .write(1, (byte) 0)  // Pitch
                .write(2, (byte) 0); // Head Yaw
            spawnPacket.getIntegers().write(1, 0); // Data
            spawnPacket.getShorts()
                .write(0, (short) 0)  // Velocity X
                .write(1, (short) 0)  // Velocity Y
                .write(2, (short) 0); // Velocity Z

            // Packet de metadata (PacketPlayOutEntityMetadata)
            PacketContainer metadataPacket = createMetadataPacket(entityId, text, scale);

            // Envoyer les packets à tous les viewers
            for (Player player : viewers) {
                if (player.isOnline()) {
                    protocolManager.sendServerPacket(player, spawnPacket);
                    protocolManager.sendServerPacket(player, metadataPacket);
                }
            }
        } catch (Exception e) {
            ZombieZPlugin.getInstance().getLogger().warning("Erreur lors du spawn du TextDisplay virtuel: " + e.getMessage());
        }
    }

    /**
     * Crée le packet de metadata pour le TextDisplay
     */
    private static PacketContainer createMetadataPacket(int entityId, Component text, float scale) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entityId);

        List<WrappedDataValue> dataValues = new ArrayList<>();

        // Index 0: Flags d'entité (invisible = false, glowing = false, etc.)
        dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));

        // Index 8: Billboard mode (2 = CENTER - face toujours le joueur)
        dataValues.add(new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), (byte) 3));

        // Index 11: View range multiplier
        dataValues.add(new WrappedDataValue(17, WrappedDataWatcher.Registry.get(Float.class), 1.0f));

        // Index 12: Shadow radius
        dataValues.add(new WrappedDataValue(18, WrappedDataWatcher.Registry.get(Float.class), 0.0f));

        // Index 13: Shadow strength
        dataValues.add(new WrappedDataValue(19, WrappedDataWatcher.Registry.get(Float.class), 0.0f));

        // Index 14: Width (pour TextDisplay)
        dataValues.add(new WrappedDataValue(20, WrappedDataWatcher.Registry.get(Float.class), 0.0f));

        // Index 15: Height
        dataValues.add(new WrappedDataValue(21, WrappedDataWatcher.Registry.get(Float.class), 0.0f));

        // Index 22: Text content - Utiliser WrappedChatComponent pour le texte JSON
        String jsonText = GsonComponentSerializer.gson().serialize(text);
        dataValues.add(new WrappedDataValue(23, WrappedDataWatcher.Registry.getChatComponentSerializer(false),
            Optional.of(WrappedChatComponent.fromJson(jsonText).getHandle())));

        // Index 23: Line width
        dataValues.add(new WrappedDataValue(24, WrappedDataWatcher.Registry.get(Integer.class), 200));

        // Index 24: Background color (ARGB) - transparent
        dataValues.add(new WrappedDataValue(25, WrappedDataWatcher.Registry.get(Integer.class), 0));

        // Index 25: Text opacity (255 = opaque)
        dataValues.add(new WrappedDataValue(26, WrappedDataWatcher.Registry.get(Byte.class), (byte) -1));

        // Index 26: Flags (has shadow = true, see through = false, default background = false, alignment = center)
        dataValues.add(new WrappedDataValue(27, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x01));

        // Scale transformation via Quaternion et Vector3f
        // Index 11: Translation
        dataValues.add(new WrappedDataValue(11, WrappedDataWatcher.Registry.get(Vector3f.class), new Vector3f(0, 0, 0)));

        // Index 12: Scale
        dataValues.add(new WrappedDataValue(12, WrappedDataWatcher.Registry.get(Vector3f.class), new Vector3f(scale, scale, scale)));

        // Index 13: Left rotation (quaternion)
        dataValues.add(new WrappedDataValue(13, WrappedDataWatcher.Registry.get(Quaternionf.class), new Quaternionf(0, 0, 0, 1)));

        // Index 14: Right rotation (quaternion)
        dataValues.add(new WrappedDataValue(14, WrappedDataWatcher.Registry.get(Quaternionf.class), new Quaternionf(0, 0, 0, 1)));

        packet.getDataValueCollectionModifier().write(0, dataValues);

        return packet;
    }

    /**
     * Met à jour la position et le scale via packets (mouvement relatif)
     */
    private static void updatePosition(int entityId, Location newLocation, float scale, List<Player> viewers) {
        try {
            // Téléportation pour des mouvements précis
            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles()
                .write(0, newLocation.getX())
                .write(1, newLocation.getY())
                .write(2, newLocation.getZ());
            teleportPacket.getBytes()
                .write(0, (byte) 0)  // Yaw
                .write(1, (byte) 0); // Pitch
            teleportPacket.getBooleans().write(0, false); // On ground

            // Mise à jour du scale si nécessaire
            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);

            List<WrappedDataValue> dataValues = new ArrayList<>();
            dataValues.add(new WrappedDataValue(12, WrappedDataWatcher.Registry.get(Vector3f.class),
                new Vector3f(scale, scale, scale)));
            metadataPacket.getDataValueCollectionModifier().write(0, dataValues);

            for (Player player : viewers) {
                if (player.isOnline()) {
                    protocolManager.sendServerPacket(player, teleportPacket);
                    protocolManager.sendServerPacket(player, metadataPacket);
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs de mise à jour de position
        }
    }

    /**
     * Détruit l'entité virtuelle via packet
     */
    private static void destroyEntity(int entityId, List<Player> viewers) {
        try {
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntLists().write(0, List.of(entityId));

            for (Player player : viewers) {
                if (player.isOnline()) {
                    protocolManager.sendServerPacket(player, destroyPacket);
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs de destruction
        }
    }

    // ========================================================================
    // ANIMATION METHODS - 100% Asynchrone avec Easing
    // ========================================================================

    /**
     * Animation principale des dégâts - ASYNC
     * Utilise Cubic Out pour une décélération naturelle
     */
    private static void animateAsync(ZombieZPlugin plugin, int entityId, Location startLoc,
                                      float targetScale, int duration, boolean critical, List<Player> viewers) {
        new BukkitRunnable() {
            private int tick = 0;
            private final Location currentLoc = startLoc.clone();
            private final float startScale = critical ? targetScale * 1.3f : targetScale * 0.85f;

            @Override
            public void run() {
                if (tick >= duration || viewers.stream().noneMatch(Player::isOnline)) {
                    // Destruction de l'entité virtuelle
                    destroyEntity(entityId, viewers);
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;

                // Easing Cubic Out: 1 - (1 - t)^3
                float easedProgress = easeOutCubic(progress);

                // Calcul du déplacement Y avec easing
                double yOffset = easedProgress * TOTAL_RISE_DISTANCE;
                currentLoc.setY(startLoc.getY() + yOffset);

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

                // Mise à jour via packets
                updatePosition(entityId, currentLoc, currentScale, viewers);

                tick++;
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L); // Chaque tick en async
    }

    /**
     * Animation de soin - ASYNC
     */
    private static void animateHealAsync(ZombieZPlugin plugin, int entityId, Location startLoc, List<Player> viewers) {
        new BukkitRunnable() {
            private int tick = 0;
            private final int duration = 18;
            private final Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (tick >= duration || viewers.stream().noneMatch(Player::isOnline)) {
                    destroyEntity(entityId, viewers);
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;
                float easedProgress = easeOutCubic(progress);

                double yOffset = easedProgress * 0.4;
                currentLoc.setY(startLoc.getY() + yOffset);

                float scale;
                if (progress < 0.12f) {
                    scale = HEAL_SCALE * (0.8f + 0.35f * easeOutBack(progress / 0.12f));
                } else if (progress > 0.7f) {
                    float fadeProgress = (progress - 0.7f) / 0.3f;
                    scale = HEAL_SCALE * (1 - easeInCubic(fadeProgress) * 0.65f);
                } else {
                    scale = HEAL_SCALE;
                }

                updatePosition(entityId, currentLoc, scale, viewers);
                tick++;
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    /**
     * Animation de statut (esquive, bloc, immunité) - ASYNC
     */
    private static void animateStatusAsync(ZombieZPlugin plugin, int entityId, Location startLoc,
                                            int duration, List<Player> viewers) {
        new BukkitRunnable() {
            private int tick = 0;
            private final Location currentLoc = startLoc.clone();
            private final float baseScale = 0.85f;

            @Override
            public void run() {
                if (tick >= duration || viewers.stream().noneMatch(Player::isOnline)) {
                    destroyEntity(entityId, viewers);
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;
                float easedProgress = easeOutCubic(progress);

                double yOffset = easedProgress * 0.28;
                currentLoc.setY(startLoc.getY() + yOffset);

                float scale;
                if (progress < 0.1f) {
                    scale = baseScale * (0.5f + 0.7f * easeOutBack(progress / 0.1f));
                } else if (progress > 0.75f) {
                    float fadeProgress = (progress - 0.75f) / 0.25f;
                    scale = baseScale * (1 - easeInCubic(fadeProgress) * 0.75f);
                } else {
                    scale = baseScale;
                }

                updatePosition(entityId, currentLoc, scale, viewers);
                tick++;
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    /**
     * Animation de headshot - ASYNC
     */
    private static void animateHeadshotAsync(ZombieZPlugin plugin, int entityId, Location startLoc,
                                              float targetScale, List<Player> viewers) {
        new BukkitRunnable() {
            private int tick = 0;
            private final int duration = 24;
            private final Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (tick >= duration || viewers.stream().noneMatch(Player::isOnline)) {
                    destroyEntity(entityId, viewers);
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;
                float easedProgress = easeOutCubic(progress);

                double yOffset = easedProgress * 0.45;
                currentLoc.setY(startLoc.getY() + yOffset);

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

                updatePosition(entityId, currentLoc, scale, viewers);
                tick++;
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    /**
     * Animation de combo - ASYNC
     */
    private static void animateComboAsync(ZombieZPlugin plugin, int entityId, Location startLoc,
                                           float targetScale, List<Player> viewers) {
        float reducedScale = targetScale / 1.4f;

        new BukkitRunnable() {
            private int tick = 0;
            private final int duration = 20;
            private final Location currentLoc = startLoc.clone();

            @Override
            public void run() {
                if (tick >= duration || viewers.stream().noneMatch(Player::isOnline)) {
                    destroyEntity(entityId, viewers);
                    cancel();
                    return;
                }

                float progress = (float) tick / duration;
                float easedProgress = easeOutCubic(progress);

                double yOffset = easedProgress * 0.3;
                currentLoc.setY(startLoc.getY() + yOffset);

                float scale;
                if (progress < 0.12f) {
                    scale = reducedScale * (0.8f + 0.35f * easeOutBack(progress / 0.12f));
                } else if (progress > 0.7f) {
                    float fadeProgress = (progress - 0.7f) / 0.3f;
                    scale = reducedScale * (1 - easeInCubic(fadeProgress) * 0.65f);
                } else {
                    scale = reducedScale;
                }

                updatePosition(entityId, currentLoc, scale, viewers);
                tick++;
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 1L);
    }

    // ========================================================================
    // EASING FUNCTIONS - Courbes d'animation fluides
    // ========================================================================

    /**
     * Cubic Out - Décélération naturelle (vitesse élevée au début, lente à la fin)
     * Formule: 1 - (1 - t)^3
     */
    private static float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    /**
     * Cubic In - Accélération progressive
     */
    private static float easeInCubic(float t) {
        return t * t * t;
    }

    /**
     * Quad Out - Décélération plus douce
     */
    private static float easeOutQuad(float t) {
        return 1 - (1 - t) * (1 - t);
    }

    /**
     * Back Out - Effet de dépassement puis retour (overshoot)
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
     * Formate le texte des dégâts avec couleurs appropriées
     */
    private static Component formatDamageComponent(double damage, boolean critical) {
        String formattedDamage = FORMAT.format(damage);

        if (critical) {
            return Component.text("✦ ", TextColor.color(0xFFD700), TextDecoration.BOLD)
                .append(Component.text(formattedDamage, TextColor.color(0xFF4444), TextDecoration.BOLD))
                .append(Component.text(" ✦", TextColor.color(0xFFD700), TextDecoration.BOLD));
        } else if (damage >= 50000) {
            return Component.text("☠ ", TextColor.color(0x8B0000), TextDecoration.BOLD)
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
     * Obtient les joueurs dans le rayon de visibilité
     */
    private static List<Player> getPlayersInRange(Location location, Player primaryViewer) {
        if (location.getWorld() == null) return Collections.emptyList();

        List<Player> viewers = new ArrayList<>();
        double viewDistSq = VIEW_DISTANCE * VIEW_DISTANCE;

        // Si un viewer principal est défini, on ne montre qu'à lui s'il est dans le range
        if (primaryViewer != null) {
            if (primaryViewer.isOnline() &&
                primaryViewer.getWorld().equals(location.getWorld()) &&
                primaryViewer.getLocation().distanceSquared(location) <= viewDistSq) {
                viewers.add(primaryViewer);
            }
        } else {
            // Sinon, montrer à tous les joueurs dans le range
            for (Player player : location.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(location) <= viewDistSq) {
                    viewers.add(player);
                }
            }
        }

        return viewers;
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
