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
    private static final double MAP_WIDTH = 1300.0; // Légèrement plus que 1242 pour marge
    private static final double SPAWN_SOUTH_BUFFER = 200.0; // Extension au sud pour éloigner le border du spawn

    // Zone spawn
    private static final int SPAWN_MAX_Z = 10200;
    private static final int ZONE_SIZE = 200;

    // Apparence du border
    // WARNING_DISTANCE: distance en blocs à partir de laquelle la texture rouge du border apparaît
    // Une valeur élevée permet de voir le mur de loin (ex: 100 = visible à 100 blocs)
    private static final int WARNING_TIME = 15; // Temps en secondes avant dégâts (effet visuel de pulsation)
    private static final int WARNING_DISTANCE = 100; // Distance de visibilité de la texture du border

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
     * Envoie le WorldBorder à un joueur en utilisant des paquets séparés
     * Plus stable que INITIALIZE_BORDER qui a des problèmes de types VarLong/VarInt en 1.21.4
     */
    private void sendWorldBorder(Player player, int maxZone, boolean animate) {
        try {
            double[] borderParams = calculateBorderParams(maxZone);
            double centerX = borderParams[0];
            double centerZ = borderParams[1];
            double size = borderParams[2];

            // 1. Paquet SET_BORDER_CENTER - définir le centre
            PacketContainer centerPacket = protocolManager.createPacket(PacketType.Play.Server.SET_BORDER_CENTER);
            centerPacket.getDoubles().write(0, centerX);
            centerPacket.getDoubles().write(1, centerZ);
            protocolManager.sendServerPacket(player, centerPacket);

            // 2. SET_BORDER_SIZE - définir la taille immédiatement
            // Note: On n'utilise plus SET_BORDER_LERP_SIZE car le VarLong pose problème en 1.21.4
            // L'animation sera simulée via plusieurs paquets si nécessaire à l'avenir
            PacketContainer sizePacket = protocolManager.createPacket(PacketType.Play.Server.SET_BORDER_SIZE);
            sizePacket.getDoubles().write(0, size);
            protocolManager.sendServerPacket(player, sizePacket);

            // 3. SET_BORDER_WARNING_DISTANCE - avertissement en blocs
            PacketContainer warningDistPacket = protocolManager.createPacket(PacketType.Play.Server.SET_BORDER_WARNING_DISTANCE);
            warningDistPacket.getIntegers().write(0, WARNING_DISTANCE);
            protocolManager.sendServerPacket(player, warningDistPacket);

            // 4. SET_BORDER_WARNING_DELAY - délai avant dégâts
            PacketContainer warningDelayPacket = protocolManager.createPacket(PacketType.Play.Server.SET_BORDER_WARNING_DELAY);
            warningDelayPacket.getIntegers().write(0, WARNING_TIME);
            protocolManager.sendServerPacket(player, warningDelayPacket);

        } catch (Exception e) {
            plugin.log(Level.WARNING, "§cErreur envoi WorldBorder à " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
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

        // La taille du border inclut le buffer au sud pour éloigner le border du spawn
        double size = MAP_WIDTH + SPAWN_SOUTH_BUFFER;

        // Centre X: milieu de la map
        double centerX = CENTER_X;

        // Le centre Z est positionné pour que la LIMITE NORD corresponde à northernLimit
        // et que la LIMITE SUD soit repoussée de SPAWN_SOUTH_BUFFER blocs
        //
        // Formule: limite_nord = centerZ - size/2 = northernLimit
        // Donc: centerZ = northernLimit + size/2
        //
        // Avec size = MAP_WIDTH + SPAWN_SOUTH_BUFFER:
        // - Limite nord = northernLimit (inchangée, bloque les zones verrouillées)
        // - Limite sud = northernLimit + size (repoussée de 200 blocs derrière le spawn)
        double centerZ = northernLimit + (size / 2.0);

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
