package com.rinaorc.zombiez.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.rinaorc.zombiez.ZombieZPlugin;
import com.rinaorc.zombiez.data.PlayerData;
import com.rinaorc.zombiez.progression.journey.JourneyGate;
import com.rinaorc.zombiez.zones.Zone;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestionnaire du WorldBorder par joueur basé sur la progression
 *
 * Affiche un WorldBorder côté client qui s'étend au fur et à mesure
 * que le joueur débloque de nouvelles zones.
 *
 * Utilise ProtocolLib pour envoyer des paquets personnalisés à chaque joueur.
 */
public class ZoneBorderManager {

    private final ZombieZPlugin plugin;
    private final ProtocolManager protocolManager;

    // Cache de la zone max de chaque joueur
    private final Map<UUID, Integer> playerMaxZoneCache = new ConcurrentHashMap<>();

    // Constantes de la map
    private static final double CENTER_X = 621.0; // Milieu de 0-1242
    private static final double CENTER_Z_BASE = 10100.0; // Juste avant spawn
    private static final double MAP_WIDTH = 1300.0; // Légèrement plus que 1242 pour marge

    // Zone spawn
    private static final int SPAWN_MAX_Z = 10200;
    private static final int ZONE_SIZE = 200;

    // Apparence du border
    private static final int WARNING_TIME = 0;
    private static final int WARNING_DISTANCE = 5;

    // Animation
    private static final int EXPAND_ANIMATION_DURATION_MS = 3000; // 3 secondes

    @Getter
    private boolean enabled = false;

    public ZoneBorderManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    /**
     * Initialise le gestionnaire
     */
    public void initialize() {
        enabled = true;
        plugin.log(Level.INFO, "§a✓ ZoneBorderManager initialisé");
    }

    /**
     * Nettoie le gestionnaire
     */
    public void cleanup() {
        enabled = false;
        playerMaxZoneCache.clear();
    }

    /**
     * Envoie le WorldBorder initial à un joueur lors de sa connexion
     */
    public void sendInitialBorder(Player player) {
        if (!enabled) return;

        int maxZone = getPlayerMaxUnlockedZone(player);
        playerMaxZoneCache.put(player.getUniqueId(), maxZone);

        sendWorldBorder(player, maxZone, false);
    }

    /**
     * Met à jour le border quand un joueur débloque une nouvelle zone
     */
    public void onZoneUnlocked(Player player, int newZoneId) {
        if (!enabled) return;

        UUID uuid = player.getUniqueId();
        Integer cachedMax = playerMaxZoneCache.get(uuid);

        // Mettre à jour si c'est une nouvelle zone max
        if (cachedMax == null || newZoneId > cachedMax) {
            playerMaxZoneCache.put(uuid, newZoneId);
            sendWorldBorderWithAnimation(player, newZoneId);
        }
    }

    /**
     * Supprime un joueur du cache (déconnexion)
     */
    public void removePlayer(UUID uuid) {
        playerMaxZoneCache.remove(uuid);
    }

