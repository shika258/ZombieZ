package com.rinaorc.zombiez.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.progression.journey.JourneyChapter;
import com.rinaorc.zombiez.progression.journey.JourneyGate;
import com.rinaorc.zombiez.zones.Zone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Gestionnaire des TextDisplay client-side pour les zones verrouillées
 *
 * Affiche un grand TextDisplay (scale x15) devant le joueur quand il approche
 * d'une zone verrouillée, positionné en fonction de sa position X.
 */
public class ZoneLockDisplayManager {

    private final ZombieZPlugin plugin;
    private final ProtocolManager protocolManager;

    // Entity ID counter pour les entités virtuelles (négatif pour éviter conflit avec vraies entités)
    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(-1000000);

    // Cache des displays actifs par joueur (pour pouvoir les supprimer)
    private final Map<UUID, ActiveLockDisplay> activeDisplays = new ConcurrentHashMap<>();

    // Cooldown pour éviter le spam de création/suppression
    private final Map<UUID, Long> displayCooldowns = new ConcurrentHashMap<>();
    private static final long DISPLAY_COOLDOWN_MS = 500;

    // Configuration
    private static final double DETECTION_DISTANCE = 30.0; // Distance de détection de la limite de zone
    private static final double DISPLAY_DISTANCE = 15.0; // Distance du display devant le joueur
    private static final double DISPLAY_HEIGHT = 8.0; // Hauteur au-dessus du sol
    private static final float SCALE = 15.0f; // Échelle du texte

    // Tâche de vérification périodique
    private BukkitRunnable checkTask;

