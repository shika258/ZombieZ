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
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Système GPS avec flèche 3D visible pour guider les joueurs vers les objectifs
 *
 * Utilise des ItemDisplay entities avec:
 * - Une flèche spectrale visible et brillante
 * - Rotation fluide vers la destination
 * - Effet de pulsation quand proche
 * - Glow coloré via scoreboard team
 */
public class GPSManager implements Listener {

    private final ZombieZPlugin plugin;
    private final JourneyManager journeyManager;

    // Joueurs avec GPS actif et leurs entités flèche
    private final Map<UUID, GPSArrow> activeArrows = new ConcurrentHashMap<>();

    // Patterns pour extraire les coordonnées
    private static final Pattern COORD_PATTERN = Pattern.compile("§b~?(-?\\d+),?\\s*~?(-?\\d+),?\\s*~?(-?\\d+)");

    // Configuration - Flèche devant le joueur (direction du regard)
    private static final double ARROW_DISTANCE = 5.0;    // 5 blocs devant le joueur
    private static final double ARROW_HEIGHT = 2.5;      // Au-dessus des yeux pour ne pas obstruer
    private static final float ARROW_SCALE = 2.0f;       // Taille de la flèche
    private static final float LERP_SPEED = 0.35f;       // Interpolation agressive pour fluidité
    private static final double ARRIVAL_DISTANCE = 5.0;  // Distance pour effet pulse

    // Team pour le glow coloré
    private Team gpsTeam;
    private static final String GPS_TEAM_NAME = "zombiez_gps";

    // Tâche de mise à jour
    private BukkitTask updateTask;

    public GPSManager(ZombieZPlugin plugin) {
        this.plugin = plugin;
        this.journeyManager = plugin.getJourneyManager();

        setupGlowTeam();
        startUpdateTask();
    }

    /**
     * Configure le scoreboard team pour le glow jaune
     */
    private void setupGlowTeam() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Supprimer l'ancienne team si elle existe
        gpsTeam = scoreboard.getTeam(GPS_TEAM_NAME);
        if (gpsTeam != null) {
            gpsTeam.unregister();
        }

