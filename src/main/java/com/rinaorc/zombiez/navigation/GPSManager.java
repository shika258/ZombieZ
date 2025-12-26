package com.rinaorc.zombiez.navigation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyManager;
import com.rinaorc.zombiez.progression.journey.JourneyStep;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Système GPS pour guider les joueurs vers les objectifs du Journey
 *
 * Affiche une flèche fluide en armor stands client-side devant le joueur,
 * pointant vers la destination de l'étape actuelle.
 *
 * Fonctionnalités:
 * - Flèche ultra-fluide (mise à jour chaque tick)
 * - Entités client-side via ProtocolLib (pas d'impact serveur)
 * - Interpolation de position pour éviter les saccades
 * - Toggle via /gps
 */
public class GPSManager implements Listener {

    private final ZombieZPlugin plugin;
    private final ProtocolManager protocolManager;
    private final JourneyManager journeyManager;

    // Joueurs avec GPS actif
    private final Set<UUID> activeGPS = ConcurrentHashMap.newKeySet();

    // Entity IDs virtuels pour chaque joueur (pour les packets)
    private final Map<UUID, int[]> playerEntityIds = new ConcurrentHashMap<>();

    // Générateur d'IDs d'entités (commence haut pour éviter conflits)
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE - 100000);

    // Pattern pour extraire les coordonnées des descriptions d'étapes
    // Supporte: "§b625, 93, 9853" ou "§b~345, ~86, ~9500"
    private static final Pattern COORD_PATTERN = Pattern.compile("§b~?(\\d+),\\s*~?(\\d+),\\s*~?(\\d+)");

    // Configuration de la flèche
    private static final int ARROW_PARTS = 7;          // Nombre de points de la flèche
    private static final double ARROW_DISTANCE = 2.5;  // Distance devant le joueur
    private static final double ARROW_HEIGHT = 1.2;    // Hauteur au niveau des yeux
    private static final double ARROW_SCALE = 0.15;    // Taille des points

    // Tâche de mise à jour
    private BukkitTask updateTask;

    // Positions interpolées pour chaque joueur
    private final Map<UUID, ArrowState> arrowStates = new ConcurrentHashMap<>();

    public GPSManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.journeyManager = plugin.getJourneyManager();

        // Démarrer la tâche de mise à jour fluide
        startUpdateTask();
    }

    /**
     * Active/Désactive le GPS pour un joueur
     * @return true si le GPS est maintenant actif
     */
    public boolean toggleGPS(Player player) {
        UUID uuid = player.getUniqueId();

        if (activeGPS.contains(uuid)) {
            // Désactiver
            disableGPS(player);
            return false;
        } else {
            // Activer
            return enableGPS(player);
        }
    }

    /**
     * Active le GPS pour un joueur
     */
    public boolean enableGPS(Player player) {
        UUID uuid = player.getUniqueId();

        // Vérifier si l'étape actuelle a des coordonnées
        Location destination = getDestinationForPlayer(player);
        if (destination == null) {
            player.sendMessage("§c§l✗ §7Aucune destination disponible pour cette étape.");
            player.sendMessage("§8Le GPS fonctionne uniquement pour les étapes avec des coordonnées.");
            return false;
        }

        // Créer les entity IDs virtuels
        int[] entityIds = new int[ARROW_PARTS];
        for (int i = 0; i < ARROW_PARTS; i++) {
            entityIds[i] = ENTITY_ID_COUNTER.getAndDecrement();
        }
        playerEntityIds.put(uuid, entityIds);

        // Initialiser l'état de la flèche
        arrowStates.put(uuid, new ArrowState(player.getLocation(), destination));

        // Spawn initial des armor stands
        spawnArrowEntities(player);

        activeGPS.add(uuid);

        player.sendMessage("§a§l✓ §eGPS activé! §7Suis la flèche...");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.5f);

        return true;
    }

    /**
     * Désactive le GPS pour un joueur
     */
    public void disableGPS(Player player) {
        UUID uuid = player.getUniqueId();

        // Détruire les entités client-side
        destroyArrowEntities(player);

        activeGPS.remove(uuid);
        playerEntityIds.remove(uuid);
        arrowStates.remove(uuid);

        player.sendMessage("§c§l✗ §7GPS désactivé.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
    }

    /**
     * Vérifie si le GPS est actif pour un joueur
     */
    public boolean isGPSActive(Player player) {
        return activeGPS.contains(player.getUniqueId());
    }

    /**
     * Obtient la destination de l'étape actuelle du joueur
     */
    public Location getDestinationForPlayer(Player player) {
        JourneyStep step = journeyManager.getCurrentStep(player);
        if (step == null) return null;

        return parseCoordinates(step.getDescription(), player.getWorld().getName());
    }

    /**
     * Parse les coordonnées depuis une description d'étape
     */
    private Location parseCoordinates(String description, String worldName) {
        Matcher matcher = COORD_PATTERN.matcher(description);
        if (!matcher.find()) return null;

        try {
            int x = Integer.parseInt(matcher.group(1));
            int y = Integer.parseInt(matcher.group(2));
            int z = Integer.parseInt(matcher.group(3));
            return new Location(plugin.getServer().getWorld(worldName), x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Démarre la tâche de mise à jour ultra-fluide (chaque tick)
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID uuid : activeGPS) {
                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        activeGPS.remove(uuid);
                        playerEntityIds.remove(uuid);
                        arrowStates.remove(uuid);
                        continue;
                    }

                    updateArrowForPlayer(player);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Chaque tick pour fluidité maximale
    }

    /**
     * Met à jour la flèche pour un joueur
     */
    private void updateArrowForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        ArrowState state = arrowStates.get(uuid);
        if (state == null) return;

        // Mettre à jour la destination (peut changer si le joueur change d'étape)
        Location newDest = getDestinationForPlayer(player);
        if (newDest == null) {
            // Plus de destination - désactiver le GPS
            disableGPS(player);
            player.sendMessage("§e§l! §7Tu as atteint ta destination ou l'étape a changé.");
            return;
        }
        state.destination = newDest;

        // Calculer la position cible de la flèche
        Location playerLoc = player.getLocation();
        Vector toDestination = newDest.toVector().subtract(playerLoc.toVector());
        double distance = toDestination.length();

        // Si très proche de la destination
        if (distance < 5) {
            // Flèche vers le bas / pulse pour indiquer qu'on est arrivé
            state.pulseEffect = true;
        } else {
            state.pulseEffect = false;
        }

        // Direction vers la destination (normalisée)
        Vector direction = toDestination.normalize();

        // Position de base de la flèche (devant le joueur, au niveau des yeux)
        Location arrowBase = playerLoc.clone().add(0, ARROW_HEIGHT, 0);
        arrowBase.add(direction.clone().multiply(ARROW_DISTANCE));

        // Interpolation fluide
        if (state.currentBasePosition == null) {
            state.currentBasePosition = arrowBase.clone();
        } else {
            // Interpolation lerp pour fluidité (0.15 = très fluide)
            state.currentBasePosition = lerp(state.currentBasePosition, arrowBase, 0.2);
        }

        // Calculer l'angle de rotation pour la flèche
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
        float pitch = (float) Math.toDegrees(-Math.asin(direction.getY()));

        // Interpolation de la rotation
        state.currentYaw = lerpAngle(state.currentYaw, yaw, 0.15f);
        state.currentPitch = lerpAngle(state.currentPitch, pitch, 0.15f);

        // Mettre à jour les positions des entités
        updateArrowPositions(player, state);
    }

    /**
     * Met à jour les positions des armor stands de la flèche
     */
    private void updateArrowPositions(Player player, ArrowState state) {
        int[] entityIds = playerEntityIds.get(player.getUniqueId());
        if (entityIds == null) return;

        Location base = state.currentBasePosition;
        if (base == null) return;

        // Calculer les positions des points de la flèche
        // La flèche est composée de:
        // - 1 point central (pointe)
        // - 2 points sur les côtés (ailes)
        // - 4 points pour la queue
        Location[] positions = calculateArrowPositions(base, state.currentYaw, state.currentPitch, state.pulseEffect);

        // Envoyer les packets de téléportation
        for (int i = 0; i < ARROW_PARTS && i < positions.length; i++) {
            sendTeleportPacket(player, entityIds[i], positions[i], state.currentYaw, state.currentPitch);
        }
    }

    /**
     * Calcule les positions des points de la flèche
     */
    private Location[] calculateArrowPositions(Location base, float yaw, float pitch, boolean pulse) {
        Location[] positions = new Location[ARROW_PARTS];

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // Vecteurs de direction
        Vector forward = new Vector(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalize();

        Vector right = new Vector(
            Math.cos(yawRad),
            0,
            Math.sin(yawRad)
        ).normalize();

        Vector up = forward.clone().crossProduct(right).normalize();

        // Effet de pulsation si proche
        double scale = pulse ? 0.8 + 0.2 * Math.sin(System.currentTimeMillis() * 0.01) : 1.0;

        // Position 0: Pointe de la flèche (avant)
        positions[0] = base.clone().add(forward.clone().multiply(0.3 * scale));

        // Positions 1-2: Ailes de la flèche
        positions[1] = base.clone().add(right.clone().multiply(0.2 * scale)).subtract(forward.clone().multiply(0.15));
        positions[2] = base.clone().subtract(right.clone().multiply(0.2 * scale)).subtract(forward.clone().multiply(0.15));

        // Positions 3-4: Corps de la flèche
        positions[3] = base.clone().subtract(forward.clone().multiply(0.1 * scale));
        positions[4] = base.clone().subtract(forward.clone().multiply(0.25 * scale));

        // Positions 5-6: Queue de la flèche
        positions[5] = base.clone().add(right.clone().multiply(0.12 * scale)).subtract(forward.clone().multiply(0.35));
        positions[6] = base.clone().subtract(right.clone().multiply(0.12 * scale)).subtract(forward.clone().multiply(0.35));

        return positions;
    }

    /**
     * Spawn les entités armor stand client-side
     */
    private void spawnArrowEntities(Player player) {
        int[] entityIds = playerEntityIds.get(player.getUniqueId());
        if (entityIds == null) return;

        Location loc = player.getLocation();

        for (int i = 0; i < ARROW_PARTS; i++) {
            try {
                // Packet de spawn d'entité
                PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

                spawnPacket.getIntegers().write(0, entityIds[i]); // Entity ID
                spawnPacket.getUUIDs().write(0, UUID.randomUUID()); // UUID
                spawnPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND); // Type
                spawnPacket.getDoubles().write(0, loc.getX()); // X
                spawnPacket.getDoubles().write(1, loc.getY()); // Y
                spawnPacket.getDoubles().write(2, loc.getZ()); // Z

                protocolManager.sendServerPacket(player, spawnPacket);

                // Packet de métadonnées pour rendre invisible + petit + marqueur
                sendMetadataPacket(player, entityIds[i], i == 0);

            } catch (Exception e) {
                plugin.getLogger().warning("Erreur spawn GPS entity: " + e.getMessage());
            }
        }
    }

    /**
     * Envoie les métadonnées pour configurer l'armor stand
     */
    private void sendMetadataPacket(Player player, int entityId, boolean isHead) {
        try {
            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);

            List<WrappedDataValue> dataValues = new ArrayList<>();

            // Index 0: Entity flags (Invisible = 0x20)
            dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0x20));

            // Index 15: Armor stand flags (Small=0x01, NoBasePlate=0x08, Marker=0x10)
            byte armorStandFlags = (byte) (0x01 | 0x08 | 0x10); // Small + NoBasePlate + Marker
            dataValues.add(new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), armorStandFlags));

            // Custom name visible avec couleur basée sur la position
            String color = isHead ? "§a" : "§e"; // Vert pour la pointe, jaune pour le reste
            dataValues.add(new WrappedDataValue(3, WrappedDataWatcher.Registry.get(Boolean.class), true)); // Name visible

            var modifier = metadataPacket.getDataValueCollectionModifier();
            if (modifier.size() > 0) {
                modifier.write(0, dataValues);
            }

            protocolManager.sendServerPacket(player, metadataPacket);

        } catch (Exception e) {
            plugin.getLogger().warning("Erreur metadata GPS: " + e.getMessage());
        }
    }

    /**
     * Envoie un packet de téléportation pour une entité
     */
    private void sendTeleportPacket(Player player, int entityId, Location loc, float yaw, float pitch) {
        try {
            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles().write(0, loc.getX());
            teleportPacket.getDoubles().write(1, loc.getY());
            teleportPacket.getDoubles().write(2, loc.getZ());
            teleportPacket.getBytes().write(0, (byte) (yaw * 256.0F / 360.0F));
            teleportPacket.getBytes().write(1, (byte) (pitch * 256.0F / 360.0F));
            teleportPacket.getBooleans().write(0, false); // On ground

            protocolManager.sendServerPacket(player, teleportPacket);

        } catch (Exception e) {
            // Silently ignore - peut arriver lors de la déconnexion
        }
    }

    /**
     * Détruit les entités armor stand client-side
     */
    private void destroyArrowEntities(Player player) {
        int[] entityIds = playerEntityIds.get(player.getUniqueId());
        if (entityIds == null) return;

        try {
            // Packet de destruction
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

            // Convertir en liste d'integers
            List<Integer> ids = new ArrayList<>();
            for (int id : entityIds) {
                ids.add(id);
            }
            destroyPacket.getIntLists().write(0, ids);

            protocolManager.sendServerPacket(player, destroyPacket);

        } catch (Exception e) {
            plugin.getLogger().warning("Erreur destroy GPS entities: " + e.getMessage());
        }
    }

    /**
     * Interpolation linéaire entre deux locations
     */
    private Location lerp(Location from, Location to, double t) {
        return new Location(
            from.getWorld(),
            from.getX() + (to.getX() - from.getX()) * t,
            from.getY() + (to.getY() - from.getY()) * t,
            from.getZ() + (to.getZ() - from.getZ()) * t
        );
    }

    /**
     * Interpolation d'angle (gère le wrap-around 360°)
     */
    private float lerpAngle(float from, float to, float t) {
        float diff = to - from;

        // Normaliser la différence entre -180 et 180
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;

        return from + diff * t;
    }

    // ==================== EVENT HANDLERS ====================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        activeGPS.remove(uuid);
        playerEntityIds.remove(uuid);
        arrowStates.remove(uuid);
    }

    /**
     * Arrête proprement le manager
     */
    public void shutdown() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        // Nettoyer toutes les entités
        for (UUID uuid : activeGPS) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                destroyArrowEntities(player);
            }
        }

        activeGPS.clear();
        playerEntityIds.clear();
        arrowStates.clear();
    }

    // ==================== CLASSES INTERNES ====================

    /**
     * État de la flèche pour un joueur (pour l'interpolation)
     */
    private static class ArrowState {
        Location currentBasePosition;
        Location destination;
        float currentYaw = 0;
        float currentPitch = 0;
        boolean pulseEffect = false;

        ArrowState(Location playerLoc, Location destination) {
            this.destination = destination;
            // Position initiale légèrement devant le joueur
            this.currentBasePosition = playerLoc.clone().add(
                playerLoc.getDirection().multiply(ARROW_DISTANCE)
            ).add(0, ARROW_HEIGHT, 0);
        }
    }
}