    /**
     * Envoie le WorldBorder à un joueur
     */
    private void sendWorldBorder(Player player, int maxZone, boolean animate) {
        try {
            double[] borderParams = calculateBorderParams(maxZone);
            double centerX = borderParams[0];
            double centerZ = borderParams[1];
            double size = borderParams[2];

            // Paquet INITIALIZE_BORDER pour configuration complète
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.INITIALIZE_BORDER);

            // Structure du paquet InitializeBorder:
            // 0: centerX (double)
            // 1: centerZ (double)
            // 2: oldSize (double) - taille actuelle
            // 3: newSize (double) - taille cible
            // 4: lerpTime (long) - durée transition en ms
            // 5: portalTeleportBoundary (int) - distance max téléportation
            // 6: warningBlocks (int)
            // 7: warningTime (int)

            packet.getDoubles().write(0, centerX);
            packet.getDoubles().write(1, centerZ);

            if (animate) {
                // Animation: commencer petit et grandir
                double oldSize = size - (ZONE_SIZE * 2); // Ancienne taille
                packet.getDoubles().write(2, Math.max(MAP_WIDTH, oldSize));
                packet.getDoubles().write(3, size);
                packet.getLongs().write(0, (long) EXPAND_ANIMATION_DURATION_MS);
            } else {
                // Pas d'animation
                packet.getDoubles().write(2, size);
                packet.getDoubles().write(3, size);
                packet.getLongs().write(0, 0L);
            }

            packet.getIntegers().write(0, 29999984); // Portal teleport boundary
            packet.getIntegers().write(1, WARNING_DISTANCE);
            packet.getIntegers().write(2, WARNING_TIME);

            protocolManager.sendServerPacket(player, packet);

        } catch (Exception e) {
            plugin.log(Level.WARNING, "§cErreur envoi WorldBorder à " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Envoie le WorldBorder avec animation d'expansion et effets
     */
    private void sendWorldBorderWithAnimation(Player player, int newZone) {
        // Effet sonore
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.5f, 1.5f);

        // Message
        Zone zone = plugin.getZoneManager().getZoneById(newZone);
        String zoneName = zone != null ? zone.getDisplayName() : "Zone " + newZone;
        player.sendMessage("");
        player.sendMessage("§a§l✦ NOUVELLE ZONE ACCESSIBLE ✦");
        player.sendMessage("§7Le monde s'ouvre devant toi...");
        player.sendMessage("§e➤ §f" + zoneName + " §7est maintenant accessible!");
        player.sendMessage("");

        // Envoyer le border avec animation
        sendWorldBorder(player, newZone, true);

        // Titre
        player.sendTitle(
            "§a§l⚔ EXPANSION ⚔",
            "§7" + zoneName + " §fdébloquée!",
            10, 40, 20
        );
    }

    /**
     * Calcule les paramètres du WorldBorder pour une zone max donnée
     *
     * @param maxZone Zone maximale débloquée par le joueur
     * @return [centerX, centerZ, size]
     */
    private double[] calculateBorderParams(int maxZone) {
        // Zone 1 est toujours accessible, donc au minimum maxZone = 1
        maxZone = Math.max(1, maxZone);

        // Calculer la limite nord (minZ de la zone la plus au nord accessible)
        // Zone 1: maxZ = 10000, minZ = 9800
        // Zone N: minZ = 10000 - (N * ZONE_SIZE)
        int northernLimit = ZoneManager.ZONE_START_Z - (maxZone * ZONE_SIZE);

        // Limite sud (spawn)
        int southernLimit = SPAWN_MAX_Z;

        // Span en Z
        double zSpan = southernLimit - northernLimit;

        // La taille du border doit couvrir le plus grand des deux spans
        double size = Math.max(MAP_WIDTH, zSpan + 100); // +100 pour marge

        // Centre X: milieu de la map
        double centerX = CENTER_X;

        // Centre Z: positionné pour que le border couvre de northernLimit à southernLimit
        // Pour un border carré centré sur centerZ avec taille size:
        // - Limite nord = centerZ - size/2
        // - Limite sud = centerZ + size/2
        // On veut: centerZ - size/2 <= northernLimit ET centerZ + size/2 >= southernLimit
        // Donc: centerZ = (northernLimit + southernLimit) / 2
        double centerZ = (northernLimit + southernLimit) / 2.0;

        // Ajustement: s'assurer que le border couvre bien toute la zone
        // Si zSpan < MAP_WIDTH, le border sera carré et couvrira plus que nécessaire en Z
        // C'est acceptable car le border sera au-delà des limites de la map

        return new double[] { centerX, centerZ, size };
    }

    /**
     * Obtient la zone maximale débloquée par un joueur
     */
    private int getPlayerMaxUnlockedZone(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayer(player);
        if (data == null) return 1;

        Set<String> unlockedGates = data.getUnlockedJourneyGates();
        int maxZone = 1; // Zone 1 toujours accessible

        for (String gateName : unlockedGates) {
            try {
                JourneyGate gate = JourneyGate.valueOf(gateName);
                if (gate.getType() == JourneyGate.GateType.ZONE) {
                    maxZone = Math.max(maxZone, gate.getValue());
                }
            } catch (IllegalArgumentException ignored) {
                // Gate inconnue, ignorer
            }
        }

        return maxZone;
    }

    /**
     * Force la mise à jour du border d'un joueur
     */
    public void refreshBorder(Player player) {
        if (!enabled) return;

        int maxZone = getPlayerMaxUnlockedZone(player);
        playerMaxZoneCache.put(player.getUniqueId(), maxZone);
        sendWorldBorder(player, maxZone, false);
    }

    /**
     * Met à jour tous les borders (appelé après rechargement config)
     */
    public void refreshAllBorders() {
        if (!enabled) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshBorder(player);
        }
    }
}