        // Créer la nouvelle team avec couleur jaune
        gpsTeam = scoreboard.registerNewTeam(GPS_TEAM_NAME);
        gpsTeam.color(net.kyori.adventure.text.format.NamedTextColor.GOLD);
        gpsTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
    }

    /**
     * Toggle le GPS pour un joueur
     */
    public boolean toggleGPS(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeArrows.containsKey(uuid)) {
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

        // Supprimer l'ancienne flèche si existante
        GPSArrow oldArrow = activeArrows.remove(uuid);
        if (oldArrow != null) {
            oldArrow.destroy();
        }

        // Créer la nouvelle flèche
        GPSArrow arrow = new GPSArrow(player, destination);
        activeArrows.put(uuid, arrow);

        // Feedback
        double distance = player.getLocation().distance(destination);
        player.sendMessage("§a§l✓ §eGPS activé! §7Distance: §e" + String.format("%.0f", distance) + " blocs");
        player.sendMessage("§7Suivez la §eflèche dorée §7pour atteindre votre objectif.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.5f);

        return true;
    }

    /**
     * Désactive le GPS pour un joueur
     */
    public void disableGPS(Player player) {
        UUID uuid = player.getUniqueId();

        GPSArrow arrow = activeArrows.remove(uuid);
        if (arrow != null) {
            arrow.destroy();
        }

        player.sendMessage("§c§l✗ §7GPS désactivé.");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f);
    }

    /**
     * Vérifie si le GPS est actif pour un joueur
     */
    public boolean isGPSActive(Player player) {
        return activeArrows.containsKey(player.getUniqueId());
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
     * Démarre la tâche de mise à jour (chaque tick = 20 FPS pour fluidité maximale)
     */
    private void startUpdateTask() {
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, GPSArrow>> iterator = activeArrows.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<UUID, GPSArrow> entry = iterator.next();
                    UUID uuid = entry.getKey();
                    GPSArrow arrow = entry.getValue();

                    Player player = plugin.getServer().getPlayer(uuid);
                    if (player == null || !player.isOnline()) {
                        arrow.destroy();
                        iterator.remove();
                        continue;
                    }

                    // Vérifier si la destination a changé (moins fréquemment)
                    if (arrow.tickCount % 20 == 0) {
                        Location newDest = getDestinationForPlayer(player);
                        if (newDest == null) {
                            arrow.destroy();
                            iterator.remove();
                            player.sendMessage("§e§l! §7Objectif atteint ou changé - GPS désactivé.");
                            continue;
                        }

                        // Mettre à jour la destination si changée
                        if (!isSameLocation(arrow.destination, newDest)) {
                            arrow.destination = newDest;
                        }
                    }

                    // Mettre à jour la position et rotation de la flèche chaque tick
                    arrow.update(player);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Chaque tick pour fluidité
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
        GPSArrow arrow = activeArrows.remove(uuid);
        if (arrow != null) {
            arrow.destroy();
        }
    }

    /**
     * Arrête proprement le manager
     */
    public void shutdown() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
        }

        for (GPSArrow arrow : activeArrows.values()) {
            arrow.destroy();
        }
        activeArrows.clear();

        // Nettoyer la team
        if (gpsTeam != null) {
            try {
                gpsTeam.unregister();
            } catch (Exception ignored) {}
        }
    }

    // ==================== CLASSE INTERNE: GPS ARROW ====================

    /**
     * Représente une flèche GPS pour un joueur
     * - Positionnée 5 blocs DEVANT le joueur (direction du regard horizontal)
     * - Légèrement en hauteur pour ne pas obstruer la vision
     * - La pointe de la flèche POINTE vers la destination
     * - Mouvement fluide avec interpolation agressive
     */
    private class GPSArrow {
        private final UUID playerUuid;
        private Entity arrowEntity;
        private Location destination;

        // État d'interpolation pour fluidité
        private double currentX, currentY, currentZ;
        private float currentYaw = 0;
        private float currentPitch = 0;
        private boolean initialized = false;

        // Compteur pour ActionBar
        int tickCount = 0;

        GPSArrow(Player player, Location destination) {
            this.playerUuid = player.getUniqueId();
            this.destination = destination;

            spawn(player);
        }

        /**
         * Spawn l'entité flèche
         */
        private void spawn(Player player) {
            World world = player.getWorld();
            Location spawnLoc = calculateTargetPosition(player);

            // Initialiser les positions courantes
            currentX = spawnLoc.getX();
            currentY = spawnLoc.getY();
            currentZ = spawnLoc.getZ();

            arrowEntity = world.spawn(spawnLoc, ItemDisplay.class, display -> {
                // Item: Flèche spectrale (très visible, brillante)
                display.setItemStack(new ItemStack(Material.SPECTRAL_ARROW));

                // Billboard FIXED pour permettre la rotation manuelle
                display.setBillboard(Display.Billboard.FIXED);

                // Glow doré pour visibilité
                display.setGlowing(true);
                display.setGlowColorOverride(Color.fromRGB(255, 215, 0));

                // Visibilité maximale
                display.setViewRange(128f);
                display.setShadowRadius(0);
                display.setShadowStrength(0);

                // Interpolation native pour téléportation fluide
                display.setTeleportDuration(1); // 1 tick de lissage
                display.setInterpolationDuration(1);

                display.setCustomNameVisible(false);
            });

            // Ajouter à la team pour le glow coloré
            if (gpsTeam != null && arrowEntity != null) {
                gpsTeam.addEntity(arrowEntity);
            }

            initialized = true;
        }

        /**
         * Met à jour la position et rotation de la flèche chaque tick
         */
        void update(Player player) {
            if (arrowEntity == null || !arrowEntity.isValid()) {
                spawn(player);
                return;
            }

            tickCount++;

            // === CALCULER LA POSITION CIBLE ===
            // 5 blocs devant le joueur (direction horizontale du regard)
            Location targetPos = calculateTargetPosition(player);

            // === INTERPOLATION DE LA POSITION ===
            // Interpolation agressive pour suivre le joueur fluidement
            currentX = lerp(currentX, targetPos.getX(), LERP_SPEED);
            currentY = lerp(currentY, targetPos.getY(), LERP_SPEED);
            currentZ = lerp(currentZ, targetPos.getZ(), LERP_SPEED);

            // === CALCULER LA ROTATION VERS LA DESTINATION ===
            Location arrowLoc = new Location(player.getWorld(), currentX, currentY, currentZ);
            Vector toDestination = destination.toVector().subtract(arrowLoc.toVector());
            double distance = toDestination.length();

            if (distance > 0.1) {
                toDestination.normalize();
            } else {
                // Si très proche, pointer vers l'avant
                toDestination = player.getLocation().getDirection();
            }

            // Angles vers la destination
            float targetYaw = (float) Math.toDegrees(Math.atan2(-toDestination.getX(), toDestination.getZ()));
            float targetPitch = (float) Math.toDegrees(-Math.asin(Math.max(-1, Math.min(1, toDestination.getY()))));

            // Interpolation de la rotation
            currentYaw = lerpAngle(currentYaw, targetYaw, LERP_SPEED);
            currentPitch = lerpAngle(currentPitch, targetPitch, LERP_SPEED);

            // === EFFET DE PULSATION SI PROCHE ===
            float scale = ARROW_SCALE;
            if (distance < ARRIVAL_DISTANCE) {
                scale = ARROW_SCALE * (0.8f + 0.4f * (float) Math.sin(System.currentTimeMillis() * 0.01));
            }

            // === TÉLÉPORTER L'ENTITÉ ===
            arrowLoc.setYaw(currentYaw);
            arrowLoc.setPitch(currentPitch);
            arrowEntity.teleport(arrowLoc);

            // === METTRE À JOUR LA TRANSFORMATION (ROTATION DE LA FLÈCHE) ===
            if (arrowEntity instanceof ItemDisplay display) {
                // La flèche spectrale pointe naturellement vers le haut (+Y)
                // On doit la tourner pour qu'elle pointe horizontalement vers la destination

                // 1. D'abord rotation de -90° sur X pour mettre la pointe vers l'avant (au lieu du haut)
                // 2. Puis rotation sur Y pour le yaw
                // 3. Puis rotation sur X pour le pitch

                float yawRad = (float) Math.toRadians(currentYaw);
                float pitchRad = (float) Math.toRadians(-currentPitch);

                // Rotation combinée: d'abord coucher la flèche (-90° sur X), puis appliquer yaw et pitch
                // Utiliser les quaternions via AxisAngle pour une rotation correcte

                // leftRotation: rotation de base pour orienter la flèche horizontalement
                // rightRotation: rotation vers la destination (yaw)

                display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    // Left rotation: coucher la flèche (-90° sur Z) et appliquer le pitch
                    new AxisAngle4f((float) Math.toRadians(-90) + pitchRad, 0, 0, 1),
                    new Vector3f(scale, scale, scale),
                    // Right rotation: yaw pour pointer vers la destination
                    new AxisAngle4f(yawRad, 0, 1, 0)
                ));
            }

            // === ACTIONBAR AVEC DISTANCE ===
            if (tickCount % 20 == 0) { // Toutes les secondes
                String distStr = distance < 10 ? String.format("%.1f", distance) : String.format("%.0f", distance);
                String direction = getCardinalDirection(currentYaw);
                player.sendActionBar(Component.text("§e➤ §7Destination: §e" + distStr + " blocs §7(" + direction + ")"));
            }
        }

        /**
         * Calcule la position cible: 5 blocs devant le joueur (direction du regard horizontal)
         */
        private Location calculateTargetPosition(Player player) {
            Location playerLoc = player.getLocation();

            // Direction du regard HORIZONTAL (ignorer le pitch pour que la flèche reste devant)
            float yaw = playerLoc.getYaw();
            double yawRad = Math.toRadians(yaw);

            // Vecteur direction horizontal
            double dirX = -Math.sin(yawRad);
            double dirZ = Math.cos(yawRad);

            // Position: devant le joueur + en hauteur
            return new Location(
                playerLoc.getWorld(),
                playerLoc.getX() + dirX * ARROW_DISTANCE,
                playerLoc.getY() + ARROW_HEIGHT,
                playerLoc.getZ() + dirZ * ARROW_DISTANCE
            );
        }

        /**
         * Direction cardinale pour l'ActionBar
         */
        private String getCardinalDirection(float yaw) {
            // Normaliser le yaw
            yaw = ((yaw % 360) + 360) % 360;

            if (yaw >= 337.5 || yaw < 22.5) return "§fS";
            if (yaw >= 22.5 && yaw < 67.5) return "§fSO";
            if (yaw >= 67.5 && yaw < 112.5) return "§fO";
            if (yaw >= 112.5 && yaw < 157.5) return "§fNO";
            if (yaw >= 157.5 && yaw < 202.5) return "§fN";
            if (yaw >= 202.5 && yaw < 247.5) return "§fNE";
            if (yaw >= 247.5 && yaw < 292.5) return "§fE";
            return "§fSE";
        }

        /**
         * Détruit l'entité flèche
         */
        void destroy() {
            if (arrowEntity != null && arrowEntity.isValid()) {
                arrowEntity.remove();
            }
            arrowEntity = null;
        }

        // Interpolation linéaire
        private double lerp(double from, double to, double t) {
            return from + (to - from) * t;
        }

        // Interpolation d'angle (gère le wrap-around 360°)
        private float lerpAngle(float from, float to, float t) {
            float diff = to - from;
            while (diff > 180) diff -= 360;
            while (diff < -180) diff += 360;
            return from + diff * t;
        }
    }
}
