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
    private static final long DISPLAY_COOLDOWN_MS = 250; // Réduit pour meilleur réactivité

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
        checkTask.runTaskTimer(plugin, 10L, 5L); // Toutes les 5 ticks (0.25s) pour suivi fluide de position X

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

        // Rafraîchir le WorldBorder pour s'assurer qu'il est visible
        // (le client peut l'avoir "oublié" après s'être éloigné)
        ZoneBorderManager borderManager = plugin.getZoneBorderManager();
        if (borderManager != null) {
            borderManager.refreshBorder(player);
        }
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
        // Le display est à la limite nord de la zone actuelle (1 bloc devant le worldborder)
        double displayZ = currentZone.getMinZ() - 1; // Juste devant le worldborder
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
     *
     * Structure du paquet SPAWN_ENTITY en 1.21.4 (ProtocolLib):
     * - integers[0]: Entity ID (VarInt)
     * - integers[1]: Data (VarInt, optionnel selon le type d'entité)
     * - uuids[0]: Entity UUID
     * - entityTypeModifier[0]: Type d'entité
     * - doubles[0-2]: X, Y, Z (position)
     * - floats[0-1]: Yaw, Pitch (angles en degrés, remplacent les bytes en 1.21.4)
     * - shorts[0-2]: Velocity X, Y, Z (pas toujours présent selon le type)
     *
     * Note: En 1.21.4, les angles sont passés en float (degrés), pas en byte.
     * La velocity et head yaw ne sont pas toujours présents.
     */
    private void sendSpawnPacket(Player player, int entityId, Location loc) throws Exception {
        PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        // Entity ID (integers[0])
        spawnPacket.getIntegers().write(0, entityId);

        // UUID
        spawnPacket.getUUIDs().write(0, UUID.randomUUID());

        // Entity Type (TEXT_DISPLAY)
        spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);

        // Position (doubles[0-2])
        spawnPacket.getDoubles().write(0, loc.getX());
        spawnPacket.getDoubles().write(1, loc.getY());
        spawnPacket.getDoubles().write(2, loc.getZ());

        // Data (integers[1]) - 0 pour TEXT_DISPLAY
        if (spawnPacket.getIntegers().size() > 1) {
            spawnPacket.getIntegers().write(1, 0);
        }

        // Yaw/Pitch en float (1.21.4 change)
        // Les floats peuvent être présents ou non selon la version de ProtocolLib
        if (spawnPacket.getFloat().size() >= 2) {
            spawnPacket.getFloat().write(0, 0.0f); // Yaw
            spawnPacket.getFloat().write(1, 0.0f); // Pitch
        }

        protocolManager.sendServerPacket(player, spawnPacket);
    }

    /**
     * Envoie les métadonnées du TextDisplay
     *
     * Index des métadonnées en 1.21.4:
     * Display: 8-22 (11=translation, 12=scale, 15=billboard, 17=view_range)
     * TextDisplay: 23-27 (23=text, 24=line_width, 25=bg_color, 26=opacity, 27=flags)
     */
    private void sendMetadataPacket(Player player, int entityId, String text) throws Exception {
        PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);

        // Entity ID
        metadataPacket.getIntegers().write(0, entityId);

        // Créer les data values
        List<WrappedDataValue> dataValues = new ArrayList<>();

        // Index 0: Entity flags (invisible = false)
        dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), (byte) 0));

        // Index 15: Billboard mode (3 = CENTER - toujours face au joueur)
        dataValues.add(new WrappedDataValue(15, WrappedDataWatcher.Registry.get(Byte.class), (byte) 3));

        // Index 17: View range (multiplicateur: 1.0 = 64 blocs de visibilité)
        dataValues.add(new WrappedDataValue(17, WrappedDataWatcher.Registry.get(Float.class), 1.0f));

        // Index 23: Text (Component en JSON)
        Component textComponent = Component.text(text);
        String jsonText = GsonComponentSerializer.gson().serialize(textComponent);
        WrappedChatComponent wrappedText = WrappedChatComponent.fromJson(jsonText);
        dataValues.add(new WrappedDataValue(23, WrappedDataWatcher.Registry.getChatComponentSerializer(false), wrappedText.getHandle()));

        // Index 24: Line width (largeur max avant retour à la ligne)
        dataValues.add(new WrappedDataValue(24, WrappedDataWatcher.Registry.get(Integer.class), 400));

        // Index 25: Background color (ARGB format - 0 = transparent/désactivé)
        dataValues.add(new WrappedDataValue(25, WrappedDataWatcher.Registry.get(Integer.class), 0));

        // Index 26: Text opacity (255 = opaque, -1 en byte signé = 255)
        dataValues.add(new WrappedDataValue(26, WrappedDataWatcher.Registry.get(Byte.class), (byte) -1));

        // Index 27: Flags (combiné: bit 0=shadow, bit 1=see-through, bit 2=default_background, bits 3-4=alignment)
        // Valeur: 0x01 (shadow) - PAS de default_background pour utiliser notre couleur custom
        byte textFlags = (byte) 0x01; // Shadow ON uniquement
        dataValues.add(new WrappedDataValue(27, WrappedDataWatcher.Registry.get(Byte.class), textFlags));

        // Vérifier que le modifier est disponible avant d'écrire (1.21.4 compatibility)
        var dataValueModifier = metadataPacket.getDataValueCollectionModifier();
        if (dataValueModifier.size() > 0) {
            dataValueModifier.write(0, dataValues);
        } else {
            // Fallback: essayer d'utiliser le watchable collection modifier (versions antérieures)
            plugin.log(Level.WARNING, "§eDataValueCollectionModifier non disponible, metadata non envoyées");
        }
        protocolManager.sendServerPacket(player, metadataPacket);

        // Envoyer la transformation (scale) séparément
        sendTransformationPacket(player, entityId, new Vector3f(SCALE, SCALE, SCALE));
    }

    /**
     * Envoie le paquet de transformation pour le scale
     *
     * En 1.21.4, les index sont:
     * - 11: Translation (Vector3f)
     * - 12: Scale (Vector3f)
     *
     * Utiliser org.joml.Vector3f directement avec le serializer approprié.
     */
    private void sendTransformationPacket(Player player, int entityId, Vector3f scale) {
        try {
            PacketContainer metadataPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metadataPacket.getIntegers().write(0, entityId);

            List<WrappedDataValue> dataValues = new ArrayList<>();

            // Obtenir le serializer pour Vector3f (org.joml.Vector3f)
            var vectorSerializer = WrappedDataWatcher.Registry.get(Vector3f.class);

            if (vectorSerializer != null) {
                // Index 11: Translation (pas de décalage)
                dataValues.add(new WrappedDataValue(11, vectorSerializer, new Vector3f(0, 0, 0)));

                // Index 12: Scale
                dataValues.add(new WrappedDataValue(12, vectorSerializer, scale));
            } else {
                // Fallback: essayer de créer le serializer manuellement
                plugin.log(Level.WARNING, "§cVector3f serializer non disponible, scale non appliqué");
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

        // Vérifier si la position a changé (2 blocs - suivi plus réactif sur l'axe X)
        if (display.location.distanceSquared(newLoc) < 4) {
            return; // Pas besoin de mettre à jour
        }

        // Envoyer un paquet de téléportation
        // Note: En 1.21.4, la structure du paquet a changé - les bytes pour yaw/pitch
        // ne sont pas toujours présents. On utilise une approche défensive.
        try {
            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);

            // Entity ID (toujours présent)
            teleportPacket.getIntegers().write(0, display.entityId);

            // Position (toujours présent)
            teleportPacket.getDoubles().write(0, newLoc.getX());
            teleportPacket.getDoubles().write(1, newLoc.getY());
            teleportPacket.getDoubles().write(2, newLoc.getZ());

            // Yaw/Pitch - vérifier la disponibilité avant d'écrire (1.21.4 compatibility)
            if (teleportPacket.getBytes().size() >= 2) {
                teleportPacket.getBytes().write(0, (byte) 0); // Yaw
                teleportPacket.getBytes().write(1, (byte) 0); // Pitch
            }

            // On Ground - vérifier la disponibilité
            if (teleportPacket.getBooleans().size() >= 1) {
                teleportPacket.getBooleans().write(0, false);
            }

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