    public ZoneLockDisplayManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Démarre le système
     */
    public void start() {
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                String gameWorld = plugin.getZoneManager().getGameWorld();
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    // Optimisation: ignorer les joueurs hors du monde de jeu
                    if (player.getWorld().getName().equals(gameWorld)) {
                        checkPlayerProximity(player);
                    }
                }
            }
        };
        checkTask.runTaskTimer(plugin, 20L, 20L); // Toutes les secondes (optimisé pour 200 joueurs)

        plugin.log(Level.INFO, "§a✓ ZoneLockDisplayManager démarré");
    }

    /**
     * Arrête le système
     */
    public void stop() {
        if (checkTask != null) {
            checkTask.cancel();
        }

        // Supprimer tous les displays actifs
        for (UUID uuid : new HashSet<>(activeDisplays.keySet())) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                removeDisplay(player);
            }
        }

        activeDisplays.clear();
        displayCooldowns.clear();
    }

    /**
     * Vérifie si un joueur approche d'une zone verrouillée
     * Note: Le monde est déjà vérifié dans la tâche principale
     */
    private void checkPlayerProximity(Player player) {
        Location loc = player.getLocation();

        // Obtenir la zone actuelle (utilise le cache du ZoneManager)
        Zone currentZone = plugin.getZoneManager().getZoneAt(loc);
        if (currentZone == null || currentZone.getId() == 0) {
            // Zone spawn ou invalide - pas de display
            removeDisplayIfExists(player);
            return;
        }

        // Calculer la distance jusqu'à la prochaine zone (vers le nord = Z décroissant)
        int distanceToNextZone = loc.getBlockZ() - currentZone.getMinZ();

        // Si le joueur est proche de la limite nord de sa zone
        if (distanceToNextZone <= DETECTION_DISTANCE && distanceToNextZone > 0) {
            int nextZoneId = currentZone.getId() + 1;

            // Vérifier si le joueur a accès à la zone suivante (utilise le cache)
            if (!plugin.getJourneyManager().canAccessZone(player, nextZoneId)) {
                // Zone verrouillée ! Afficher le display
                showLockDisplay(player, currentZone, nextZoneId, distanceToNextZone);
                return;
            }
        }

        // Pas proche d'une limite ou zone accessible - supprimer le display
        removeDisplayIfExists(player);
    }

    /**
     * Affiche le TextDisplay de zone verrouillée
     */
    private void showLockDisplay(Player player, Zone currentZone, int nextZoneId, int distanceToZone) {
        UUID uuid = player.getUniqueId();

        // Vérifier le cooldown
        Long lastDisplay = displayCooldowns.get(uuid);
        if (lastDisplay != null && System.currentTimeMillis() - lastDisplay < DISPLAY_COOLDOWN_MS) {
            return;
        }

        // Vérifier si un display existe déjà
        ActiveLockDisplay existing = activeDisplays.get(uuid);
        if (existing != null) {
            // Mettre à jour la position si nécessaire
            updateDisplayPosition(player, existing, currentZone);
            return;
        }

        // Créer un nouveau display
        createLockDisplay(player, currentZone, nextZoneId);
        displayCooldowns.put(uuid, System.currentTimeMillis());
    }

    /**
     * Crée un nouveau TextDisplay client-side
     */
    private void createLockDisplay(Player player, Zone currentZone, int nextZoneId) {
        UUID uuid = player.getUniqueId();
        Location playerLoc = player.getLocation();

        // Calculer la position du display (à la limite de zone, devant le joueur)
        Location displayLoc = calculateDisplayLocation(playerLoc, currentZone);

        // Générer un entity ID unique
        int entityId = ENTITY_ID_COUNTER.getAndDecrement();

        // Déterminer le texte à afficher
        String displayText = buildDisplayText(nextZoneId, currentZone.getId());

        try {
            // Envoyer le paquet de spawn
            sendSpawnPacket(player, entityId, displayLoc);

            // Envoyer les métadonnées
            sendMetadataPacket(player, entityId, displayText);

            // Enregistrer le display actif
            activeDisplays.put(uuid, new ActiveLockDisplay(entityId, displayLoc, nextZoneId));

        } catch (Exception e) {
            plugin.log(Level.WARNING, "§cErreur création display lock pour " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Calcule la position du display en fonction de la position du joueur
     */
    private Location calculateDisplayLocation(Location playerLoc, Zone currentZone) {
        // Le display est à la limite nord de la zone actuelle
        double displayZ = currentZone.getMinZ() - 2; // Juste après la limite
        double displayX = playerLoc.getX(); // Même X que le joueur
        double displayY = playerLoc.getY() + DISPLAY_HEIGHT;

        return new Location(playerLoc.getWorld(), displayX, displayY, displayZ);
    }

    /**
     * Construit le texte du display
     */
    private String buildDisplayText(int nextZoneId, int currentZoneId) {
        // Trouver le chapitre requis
        JourneyGate gate = JourneyGate.getZoneGate(nextZoneId);
        String chapterRequired = "Chapitre ?";

        if (gate != null) {
            // Trouver quel chapitre débloque cette gate
            for (JourneyChapter chapter : JourneyChapter.values()) {
                for (JourneyGate unlock : chapter.getUnlocks()) {
                    if (unlock == gate) {
                        chapterRequired = "Chapitre " + chapter.getId();
                        break;
                    }
                }
            }
        }

        // Déterminer les zones verrouillées (souvent 2-3 zones par chapitre)
        int endZone = getChapterEndZone(nextZoneId);
        String zoneRange = endZone > nextZoneId
            ? "Zone " + nextZoneId + "-" + endZone
            : "Zone " + nextZoneId;

        // Construire le texte multi-lignes
        StringBuilder text = new StringBuilder();
        text.append("§c§l⛔ ").append(zoneRange).append(" ⛔\n");
        text.append("§4§lVERROUILLÉE\n");
        text.append("\n");
        text.append("§7Termine le\n");
        text.append("§e§l").append(chapterRequired).append("\n");
        text.append("\n");
        text.append("§b§l/journal");

        return text.toString();
    }

    /**
     * Obtient la dernière zone débloquée par le même chapitre
     */
    private int getChapterEndZone(int startZone) {
        JourneyGate startGate = JourneyGate.getZoneGate(startZone);
        if (startGate == null) return startZone;

        // Trouver le chapitre qui débloque cette zone
        JourneyChapter targetChapter = null;
        for (JourneyChapter chapter : JourneyChapter.values()) {
            for (JourneyGate unlock : chapter.getUnlocks()) {
                if (unlock == startGate) {
                    targetChapter = chapter;
                    break;
                }
            }
            if (targetChapter != null) break;
        }

        if (targetChapter == null) return startZone;

        // Trouver la zone max débloquée par ce chapitre
        int maxZone = startZone;
        for (JourneyGate unlock : targetChapter.getUnlocks()) {
            if (unlock.getType() == JourneyGate.GateType.ZONE) {
                maxZone = Math.max(maxZone, unlock.getValue());
            }
        }

        return maxZone;
    }

    /**
     * Envoie le paquet de spawn de l'entité TextDisplay
     */
    private void sendSpawnPacket(Player player, int entityId, Location loc) throws Exception {
        PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        // Entity ID
        spawnPacket.getIntegers().write(0, entityId);

        // UUID
        spawnPacket.getUUIDs().write(0, UUID.randomUUID());

        // Entity Type (TEXT_DISPLAY)
        spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);

        // Position
        spawnPacket.getDoubles().write(0, loc.getX());
        spawnPacket.getDoubles().write(1, loc.getY());
        spawnPacket.getDoubles().write(2, loc.getZ());

        // Velocity (0)
        spawnPacket.getIntegers().write(1, 0);
        spawnPacket.getIntegers().write(2, 0);
        spawnPacket.getIntegers().write(3, 0);

        // Pitch/Yaw
        spawnPacket.getBytes().write(0, (byte) 0); // Pitch
        spawnPacket.getBytes().write(1, (byte) 0); // Yaw
        spawnPacket.getBytes().write(2, (byte) 0); // Head yaw

        // Data
        spawnPacket.getIntegers().write(4, 0);

        protocolManager.sendServerPacket(player, spawnPacket);
    }

    /**
     * Envoie les métadonnées du TextDisplay
     */
    private void sendMetadataPacket(Player player, int entityId, String text) throws Exception {
        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);

        // Entity ID
        metadataPacket.getIntegers().write(0, entityId);

        // Créer les data values
        List<WrappedDataValue> dataValues = new ArrayList<>();

        // Index 0: Flags (invisible = false)
        dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));

        // Index 8: Billboard mode (2 = CENTER - toujours face au joueur)
        dataValues.add(new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), (byte) 3));

        // Index 22: Text (Component en JSON)
        Component textComponent = Component.text(text);
        String jsonText = GsonComponentSerializer.gson().serialize(textComponent);
        WrappedChatComponent wrappedText = WrappedChatComponent.fromJson(jsonText);
        dataValues.add(new WrappedDataValue(23, WrappedDataWatcher.Registry.getChatComponentSerializer(false), wrappedText.getHandle()));

        // Index 23: Line width (200 pour permettre le texte large)
        dataValues.add(new WrappedDataValue(24, WrappedDataWatcher.Registry.get(Integer.class), 400));

        // Index 29: Default background (1 = utiliser le fond par défaut style tooltip)
        dataValues.add(new WrappedDataValue(29, WrappedDataWatcher.Registry.get(Byte.class), (byte) 1));

        // Index 26: Text opacity (255 = opaque)
        dataValues.add(new WrappedDataValue(27, WrappedDataWatcher.Registry.get(Byte.class), (byte) -1));

        // Index 27: Shadowed text
        dataValues.add(new WrappedDataValue(28, WrappedDataWatcher.Registry.get(Byte.class), (byte) 1));

        // Scale via Transformation
        // Pour le scale, on utilise l'index 12 (Transformation)
        // Format: translation (Vector3f), left rotation (Quaternion), scale (Vector3f), right rotation (Quaternion)
        Vector3f translation = new Vector3f(0, 0, 0);
        Vector3f scale = new Vector3f(SCALE, SCALE, SCALE);
        org.joml.Quaternionf leftRot = new org.joml.Quaternionf(0, 0, 0, 1);
        org.joml.Quaternionf rightRot = new org.joml.Quaternionf(0, 0, 0, 1);

        // Créer le tableau de transformation
        // ProtocolLib attend un Transformation, pas des composants séparés
        // On va essayer d'utiliser les index individuels pour la transformation
        // En 1.21.4, Display entities ont:
        // Index 11: interpolation duration (int)
        // Index 12: translation (Vector3f)
        // Index 13: scale (Vector3f)
        // Index 14: left rotation (Quaternionf)
        // Index 15: right rotation (Quaternionf)

        // Pour simplifier, utilisons view_range et shadow_radius pour un effet visible
        // View range (index 17)
        dataValues.add(new WrappedDataValue(18, WrappedDataWatcher.Registry.get(Float.class), 100.0f));

        metadataPacket.getDataValueCollectionModifier().write(0, dataValues);

        protocolManager.sendServerPacket(player, metadataPacket);

        // Envoyer la transformation séparément via un paquet bundle si nécessaire
        sendTransformationPacket(player, entityId, scale);
    }

    /**
     * Envoie le paquet de transformation pour le scale
     */
    private void sendTransformationPacket(Player player, int entityId, Vector3f scale) {
        try {
            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);

            List<WrappedDataValue> dataValues = new ArrayList<>();

            // Transformation complète (index 12 dans Display)
            // Utiliser la classe Transformation de Bukkit
            org.bukkit.util.Transformation transformation = new org.bukkit.util.Transformation(
                new Vector3f(0, 0, 0),           // Translation
                new org.joml.AxisAngle4f(0, 0, 0, 1), // Left rotation
                scale,                           // Scale
                new org.joml.AxisAngle4f(0, 0, 0, 1)  // Right rotation
            );

            // ProtocolLib gère les types Bukkit pour la sérialisation
            // On utilise le registre approprié
            var registry = WrappedDataWatcher.Registry.get(org.bukkit.util.Transformation.class);
            if (registry != null) {
                dataValues.add(new WrappedDataValue(12, registry, transformation));
            }

            if (!dataValues.isEmpty()) {
                metadataPacket.getDataValueCollectionModifier().write(0, dataValues);
                protocolManager.sendServerPacket(player, metadataPacket);
            }

        } catch (Exception e) {
            // Fallback silencieux si la transformation ne peut pas être envoyée
            plugin.log(Level.FINE, "Transformation packet fallback: " + e.getMessage());
        }
    }

    /**
     * Met à jour la position d'un display existant
     */
    private void updateDisplayPosition(Player player, ActiveLockDisplay display, Zone currentZone) {
        Location playerLoc = player.getLocation();
        Location newLoc = calculateDisplayLocation(playerLoc, currentZone);

        // Vérifier si la position a significativement changé (5 blocs)
        if (display.location.distanceSquared(newLoc) < 25) {
            return; // Pas besoin de mettre à jour
        }

        // Envoyer un paquet de téléportation
        try {
            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

            teleportPacket.getIntegers().write(0, display.entityId);
            teleportPacket.getDoubles().write(0, newLoc.getX());
            teleportPacket.getDoubles().write(1, newLoc.getY());
            teleportPacket.getDoubles().write(2, newLoc.getZ());
            teleportPacket.getBytes().write(0, (byte) 0); // Yaw
            teleportPacket.getBytes().write(1, (byte) 0); // Pitch
            teleportPacket.getBooleans().write(0, false); // On ground

            protocolManager.sendServerPacket(player, teleportPacket);

            // Mettre à jour le cache
            display.location = newLoc;

        } catch (Exception e) {
            plugin.log(Level.WARNING, "§cErreur téléportation display: " + e.getMessage());
        }
    }

    /**
     * Supprime le display d'un joueur s'il existe
     */
    private void removeDisplayIfExists(Player player) {
        if (activeDisplays.containsKey(player.getUniqueId())) {
            removeDisplay(player);
        }
    }

    /**
     * Supprime le display d'un joueur
     */
    public void removeDisplay(Player player) {
        UUID uuid = player.getUniqueId();
        ActiveLockDisplay display = activeDisplays.remove(uuid);

        if (display == null) return;

        try {
            // Envoyer le paquet de destruction
            PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroyPacket.getIntLists().write(0, List.of(display.entityId));
            protocolManager.sendServerPacket(player, destroyPacket);

        } catch (Exception e) {
            plugin.log(Level.WARNING, "§cErreur suppression display: " + e.getMessage());
        }
    }

    /**
     * Nettoie les données d'un joueur (déconnexion)
     */
    public void cleanupPlayer(UUID uuid) {
        activeDisplays.remove(uuid);
        displayCooldowns.remove(uuid);
    }

    /**
     * Représente un display actif
     */
    private static class ActiveLockDisplay {
        final int entityId;
        Location location;
        final int lockedZoneId;

        ActiveLockDisplay(int entityId, Location location, int lockedZoneId) {
            this.entityId = entityId;
            this.location = location;
            this.lockedZoneId = lockedZoneId;
        }
    }
}
