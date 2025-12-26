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
 * Système GPS optimisé pour guider les joueurs vers les objectifs du Journey
 *
 * OPTIMISATIONS APPLIQUÉES:
 * - 5 entités (pointe + 2 ailes + 2 corps) = forme de flèche claire
 * - Mise à jour seulement si le joueur bouge (seuil de distance)
 * - Packets de mouvement relatif (plus légers que téléportation)
 * - Interpolation fluide avec cache des positions
 * - Fréquence adaptative (plus lent si joueur immobile)
 *
 * Fonctionne automatiquement pour toutes les étapes avec coordonnées
 * Format supporté: "§bX, Y, Z" ou "§b~X, ~Y, ~Z" dans la description
 */
public class GPSManager implements Listener {

    private final ZombieZPlugin plugin;
    private final ProtocolManager protocolManager;
    private final JourneyManager journeyManager;

    // Joueurs avec GPS actif
    private final Set<UUID> activeGPS = ConcurrentHashMap.newKeySet();

    // Entity IDs virtuels pour chaque joueur
    private final Map<UUID, int[]> playerEntityIds = new ConcurrentHashMap<>();

    // Générateur d'IDs d'entités (haut pour éviter conflits)
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE - 100000);

    // Patterns pour extraire les coordonnées (supporte plusieurs formats)
    // Format: "§bX, Y, Z" ou "§b~X, ~Y, ~Z" ou "Coords: §bX, Y, Z"
    private static final Pattern COORD_PATTERN = Pattern.compile("§b~?(-?\\d+),?\\s*~?(-?\\d+),?\\s*~?(-?\\d+)");

    // ==================== CONFIGURATION OPTIMISÉE ====================
    private static final int ARROW_PARTS = 5;           // Pointe + 2 ailes + 2 corps
    private static final double ARROW_DISTANCE = 2.5;   // Distance devant le joueur
    private static final double ARROW_HEIGHT = 1.3;     // Hauteur au niveau des yeux
    private static final double MOVE_THRESHOLD = 0.05;  // Seuil pour envoyer update (en blocs)
    private static final double ROTATE_THRESHOLD = 2.0; // Seuil pour rotation (en degrés)
    private static final float LERP_SPEED = 0.25f;      // Vitesse d'interpolation (plus haut = plus réactif)

    // Tâche de mise à jour
    private BukkitTask updateTask;

    // États des flèches par joueur
    private final Map<UUID, ArrowState> arrowStates = new ConcurrentHashMap<>();

    public GPSManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.journeyManager = plugin.getJourneyManager();

        startUpdateTask();
    }

    /**
     * Toggle le GPS pour un joueur
     */
    public boolean toggleGPS(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeGPS.contains(uuid)) {
            disableGPS(player);
            return false;
        } else {
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
            player.sendMessage("§8Le GPS fonctionne pour les étapes avec coordonnées.");
            return false;
        }

        // Créer les entity IDs virtuels (3 entités seulement)
        int[] entityIds = new int[ARROW_PARTS];
        for (int i = 0; i < ARROW_PARTS; i++) {
            entityIds[i] = ENTITY_ID_COUNTER.getAndDecrement();
        }
        playerEntityIds.put(uuid, entityIds);

        // Initialiser l'état de la flèche
        Location playerLoc = player.getLocation();
        arrowStates.put(uuid, new ArrowState(playerLoc, destination));

        // Spawn initial des armor stands
        spawnArrowEntities(player, playerLoc);

        activeGPS.add(uuid);

        // Afficher la distance
        double distance = playerLoc.distance(destination);
        String distStr = distance < 100 ? String.format("%.0f", distance) : String.format("%.0f", distance);

        player.sendMessage("§a§l✓ §eGPS activé! §7Distance: §e" + distStr + " blocs");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.5f);

        return true;
    }

    /**
     * Désactive le GPS pour un joueur
     */
    public void disableGPS(Player player) {
        UUID uuid = player.getUniqueId();

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
     * Supporte: "§b625, 93, 9853", "Coords: §b1036, 82, 9627", "§b~345, ~86, ~9500"
     */
    private Location parseCoordinates(String description, String worldName) {
        if (description == null) return null;

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
     * Démarre la tâche de mise à jour optimisée
     * Fréquence: tous les 2 ticks (10 updates/sec = suffisant pour fluidité)
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Itérateur pour suppression safe
                Iterator<UUID> iterator = activeGPS.iterator();
                while (iterator.hasNext()) {
                    UUID uuid = iterator.next();
                    Player player = plugin.getServer().getPlayer(uuid);

                    if (player == null || !player.isOnline()) {
                        iterator.remove();
                        playerEntityIds.remove(uuid);
                        arrowStates.remove(uuid);
                        continue;
                    }

                    updateArrowForPlayer(player);
                }
            }
        }.runTaskTimer(plugin, 2L, 2L); // Tous les 2 ticks = 10 FPS (fluide et léger)
    }

    /**
     * Met à jour la flèche pour un joueur (optimisé)
     */
    private void updateArrowForPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        ArrowState state = arrowStates.get(uuid);
        if (state == null) return;

        Location playerLoc = player.getLocation();

        // Vérifier si la destination a changé (changement d'étape)
        Location newDest = getDestinationForPlayer(player);
        if (newDest == null) {
            // Plus de destination - désactiver silencieusement
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                disableGPS(player);
                player.sendMessage("§e§l! §7Étape terminée ou changée.");
            });
            return;
        }

        // Mise à jour de la destination si changée
        if (!isSameLocation(state.destination, newDest)) {
            state.destination = newDest;
        }

        // Calculer la direction vers la destination
        Vector toDestination = newDest.toVector().subtract(playerLoc.toVector());
        double distance = toDestination.length();

        // Vérifier si arrivé à destination
        if (distance < 3) {
            state.pulseEffect = true;
        } else {
            state.pulseEffect = false;
        }

        // Éviter division par zéro
        if (distance < 0.1) {
            toDestination = new Vector(0, 0, 1);
        } else {
            toDestination.normalize();
        }

        // Position cible de la flèche (devant le joueur, vers la destination)
        Location targetBase = playerLoc.clone().add(0, ARROW_HEIGHT, 0);
        targetBase.add(toDestination.clone().multiply(ARROW_DISTANCE));

        // Angles cibles
        float targetYaw = (float) Math.toDegrees(Math.atan2(-toDestination.getX(), toDestination.getZ()));
        float targetPitch = (float) Math.toDegrees(-Math.asin(toDestination.getY()));

        // OPTIMISATION: Vérifier si mise à jour nécessaire
        boolean needsUpdate = false;

        // Vérifier le mouvement du joueur
        if (state.lastPlayerPos == null ||
            state.lastPlayerPos.distanceSquared(playerLoc) > MOVE_THRESHOLD * MOVE_THRESHOLD) {
            needsUpdate = true;
            state.lastPlayerPos = playerLoc.clone();
        }

        // Vérifier la rotation vers la destination
        float yawDiff = Math.abs(angleDiff(state.currentYaw, targetYaw));
        float pitchDiff = Math.abs(state.currentPitch - targetPitch);
        if (yawDiff > ROTATE_THRESHOLD || pitchDiff > ROTATE_THRESHOLD) {
            needsUpdate = true;
        }

        // Toujours mettre à jour l'interpolation interne
        if (state.currentBasePosition == null) {
            state.currentBasePosition = targetBase.clone();
        } else {
            state.currentBasePosition = lerp(state.currentBasePosition, targetBase, LERP_SPEED);
        }
        state.currentYaw = lerpAngle(state.currentYaw, targetYaw, LERP_SPEED);
        state.currentPitch = lerpAngle(state.currentPitch, targetPitch, LERP_SPEED);

        // OPTIMISATION: Envoyer packets seulement si nécessaire ou en pulse mode
        if (needsUpdate || state.pulseEffect) {
            sendArrowPositionPackets(player, state);
        }
    }

    /**
     * Envoie les packets de position pour les entités de la flèche
     */
    private void sendArrowPositionPackets(Player player, ArrowState state) {
        int[] entityIds = playerEntityIds.get(player.getUniqueId());
        if (entityIds == null || state.currentBasePosition == null) return;

        Location base = state.currentBasePosition;
        float yaw = state.currentYaw;

        // Calculer les positions (3 entités seulement)
        Location[] positions = calculateArrowPositions(base, yaw, state.currentPitch, state.pulseEffect);

        // Envoyer les packets de téléportation en batch
        for (int i = 0; i < ARROW_PARTS && i < positions.length; i++) {
            sendTeleportPacket(player, entityIds[i], positions[i], yaw);
        }
    }

    /**
     * Calcule les positions optimisées de la flèche (5 points)
     *
     * Structure visuelle:
     *       ●        <- Pointe (0)
     *     ●   ●      <- Ailes (1, 2)
     *       ●        <- Corps (3)
     *       ●        <- Queue (4)
     */
    private Location[] calculateArrowPositions(Location base, float yaw, float pitch, boolean pulse) {
        Location[] positions = new Location[ARROW_PARTS];

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // Vecteur forward (direction de la flèche)
        Vector forward = new Vector(
            -Math.sin(yawRad) * Math.cos(pitchRad),
            -Math.sin(pitchRad),
            Math.cos(yawRad) * Math.cos(pitchRad)
        );

        // Vecteur right (perpendiculaire horizontal)
        Vector right = new Vector(Math.cos(yawRad), 0, Math.sin(yawRad));

        // Effet de pulsation si proche de la destination
        double scale = pulse ? 0.85 + 0.15 * Math.sin(System.currentTimeMillis() * 0.008) : 1.0;

        // Position 0: Pointe de la flèche (tout devant)
        positions[0] = base.clone().add(forward.clone().multiply(0.45 * scale));

        // Positions 1-2: Ailes de la flèche (écartées sur les côtés, légèrement en arrière)
        positions[1] = base.clone()
            .add(right.clone().multiply(0.30 * scale))
            .subtract(forward.clone().multiply(0.15));

        positions[2] = base.clone()
            .subtract(right.clone().multiply(0.30 * scale))
            .subtract(forward.clone().multiply(0.15));

        // Position 3: Corps de la flèche (derrière les ailes)
        positions[3] = base.clone().subtract(forward.clone().multiply(0.25 * scale));

        // Position 4: Queue de la flèche (tout derrière)
        positions[4] = base.clone().subtract(forward.clone().multiply(0.50 * scale));

        return positions;
    }

    /**
     * Spawn les entités armor stand client-side
     */
    private void spawnArrowEntities(Player player, Location loc) {
        int[] entityIds = playerEntityIds.get(player.getUniqueId());
        if (entityIds == null) return;

        try {
            for (int i = 0; i < ARROW_PARTS; i++) {
                // Packet de spawn
                PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
                spawnPacket.getIntegers().write(0, entityIds[i]);
                spawnPacket.getUUIDs().write(0, UUID.randomUUID());
                spawnPacket.getEntityTypeModifier().write(0, EntityType.ARMOR_STAND);
                spawnPacket.getDoubles().write(0, loc.getX());
                spawnPacket.getDoubles().write(1, loc.getY() + ARROW_HEIGHT);
                spawnPacket.getDoubles().write(2, loc.getZ());

                protocolManager.sendServerPacket(player, spawnPacket);

                // Packet de métadonnées
                sendMetadataPacket(player, entityIds[i], i == 0);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur spawn GPS: " + e.getMessage());
        }
    }

    /**
     * Configure l'armor stand comme marqueur invisible avec glow
     */
    private void sendMetadataPacket(Player player, int entityId, boolean isHead) {
        try {
            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);

            List<WrappedDataValue> dataValues = new ArrayList<>();

            // Index 0: Entity flags (Invisible=0x20, Glowing=0x40)
            byte entityFlags = (byte) (0x20 | 0x40); // Invisible + Glowing
            dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), entityFlags));

            // Index 15: Armor stand flags (Small=0x01, NoBasePlate=0x08, Marker=0x10)
            byte armorStandFlags = (byte) (0x01 | 0x08 | 0x10);
            dataValues.add(new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), armorStandFlags));

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
     * Envoie un packet de téléportation optimisé
     */
    private void sendTeleportPacket(Player player, int entityId, Location loc, float yaw) {
        try {
            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
            teleportPacket.getIntegers().write(0, entityId);
            teleportPacket.getDoubles().write(0, loc.getX());
            teleportPacket.getDoubles().write(1, loc.getY());
            teleportPacket.getDoubles().write(2, loc.getZ());
            teleportPacket.getBytes().write(0, (byte) (yaw * 256.0F / 360.0F));
            teleportPacket.getBytes().write(1, (byte) 0);
            teleportPacket.getBooleans().write(0, false);

            protocolManager.sendServerPacket(player, teleportPacket);
        } catch (Exception e) {
            // Silencieux - peut arriver lors de la déconnexion
        }
    }

    /**
     * Détruit les entités armor stand client-side
     */
    private void destroyArrowEntities(Player player) {
        int[] entityIds = playerEntityIds.get(player.getUniqueId());
        if (entityIds == null) return;

        try {
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            List<Integer> ids = new ArrayList<>();
            for (int id : entityIds) ids.add(id);
            destroyPacket.getIntLists().write(0, ids);
            protocolManager.sendServerPacket(player, destroyPacket);
        } catch (Exception e) {
            plugin.getLogger().warning("Erreur destroy GPS: " + e.getMessage());
        }
    }

    // ==================== UTILITAIRES ====================

    private Location lerp(Location from, Location to, double t) {
        return new Location(
            from.getWorld(),
            from.getX() + (to.getX() - from.getX()) * t,
            from.getY() + (to.getY() - from.getY()) * t,
            from.getZ() + (to.getZ() - from.getZ()) * t
        );
    }

    private float lerpAngle(float from, float to, float t) {
        float diff = angleDiff(from, to);
        return from + diff * t;
    }

    private float angleDiff(float from, float to) {
        float diff = to - from;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return diff;
    }

    private boolean isSameLocation(Location a, Location b) {
        if (a == null || b == null) return false;
        return a.getBlockX() == b.getBlockX() &&
               a.getBlockY() == b.getBlockY() &&
               a.getBlockZ() == b.getBlockZ();
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

    // ==================== CLASSE INTERNE ====================

    /**
     * État de la flèche pour un joueur (optimisé avec cache)
     */
    private static class ArrowState {
        Location currentBasePosition;
        Location destination;
        Location lastPlayerPos;  // Cache pour optimisation
        float currentYaw = 0;
        float currentPitch = 0;
        boolean pulseEffect = false;

        ArrowState(Location playerLoc, Location destination) {
            this.destination = destination;
            this.lastPlayerPos = playerLoc.clone();
            this.currentBasePosition = playerLoc.clone().add(
                playerLoc.getDirection().multiply(ARROW_DISTANCE)
            ).add(0, ARROW_HEIGHT, 0);
        }
    }
}
